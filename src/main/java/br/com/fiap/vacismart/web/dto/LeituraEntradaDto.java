package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@io.swagger.v3.oas.annotations.media.Schema(description = "transporteId deve coincidir com a rota. dispositivoId é texto obrigatório até 100, não uma credencial. Temperatura numeric(10,3), sem arredondamento: 5.2000 aceito, 8.0001 rejeitado. medidaEm tem offset e fica entre início do monitoramento e recepção pelo servidor.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record LeituraEntradaDto(@NotNull UUID transporteId, @NotBlank @Size(max = 100) String dispositivoId,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaC,
        @NotNull OffsetDateTime medidaEm) { }
