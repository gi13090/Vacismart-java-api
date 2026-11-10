package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.domain.rules.ClassificadorTemperatura;
import br.com.fiap.vacismart.domain.rules.Validacoes;
import br.com.fiap.vacismart.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class MonitoramentoService {
    private final br.com.fiap.vacismart.security.AcessoService acesso;
    private final TransporteRepository transportes;
    private final LoteRepository lotes;
    private final LeituraTemperaturaRepository leituras;
    private final OcorrenciaRepository ocorrencias;
    private final EventoAuditoriaRepository auditoria;
    private final ClassificadorTemperatura classificador;
    private final Clock relogio;
    private final Duration limiteTelemetria;

    public MonitoramentoService(br.com.fiap.vacismart.security.AcessoService acesso, TransporteRepository transportes, LoteRepository lotes,
            LeituraTemperaturaRepository leituras, OcorrenciaRepository ocorrencias,
            EventoAuditoriaRepository auditoria, ClassificadorTemperatura classificador,
            Clock relogio, @Value("${vacismart.telemetria.limite:PT3M}") Duration limiteTelemetria) {
        if (limiteTelemetria == null || limiteTelemetria.isNegative() || limiteTelemetria.isZero()) {
            throw new IllegalArgumentException("Limite de telemetria deve ser positivo");
        }
        this.acesso = acesso; this.transportes = transportes;
        this.lotes = lotes;
        this.leituras = leituras;
        this.ocorrencias = ocorrencias;
        this.auditoria = auditoria;
        this.classificador = classificador;
        this.relogio = relogio;
        this.limiteTelemetria = limiteTelemetria;
    }

    public void iniciarMonitoramento(UUID transporteId) {
        Transporte transporte = buscarComBloqueio(transporteId);
        if (lotes.findByTransporteId(transporteId).isEmpty()) {
            throw new IllegalStateException("Transporte sem lotes não pode iniciar monitoramento");
        }
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        transporte.iniciarMonitoramento(agora);
        auditar("Transporte", transporteId, "EM_TRANSPORTE", agora, "Início do monitoramento simulado");
    }

    public LeituraTemperatura registrarLeitura(UUID transporteId, String dispositivoId,
            BigDecimal temperaturaC, OffsetDateTime medidaEm) {
        Transporte transporte = buscarComBloqueio(transporteId);
        exigirEmTransporte(transporte);
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Validacoes.texto(dispositivoId, 100, "Dispositivo");
        Validacoes.decimal(temperaturaC);
        Objects.requireNonNull(medidaEm, "Momento da medição obrigatório");
        if (medidaEm.isAfter(agora) || medidaEm.isBefore(transporte.getInicioMonitoramentoEm())) {
            throw new IllegalArgumentException("Medição futura ou anterior ao início do monitoramento");
        }
        List<Lote> lotesTransporte = lotes.findByTransporteId(transporteId);
        if (lotesTransporte.isEmpty()) throw new IllegalStateException("Transporte sem lotes");
        ClassificacaoTemperatura pior = ClassificacaoTemperatura.NORMAL;
        List<Lote> afetados = new ArrayList<>();
        for (Lote lote : lotesTransporte) {
            ClassificacaoTemperatura classificacao = classificador.classificar(temperaturaC, lote.getVacina());
            if (classificacao.maisGraveQue(pior)) pior = classificacao;
            if (classificacao == ClassificacaoTemperatura.FORA_DA_FAIXA) afetados.add(lote);
        }
        LeituraTemperatura leitura = leituras.save(new LeituraTemperatura(transporte, dispositivoId,
                temperaturaC, medidaEm, agora, pior));
        if (!afetados.isEmpty()) {
            abrirOcorrencia(transporte, TipoOcorrencia.FORA_DA_FAIXA, agora);
            for (Lote lote : afetados) {
                if (lote.bloquearPreventivamente()) {
                    auditar("Lote", lote.getId(), "BLOQUEADO_PREVENTIVAMENTE", agora,
                            "Temperatura fora da faixa da vacina; leitura " + leitura.getId());
                }
            }
        }
        // A volta da comunicação encerra somente a ocorrência de telemetria.
        encerrarAberta(transporteId, TipoOcorrencia.PERDA_DE_TELEMETRIA, agora, "Recebimento de leitura retomado");
        return leitura;
    }

    public boolean verificarPerdaTelemetria(UUID transporteId) {
        Transporte transporte = buscarComBloqueio(transporteId);
        if (transporte.getEstado() != EstadoTransporte.EM_TRANSPORTE) return false;
        OffsetDateTime referencia = leituras.findFirstByTransporteIdOrderByRecebidaEmDesc(transporteId)
                .map(LeituraTemperatura::getRecebidaEm).orElse(transporte.getInicioMonitoramentoEm());
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        if (Duration.between(referencia, agora).compareTo(limiteTelemetria) <= 0) return false;
        return abrirOcorrencia(transporte, TipoOcorrencia.PERDA_DE_TELEMETRIA, agora);
    }

    public void encerrarOcorrencia(UUID transporteId, TipoOcorrencia tipo, String justificativa) {
        buscarComBloqueio(transporteId);
        Objects.requireNonNull(tipo, "Tipo obrigatório");
        Validacoes.texto(justificativa, 1000, "Justificativa");
        encerrarAberta(transporteId, tipo, OffsetDateTime.now(relogio), justificativa);
    }

    private Transporte buscarComBloqueio(UUID id) {
        Objects.requireNonNull(id, "Transporte obrigatório");
        acesso.transporte(id);
        // Serializa leitura, início e verificação por transporte, inclusive entre instâncias da API.
        Transporte transporte = transportes.buscarParaAtualizacao(id)
                .orElseThrow(() -> new NoSuchElementException("Transporte não encontrado: " + id));
        acesso.unidade(transporte.getUnidadeSaude().getId());
        return transporte;
    }

    private void exigirEmTransporte(Transporte transporte) {
        if (transporte.getEstado() != EstadoTransporte.EM_TRANSPORTE) {
            throw new IllegalStateException("Leituras exigem transporte EM_TRANSPORTE");
        }
    }

    private boolean abrirOcorrencia(Transporte transporte, TipoOcorrencia tipo, OffsetDateTime agora) {
        if (ocorrencias.findByTransporteIdAndTipoAndEstado(transporte.getId(), tipo, EstadoOcorrencia.ABERTA).isPresent()) {
            return false;
        }
        Ocorrencia ocorrencia = ocorrencias.save(new Ocorrencia(transporte, tipo, agora));
        auditar("Ocorrencia", ocorrencia.getId(), "ABERTA", agora, tipo.name());
        return true;
    }

    private void encerrarAberta(UUID transporteId, TipoOcorrencia tipo, OffsetDateTime agora, String justificativa) {
        ocorrencias.findByTransporteIdAndTipoAndEstado(transporteId, tipo, EstadoOcorrencia.ABERTA)
                .ifPresent(ocorrencia -> {
                    ocorrencia.encerrar(agora);
                    auditar("Ocorrencia", ocorrencia.getId(), "ENCERRADA", agora, justificativa);
                });
    }

    private void auditar(String entidade, UUID id, String acao, OffsetDateTime agora, String justificativa) {
        auditoria.save(new EventoAuditoria(entidade, id, acao, "SISTEMA", agora, justificativa));
    }
}
