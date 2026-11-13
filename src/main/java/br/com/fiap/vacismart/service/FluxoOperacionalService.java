package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.entity.EventoAuditoria;
import br.com.fiap.vacismart.repository.EventoAuditoriaRepository;
import br.com.fiap.vacismart.repository.LoteRepository;
import br.com.fiap.vacismart.security.AcessoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class FluxoOperacionalService {
    private final AcessoService acesso;
    private final MonitoramentoService monitoramento;
    private final LoteRepository lotes;
    private final EventoAuditoriaRepository auditoria;
    private final Clock relogio;

    public FluxoOperacionalService(AcessoService acesso, MonitoramentoService monitoramento,
            LoteRepository lotes, EventoAuditoriaRepository auditoria, Clock relogio) {
        this.acesso = acesso;
        this.monitoramento = monitoramento;
        this.lotes = lotes;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    public void receberTransporte(UUID id) {
        var transporte = monitoramento.buscarComBloqueio(id);
        String autor = acesso.atual().getId().toString();
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        // Avalia o silêncio antes de encerrar o monitoramento, sob o mesmo lock/transação.
        monitoramento.verificarPerdaTelemetria(transporte, agora);
        transporte.receber();
        auditoria.save(new EventoAuditoria("Transporte", id, "RECEBIDO", autor, agora,
                "Recebimento na unidade de destino; conferência humana pendente"));
        for (var lote : lotes.findByTransporteId(id)) {
            if (lote.encaminharParaConferencia()) {
                auditoria.save(new EventoAuditoria("Lote", lote.getId(), "AGUARDANDO_CONFERENCIA", autor, agora,
                        "Recebimento do transporte " + id));
            }
        }
    }
}
