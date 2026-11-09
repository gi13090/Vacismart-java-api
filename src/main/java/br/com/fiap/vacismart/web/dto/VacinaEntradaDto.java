package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record VacinaEntradaDto(@NotBlank @Size(max = 150) String nome,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaMinC,
        @NotNull @DecimalMin("-9999999.999") @DecimalMax("9999999.999") BigDecimal temperaturaMaxC,
        @NotNull @DecimalMin("0") @DecimalMax("9999999.999") BigDecimal margemAtencaoC) { }
