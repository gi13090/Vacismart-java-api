package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

@io.swagger.v3.oas.annotations.media.Schema(description = "Código obrigatório até 60; UUIDs de vacina e transporte obrigatórios. Cadastro somente antes do monitoramento.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record LoteEntradaDto(@NotBlank @Size(max = 60) String codigo, @NotNull UUID vacinaId, @NotNull UUID transporteId) { }
