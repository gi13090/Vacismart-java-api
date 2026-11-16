package br.com.fiap.vacismart.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import org.springdoc.core.annotations.ParameterObject;

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
    @Operation(summary = "Cadastrar transporte", description = "FUNCIONARIO da unidade informada ou GESTOR global. Inicia CADASTRADO, sem GPS.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<TransporteSaidaDto> criar(@Valid @RequestBody TransporteEntradaDto dto) {
        var resposta = service.criarTransporte(dto);
        return ResponseEntity.created(URI.create("/api/v1/transportes/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar transporte", description = "FUNCIONARIO da unidade responsável ou GESTOR global.")
    public TransporteSaidaDto consultar(@PathVariable UUID id) { return service.transporte(id); }
    @GetMapping
    @Operation(summary = "Listar transportes", description = "FUNCIONARIO restrito à unidade; GESTOR global. Filtros cumulativos. Ordenação: id, codigo, estado, inicioMonitoramentoEm; padrão id.")
    public PaginaDto<TransporteSaidaDto> listar(@RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) EstadoTransporte estado, @ParameterObject Pageable pageable) { return service.transportes(unidadeId, estado, pageable); }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar transporte", description = "FUNCIONARIO da unidade ou GESTOR. Somente CADASTRADO; funcionário não transfere entre unidades.")
    public TransporteSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody TransporteEntradaDto dto) { return service.atualizarTransporte(id, dto); }
    @PostMapping("/{id}/monitoramento")
    @Operation(summary = "Iniciar monitoramento simulado", description = "FUNCIONARIO da unidade ou GESTOR. Sem corpo. CADASTRADO com pelo menos um lote passa a EM_TRANSPORTE; registra auditoria.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> iniciar(@PathVariable UUID id) { monitoramento.iniciarMonitoramento(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/recebimento")
    @Operation(summary = "Receber transporte", description = "FUNCIONARIO da unidade de destino ou GESTOR. Sem corpo. EM_TRANSPORTE passa a RECEBIDO; lotes em monitoramento aguardam conferência e bloqueados permanecem. Sem liberação, disponibilidade ou finalização automática. Repetição retorna 409.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> receber(@PathVariable UUID id) {
        fluxo.receberTransporte(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir transporte", description = "FUNCIONARIO da unidade ou GESTOR. Somente CADASTRADO, sem lotes, leituras, ocorrências ou histórico.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirTransporte(id); return ResponseEntity.noContent().build(); }
}
