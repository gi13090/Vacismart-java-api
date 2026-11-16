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
@RequestMapping("/api/v1/vacinas")
public class VacinaController {
    private final CadastroService service;
    public VacinaController(CadastroService service) { this.service = service; }
    @PostMapping
    @Operation(summary = "Cadastrar vacina", description = "Somente GESTOR. Temperaturas numeric(10,3), sem arredondamento; mínimo < máximo, margem >= 0 e 2 × margem <= máximo − mínimo.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<VacinaSaidaDto> criar(@Valid @RequestBody VacinaEntradaDto dto) {
        var resposta = service.criarVacina(dto);
        return ResponseEntity.created(URI.create("/api/v1/vacinas/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar vacina", description = "FUNCIONARIO ou GESTOR. Catálogo operacional; não é endpoint cidadão.")
    public VacinaSaidaDto consultar(@PathVariable UUID id) { return service.vacina(id); }
    @GetMapping
    @Operation(summary = "Listar vacinas", description = "FUNCIONARIO ou GESTOR. Ordenação: id, nome; padrão id.")
    public PaginaDto<VacinaSaidaDto> listar(@ParameterObject Pageable pageable) { return service.vacinas(pageable); }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vacina", description = "Somente GESTOR. Parâmetros térmicos ficam congelados quando existem lotes; renomear com parâmetros numericamente iguais é permitido.")
    public VacinaSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody VacinaEntradaDto dto) { return service.atualizarVacina(id, dto); }
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir vacina", description = "Somente GESTOR. Exige ausência de lotes e histórico.")
    @ApiResponse(responseCode = "204", description = "Operação concluída, sem corpo", content = @Content)
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirVacina(id); return ResponseEntity.noContent().build(); }
}
