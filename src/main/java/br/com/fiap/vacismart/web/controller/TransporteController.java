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
@RequestMapping("/api/v1/transportes")
public class TransporteController {
    private final CadastroService service;
    private final MonitoramentoService monitoramento;
    private final FluxoOperacionalService fluxo;
    public TransporteController(CadastroService service, MonitoramentoService monitoramento, FluxoOperacionalService fluxo) {
        this.service = service; this.monitoramento = monitoramento; this.fluxo = fluxo;
    }
    @PostMapping
    public ResponseEntity<TransporteSaidaDto> criar(@Valid @RequestBody TransporteEntradaDto dto) {
        var resposta = service.criarTransporte(dto);
        return ResponseEntity.created(URI.create("/api/v1/transportes/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    public TransporteSaidaDto consultar(@PathVariable UUID id) { return service.transporte(id); }
    @GetMapping
    public PaginaDto<TransporteSaidaDto> listar(@RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) EstadoTransporte estado, Pageable pageable) { return service.transportes(unidadeId, estado, pageable); }
    @PutMapping("/{id}")
    public TransporteSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody TransporteEntradaDto dto) { return service.atualizarTransporte(id, dto); }
    @PostMapping("/{id}/monitoramento")
    public ResponseEntity<Void> iniciar(@PathVariable UUID id) { monitoramento.iniciarMonitoramento(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/recebimento")
    public ResponseEntity<Void> receber(@PathVariable UUID id) {
        fluxo.receberTransporte(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirTransporte(id); return ResponseEntity.noContent().build(); }
}
