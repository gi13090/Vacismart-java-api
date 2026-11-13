package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.DisponibilidadeLote;
import jakarta.validation.constraints.NotNull;

public record DisponibilidadeEntradaDto(@NotNull DisponibilidadeLote disponibilidade) { }
