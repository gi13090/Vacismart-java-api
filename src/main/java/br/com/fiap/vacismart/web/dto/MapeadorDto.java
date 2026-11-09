package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.entity.*;

public final class MapeadorDto {
    private MapeadorDto() { }
    public static UnidadeSaidaDto de(UnidadeSaude e) { return new UnidadeSaidaDto(e.getId(), e.getNome()); }
    public static VacinaSaidaDto de(Vacina e) { return new VacinaSaidaDto(e.getId(), e.getNome(), e.getTemperaturaMinC(), e.getTemperaturaMaxC(), e.getMargemAtencaoC()); }
    public static TransporteSaidaDto de(Transporte e) { return new TransporteSaidaDto(e.getId(), e.getCodigo(), e.getUnidadeSaude().getId(), e.getEstado(), e.getInicioMonitoramentoEm()); }
    public static LoteSaidaDto de(Lote e) { return new LoteSaidaDto(e.getId(), e.getCodigo(), e.getVacina().getId(), e.getTransporte().getId(), e.getEstado(), e.getDisponibilidade()); }
    public static LeituraSaidaDto de(LeituraTemperatura e) { return new LeituraSaidaDto(e.getId(), e.getTransporte().getId(), e.getDispositivoId(), e.getTemperaturaC(), e.getMedidaEm(), e.getRecebidaEm(), e.getClassificacao()); }
    public static OcorrenciaSaidaDto de(Ocorrencia e) { return new OcorrenciaSaidaDto(e.getId(), e.getTransporte().getId(), e.getTipo(), e.getEstado(), e.getAbertaEm(), e.getEncerradaEm()); }
    public static EventoSaidaDto de(EventoAuditoria e) { return new EventoSaidaDto(e.getId(), e.getEntidade(), e.getReferenciaId(), e.getAcao(), e.getAutor(), e.getDataHora(), e.getJustificativa()); }
}
