package br.com.fiap.vacismart.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Verificação demonstrativa de aplicação; identidade e unidade são obtidas da autenticação, sem dados clínicos.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record AplicacaoEntradaDto(@NotNull UUID loteId) { }
