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
@RequestMapping("/api/v1/unidades")
public class UnidadeController {
    private final CadastroService service;
    public UnidadeController(CadastroService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<UnidadeSaidaDto> criar(@Valid @RequestBody UnidadeEntradaDto dto) {
        var resposta = service.criarUnidade(dto);
        return ResponseEntity.created(URI.create("/api/v1/unidades/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    public UnidadeSaidaDto consultar(@PathVariable UUID id) { return service.unidade(id); }
    @GetMapping
    public PaginaDto<UnidadeSaidaDto> listar(Pageable pageable) { return service.unidades(pageable); }
    @PutMapping("/{id}")
    public UnidadeSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody UnidadeEntradaDto dto) { return service.atualizarUnidade(id, dto); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirUnidade(id); return ResponseEntity.noContent().build(); }
}
