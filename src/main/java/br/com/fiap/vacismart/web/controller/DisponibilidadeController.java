package br.com.fiap.vacismart.web.controller;

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
    public PaginaDto<DisponibilidadeSaidaDto> consultar(@RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID vacinaId, Pageable pageable) {
        return service.consultar(unidadeId, vacinaId, pageable);
    }
}
