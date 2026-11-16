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
@RequestMapping("/api/v1/lotes")
public class LoteController {
    private final CadastroService service;
    private final ConsultaOperacionalService consultas;
    private final FluxoOperacionalService fluxo;
    public LoteController(CadastroService service, ConsultaOperacionalService consultas, FluxoOperacionalService fluxo) {
        this.service = service; this.consultas = consultas; this.fluxo = fluxo;
    }
    @PostMapping
    @Operation(summary = "Cadastrar lote", description = "Somente GESTOR. Transporte CADASTRADO; inicia EM_MONITORAMENTO e NAO_DISPONIVEL.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<LoteSaidaDto> criar(@Valid @RequestBody LoteEntradaDto dto) {
        var resposta = service.criarLote(dto);
        return ResponseEntity.created(URI.create("/api/v1/lotes/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar lote", description = "FUNCIONARIO da unidade responsável ou GESTOR global.")
    public LoteSaidaDto consultar(@PathVariable UUID id) { return service.lote(id); }
    @GetMapping
    @Operation(summary = "Listar lotes", description = "FUNCIONARIO restrito à unidade; GESTOR global. Filtros cumulativos. Ordenação: id, codigo, estado; padrão id.")
    public PaginaDto<LoteSaidaDto> listar(@RequestParam(required = false) UUID transporteId,
            @RequestParam(required = false) UUID vacinaId, @RequestParam(required = false) EstadoLote estado, @ParameterObject Pageable pageable) { return service.lotes(transporteId, vacinaId, estado, pageable); }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar código do lote", description = "FUNCIONARIO da unidade ou GESTOR. Somente EM_MONITORAMENTO em transporte CADASTRADO. Não troca vacina/transporte.")
    public LoteSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody LoteAtualizacaoDto dto) { return service.atualizarLote(id, dto); }
    @PostMapping("/{id}/decisao-tecnica")
    @Operation(summary = "Registrar decisão técnica", description = "Somente GESTOR ativo. Transporte RECEBIDO; lote AGUARDANDO_CONFERENCIA ou BLOQUEADO_PREVENTIVAMENTE. LIBERAR ou DESCARTAR exige justificativa até 1000 caracteres. Destinos terminais; repetição 409. Audita; não publica disponibilidade nem encerra ocorrências.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> decidir(@PathVariable UUID id, @Valid @RequestBody DecisaoTecnicaEntradaDto dto) {
        fluxo.decidirLote(id, dto.decisao(), dto.justificativa());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/disponibilidade")
    @Operation(summary = "Confirmar ou retirar disponibilidade", description = "FUNCIONARIO da unidade ou GESTOR. Exige transporte RECEBIDO e lote LIBERADO, inclusive ao repetir. Repetição válida retorna 204 sem novo evento; mudança efetiva é auditada. Retirada não é descarte nem reserva.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> alterarDisponibilidade(@PathVariable UUID id, @Valid @RequestBody DisponibilidadeEntradaDto dto) {
        fluxo.alterarDisponibilidade(id, dto.disponibilidade());
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/historico")
    @Operation(summary = "Consultar histórico do lote", description = "FUNCIONARIO da unidade ou GESTOR. Somente eventos deste lote; leitura sem mutação. Ordenação: id, dataHora, acao; padrão id.")
    public PaginaDto<EventoSaidaDto> historico(@PathVariable UUID id, @ParameterObject Pageable pageable) { return consultas.historicoLote(id, pageable); }
}
