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
@RequestMapping("/api/v1/vacinas")
public class VacinaController {
    private final CadastroService service;
    public VacinaController(CadastroService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<VacinaSaidaDto> criar(@Valid @RequestBody VacinaEntradaDto dto) {
        var resposta = service.criarVacina(dto);
        return ResponseEntity.created(URI.create("/api/v1/vacinas/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    public VacinaSaidaDto consultar(@PathVariable UUID id) { return service.vacina(id); }
    @GetMapping
    public PaginaDto<VacinaSaidaDto> listar(Pageable pageable) { return service.vacinas(pageable); }
    @PutMapping("/{id}")
    public VacinaSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody VacinaEntradaDto dto) { return service.atualizarVacina(id, dto); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) { service.excluirVacina(id); return ResponseEntity.noContent().build(); }
}
