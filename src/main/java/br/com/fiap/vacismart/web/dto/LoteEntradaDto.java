package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record LoteEntradaDto(@NotBlank @Size(max = 60) String codigo, @NotNull UUID vacinaId, @NotNull UUID transporteId) { }
