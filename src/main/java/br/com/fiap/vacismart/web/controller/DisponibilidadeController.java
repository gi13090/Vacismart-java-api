package br.com.fiap.vacismart.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;

import br.com.fiap.vacismart.service.ConsultaDisponibilidadeService;
import br.com.fiap.vacismart.web.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disponibilidade")
public class DisponibilidadeController {
    private final ConsultaDisponibilidadeService service;
    public DisponibilidadeController(ConsultaDisponibilidadeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Consultar disponibilidade informativa", description = "CIDADAO, FUNCIONARIO e GESTOR ativos recebem a mesma visão pública global. Filtros cumulativos; universo unidade × vacina, sem implicar oferta ou estoque. CONFIRMADA exige lote LIBERADO/CONFIRMADA e transporte RECEBIDO; CONFIRMACAO_RETIRADA exige retirada explícita sem confirmação válida/flag inconsistente; demais casos SEM_INFORMACAO. Sem quantidades, dados operacionais ou garantia de dose. UUID inexistente retorna página vazia. Ordenação somente unidadeNome, vacinaNome; padrão unidadeNome e vacinaNome ascendentes, com desempate por UUIDs.")
    public PaginaDto<DisponibilidadeSaidaDto> consultar(@RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID vacinaId, @ParameterObject Pageable pageable) {
        return service.consultar(unidadeId, vacinaId, pageable);
    }
}
