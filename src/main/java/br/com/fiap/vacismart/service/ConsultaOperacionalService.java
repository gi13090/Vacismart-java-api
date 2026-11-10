package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.enums.EstadoOcorrencia;
import br.com.fiap.vacismart.repository.*;
import br.com.fiap.vacismart.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ConsultaOperacionalService {
    private final br.com.fiap.vacismart.security.AcessoService acesso;
    private final TransporteRepository transportes;
    private final LoteRepository lotes;
    private final LeituraTemperaturaRepository leituras;
    private final OcorrenciaRepository ocorrencias;
    private final EventoAuditoriaRepository auditoria;
    public ConsultaOperacionalService(br.com.fiap.vacismart.security.AcessoService acesso, TransporteRepository transportes, LoteRepository lotes,
            LeituraTemperaturaRepository leituras, OcorrenciaRepository ocorrencias, EventoAuditoriaRepository auditoria) {
        this.acesso = acesso; this.transportes = transportes; this.lotes = lotes; this.leituras = leituras; this.ocorrencias = ocorrencias; this.auditoria = auditoria;
    }
    private void exigirTransporte(UUID id) {
        acesso.transporte(id);
        if (!transportes.existsById(id)) throw new NoSuchElementException("Transporte não encontrado");
    }
    public LeituraSaidaDto leitura(UUID transporteId, UUID id) {
        exigirTransporte(transporteId);
        return leituras.findById(id).filter(e -> e.getTransporte().getId().equals(transporteId))
                .map(MapeadorDto::de).orElseThrow(() -> new NoSuchElementException("Leitura não encontrada"));
    }
    public PaginaDto<LeituraSaidaDto> leituras(UUID id, Pageable p) {
        exigirTransporte(id);
        return PaginaDto.de(leituras.findByTransporteId(id, Paginacao.validar(p, "id", "medidaEm", "recebidaEm", "temperaturaC", "classificacao")).map(MapeadorDto::de));
    }
    public PaginaDto<OcorrenciaSaidaDto> ocorrencias(UUID id, EstadoOcorrencia estado, Pageable p) {
        exigirTransporte(id);
        return PaginaDto.de(ocorrencias.consultarPorTransporte(id, estado, Paginacao.validar(p, "id", "abertaEm", "encerradaEm", "tipo", "estado")).map(MapeadorDto::de));
    }
    public PaginaDto<EventoSaidaDto> historicoTransporte(UUID id, Pageable p) {
        exigirTransporte(id); return historico("Transporte", id, p);
    }
    public PaginaDto<EventoSaidaDto> historicoLote(UUID id, Pageable p) {
        acesso.lote(id);
        if (!lotes.existsById(id)) throw new NoSuchElementException("Lote não encontrado");
        return historico("Lote", id, p);
    }
    private PaginaDto<EventoSaidaDto> historico(String entidade, UUID id, Pageable p) {
        return PaginaDto.de(auditoria.findByEntidadeAndReferenciaId(entidade, id,
                Paginacao.validar(p, "id", "dataHora", "acao")).map(MapeadorDto::de));
    }
}
