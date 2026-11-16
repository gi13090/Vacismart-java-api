package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.DisponibilidadeLote;
import jakarta.validation.constraints.NotNull;

@io.swagger.v3.oas.annotations.media.Schema(description = "Estado desejado CONFIRMADA ou NAO_DISPONIVEL. Requer transporte RECEBIDO e lote LIBERADO mesmo na repetição idempotente.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record DisponibilidadeEntradaDto(@NotNull DisponibilidadeLote disponibilidade) { }
