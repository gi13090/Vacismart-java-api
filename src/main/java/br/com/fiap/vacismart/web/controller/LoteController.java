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
@RequestMapping("/api/v1/lotes")
public class LoteController {
    private final CadastroService service;
    private final ConsultaOperacionalService consultas;
    private final FluxoOperacionalService fluxo;
    public LoteController(CadastroService service, ConsultaOperacionalService consultas, FluxoOperacionalService fluxo) {
        this.service = service; this.consultas = consultas; this.fluxo = fluxo;
    }
    @PostMapping
    public ResponseEntity<LoteSaidaDto> criar(@Valid @RequestBody LoteEntradaDto dto) {
        var resposta = service.criarLote(dto);
        return ResponseEntity.created(URI.create("/api/v1/lotes/" + resposta.id())).body(resposta);
    }
    @GetMapping("/{id}")
    public LoteSaidaDto consultar(@PathVariable UUID id) { return service.lote(id); }
    @GetMapping
    public PaginaDto<LoteSaidaDto> listar(@RequestParam(required = false) UUID transporteId,
            @RequestParam(required = false) UUID vacinaId, @RequestParam(required = false) EstadoLote estado, Pageable pageable) { return service.lotes(transporteId, vacinaId, estado, pageable); }
    @PutMapping("/{id}")
    public LoteSaidaDto atualizar(@PathVariable UUID id, @Valid @RequestBody LoteAtualizacaoDto dto) { return service.atualizarLote(id, dto); }
    @PostMapping("/{id}/decisao-tecnica")
    public ResponseEntity<Void> decidir(@PathVariable UUID id, @Valid @RequestBody DecisaoTecnicaEntradaDto dto) {
        fluxo.decidirLote(id, dto.decisao(), dto.justificativa());
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/historico")
    public PaginaDto<EventoSaidaDto> historico(@PathVariable UUID id, Pageable pageable) { return consultas.historicoLote(id, pageable); }
}
