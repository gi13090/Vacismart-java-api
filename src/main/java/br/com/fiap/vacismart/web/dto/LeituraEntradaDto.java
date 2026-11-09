package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LeituraEntradaDto(@NotNull UUID transporteId, @NotBlank @Size(max = 100) String dispositivoId,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaC,
        @NotNull OffsetDateTime medidaEm) { }
