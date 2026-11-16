package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

@io.swagger.v3.oas.annotations.media.Schema(description = "Código obrigatório até 60 e UUID da unidade responsável. Estado é controlado pelo backend.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record TransporteEntradaDto(@NotBlank @Size(max = 60) String codigo, @NotNull UUID unidadeSaudeId) { }
