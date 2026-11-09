package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.domain.rules.Validacoes;
import br.com.fiap.vacismart.repository.*;
import br.com.fiap.vacismart.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CadastroService {
    private final UnidadeSaudeRepository unidades;
    private final VacinaRepository vacinas;
    private final TransporteRepository transportes;
    private final LoteRepository lotes;
    private final LeituraTemperaturaRepository leituras;
    private final OcorrenciaRepository ocorrencias;
    private final EventoAuditoriaRepository auditoria;

    public CadastroService(UnidadeSaudeRepository unidades, VacinaRepository vacinas,
            TransporteRepository transportes, LoteRepository lotes, LeituraTemperaturaRepository leituras,
            OcorrenciaRepository ocorrencias, EventoAuditoriaRepository auditoria) {
        this.unidades = unidades; this.vacinas = vacinas; this.transportes = transportes;
        this.lotes = lotes; this.leituras = leituras; this.ocorrencias = ocorrencias; this.auditoria = auditoria;
    }
    private static NoSuchElementException ausente() { return new NoSuchElementException("Recurso não encontrado"); }
    private UnidadeSaude unidadeBloqueada(UUID id) { return unidades.buscarParaAtualizacao(id).orElseThrow(CadastroService::ausente); }
    private Vacina vacinaBloqueada(UUID id) { return vacinas.buscarParaAtualizacao(id).orElseThrow(CadastroService::ausente); }
    private Transporte transporteBloqueado(UUID id) { return transportes.buscarParaAtualizacao(id).orElseThrow(CadastroService::ausente); }
    public UnidadeSaidaDto unidade(UUID id) { return MapeadorDto.de(unidades.findById(id).orElseThrow(CadastroService::ausente)); }
    public VacinaSaidaDto vacina(UUID id) { return MapeadorDto.de(vacinas.findById(id).orElseThrow(CadastroService::ausente)); }
    public TransporteSaidaDto transporte(UUID id) { return MapeadorDto.de(transportes.findById(id).orElseThrow(CadastroService::ausente)); }
    public LoteSaidaDto lote(UUID id) { return MapeadorDto.de(lotes.findById(id).orElseThrow(CadastroService::ausente)); }
    public PaginaDto<UnidadeSaidaDto> unidades(Pageable p) { return PaginaDto.de(unidades.findAll(Paginacao.validar(p, "id", "nome")).map(MapeadorDto::de)); }
    public PaginaDto<VacinaSaidaDto> vacinas(Pageable p) { return PaginaDto.de(vacinas.findAll(Paginacao.validar(p, "id", "nome")).map(MapeadorDto::de)); }
    public PaginaDto<TransporteSaidaDto> transportes(UUID unidadeId, EstadoTransporte estado, Pageable p) {
        return PaginaDto.de(transportes.findAll(Filtros.transportes(unidadeId, estado), Paginacao.validar(p, "id", "codigo", "estado", "inicioMonitoramentoEm")).map(MapeadorDto::de));
    }
    public PaginaDto<LoteSaidaDto> lotes(UUID transporteId, UUID vacinaId, EstadoLote estado, Pageable p) {
        return PaginaDto.de(lotes.findAll(Filtros.lotes(transporteId, vacinaId, estado), Paginacao.validar(p, "id", "codigo", "estado")).map(MapeadorDto::de));
    }
    @Transactional
    public UnidadeSaidaDto criarUnidade(UnidadeEntradaDto d) { return MapeadorDto.de(unidades.save(new UnidadeSaude(d.nome()))); }
    @Transactional
    public UnidadeSaidaDto atualizarUnidade(UUID id, UnidadeEntradaDto d) {
        UnidadeSaude e = unidadeBloqueada(id); e.atualizar(d.nome()); return MapeadorDto.de(e);
    }
    @Transactional
    public void excluirUnidade(UUID id) {
        UnidadeSaude e = unidadeBloqueada(id);
        if (transportes.existsByUnidadeSaudeId(id) || auditoria.existsByReferenciaId(id)) throw new IllegalStateException("Unidade possui dependências históricas");
        unidades.delete(e);
    }
    @Transactional
    public VacinaSaidaDto criarVacina(VacinaEntradaDto d) {
        return MapeadorDto.de(vacinas.save(new Vacina(d.nome(), d.temperaturaMinC(), d.temperaturaMaxC(), d.margemAtencaoC())));
    }
    @Transactional
    public VacinaSaidaDto atualizarVacina(UUID id, VacinaEntradaDto d) {
        Vacina e = vacinaBloqueada(id);
        Validacoes.faixa(d.temperaturaMinC(), d.temperaturaMaxC(), d.margemAtencaoC());
        boolean mudou = e.getTemperaturaMinC().compareTo(d.temperaturaMinC()) != 0
                || e.getTemperaturaMaxC().compareTo(d.temperaturaMaxC()) != 0
                || e.getMargemAtencaoC().compareTo(d.margemAtencaoC()) != 0;
        if (mudou && lotes.existsByVacinaId(id)) throw new IllegalStateException("Faixa térmica de vacina com lotes não pode ser alterada");
        e.atualizar(d.nome(), d.temperaturaMinC(), d.temperaturaMaxC(), d.margemAtencaoC());
        return MapeadorDto.de(e);
    }
    @Transactional
    public void excluirVacina(UUID id) {
        Vacina e = vacinaBloqueada(id);
        if (lotes.existsByVacinaId(id) || auditoria.existsByReferenciaId(id)) throw new IllegalStateException("Vacina possui dependências históricas");
        vacinas.delete(e);
    }
    @Transactional
    public TransporteSaidaDto criarTransporte(TransporteEntradaDto d) {
        return MapeadorDto.de(transportes.save(new Transporte(d.codigo(), unidadeBloqueada(d.unidadeSaudeId()))));
    }
    @Transactional
    public TransporteSaidaDto atualizarTransporte(UUID id, TransporteEntradaDto d) {
        Transporte e = transporteBloqueado(id);
        if (e.getEstado() != EstadoTransporte.CADASTRADO) throw new IllegalStateException("Transporte já iniciou monitoramento");
        e.atualizar(d.codigo(), unidadeBloqueada(d.unidadeSaudeId())); return MapeadorDto.de(e);
    }
    @Transactional
    public void excluirTransporte(UUID id) {
        Transporte e = transporteBloqueado(id);
        if (e.getEstado() != EstadoTransporte.CADASTRADO || lotes.existsByTransporteId(id)
                || leituras.existsByTransporteId(id) || ocorrencias.existsByTransporteId(id)
                || auditoria.existsByReferenciaId(id)) throw new IllegalStateException("Transporte possui vínculos ou histórico");
        transportes.delete(e);
    }
    @Transactional
    public LoteSaidaDto criarLote(LoteEntradaDto d) {
        Transporte t = transporteBloqueado(d.transporteId());
        if (t.getEstado() != EstadoTransporte.CADASTRADO) throw new IllegalStateException("Cadastro de lote exige transporte CADASTRADO");
        return MapeadorDto.de(lotes.save(new Lote(d.codigo(), vacinaBloqueada(d.vacinaId()), t)));
    }
    @Transactional
    public LoteSaidaDto atualizarLote(UUID id, LoteAtualizacaoDto d) {
        Lote referencia = lotes.findById(id).orElseThrow(CadastroService::ausente);
        Transporte t = transporteBloqueado(referencia.getTransporte().getId());
        // O estado é conferido sob o mesmo lock usado pelo monitoramento.
        if (t.getEstado() != EstadoTransporte.CADASTRADO) throw new IllegalStateException("Lote já possui monitoramento");
        referencia.atualizarCodigo(d.codigo()); return MapeadorDto.de(referencia);
    }
}
