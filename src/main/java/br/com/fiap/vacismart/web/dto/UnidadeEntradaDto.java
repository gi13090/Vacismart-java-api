package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

@io.swagger.v3.oas.annotations.media.Schema(description = "Nome obrigatório, não branco, até 150 caracteres.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record UnidadeEntradaDto(@NotBlank @Size(max = 150) String nome) { }
