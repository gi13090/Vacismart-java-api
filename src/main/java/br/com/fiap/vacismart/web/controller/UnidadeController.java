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
@RequestMapping("/api/v1/unidades")
public class UnidadeController {
    private final CadastroService service;
    public UnidadeController(CadastroService service) { this.service = service; }
    @PostMapping
    @Operation(summary = "Cadastrar unidade", description = "Somente GESTOR.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<UnidadeSaidaDto> criar(@Valid @RequestBody UnidadeEntradaDto dto) {
        var resposta = service.criarUnidade(dto);
        return ResponseEntity.created(URI.create("/api/v1/unidades/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar unidade", description = "FUNCIONARIO da própria unidade ou GESTOR global.")
    public UnidadeSaidaDto consultar(@PathVariable UUID id) { return service.unidade(id); }
    @GetMapping
    @Operation(summary = "Listar unidades", description = "FUNCIONARIO vê apenas a própria unidade; GESTOR vê todas. Ordenação: id, nome; padrão id.")
    public PaginaDto<UnidadeSaidaDto> listar(@ParameterObject Pageable pageable) { return service.unidades(pageable); }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar unidade", description = "Somente GESTOR. Atualiza o nome.")
    public UnidadeSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody UnidadeEntradaDto dto) { return service.atualizarUnidade(id, dto); }
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir unidade", description = "Somente GESTOR. Exige ausência de transportes, histórico e vínculos impeditivos; não remove dependências em cascata.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirUnidade(id); return ResponseEntity.noContent().build(); }
}
