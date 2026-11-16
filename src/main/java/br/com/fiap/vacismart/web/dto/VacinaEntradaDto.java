package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@io.swagger.v3.oas.annotations.media.Schema(description = "Parâmetros por vacina: numeric(10,3), até sete dígitos inteiros e três casas significativas, sem arredondar. Mínimo < máximo; margem >= 0 e duas vezes a margem <= amplitude. Margem obrigatória, sem default na entrada.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record VacinaEntradaDto(@NotBlank @Size(max = 150) String nome,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaMinC,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaMaxC,
        @NotNull @DecimalMin("0") @DecimalMax("9999999.999") BigDecimal margemAtencaoC) { }
