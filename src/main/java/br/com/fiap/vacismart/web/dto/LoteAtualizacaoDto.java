package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

@io.swagger.v3.oas.annotations.media.Schema(description = "Somente o código obrigatório até 60; vacina, transporte, estado e disponibilidade não são editáveis por este DTO.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record LoteAtualizacaoDto(@NotBlank @Size(max = 60) String codigo) { }
