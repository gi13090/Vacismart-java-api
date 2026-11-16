package br.com.fiap.vacismart.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado exclusivamente demonstrativo; não autoriza nem registra administração real de vacina.")
public record AplicacaoSaidaDto(
        @Schema(allowableValues = {"PERMITIDA"}) String resultado,
        String mensagem) { }
