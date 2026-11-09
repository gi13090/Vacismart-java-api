package br.com.fiap.vacismart.web.controller;

import br.com.fiap.vacismart.service.*;
import br.com.fiap.vacismart.web.dto.*;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import jakarta.validation.Valid;
import java.util.UUID;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/transportes/{id}")
public class OperacaoTransporteController {
    private final MonitoramentoService monitoramento;
    private final ConsultaOperacionalService consultas;
    public OperacaoTransporteController(MonitoramentoService monitoramento, ConsultaOperacionalService consultas) { this.monitoramento = monitoramento; this.consultas = consultas; }
    @PostMapping("/leituras")
    public ResponseEntity<LeituraSaidaDto> registrar(@PathVariable UUID id, @Valid @RequestBody LeituraEntradaDto dto) {
        if (!id.equals(dto.transporteId())) throw new IllegalArgumentException("transporteId deve coincidir com a rota");
        var leitura = monitoramento.registrarLeitura(id, dto.dispositivoId(), dto.temperaturaC(), dto.medidaEm());
        var resposta = MapeadorDto.de(leitura);
        return ResponseEntity.created(URI.create("/api/v1/transportes/" + id + "/leituras/" + resposta.id())).body(resposta);
    }
    @GetMapping("/leituras/{leituraId}")
    public LeituraSaidaDto leitura(@PathVariable UUID id, @PathVariable UUID leituraId) { return consultas.leitura(id, leituraId); }
    @GetMapping("/leituras")
    public PaginaDto<LeituraSaidaDto> leituras(@PathVariable UUID id, Pageable pageable) { return consultas.leituras(id, pageable); }
    @GetMapping("/ocorrencias")
    public PaginaDto<OcorrenciaSaidaDto> ocorrencias(@PathVariable UUID id,
            @RequestParam(required = false) EstadoOcorrencia estado, Pageable pageable) { return consultas.ocorrencias(id, estado, pageable); }
    @GetMapping("/historico")
    public PaginaDto<EventoSaidaDto> historico(@PathVariable UUID id, Pageable pageable) { return consultas.historicoTransporte(id, pageable); }
}
