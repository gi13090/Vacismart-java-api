package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.DecisaoTecnica;
import jakarta.validation.constraints.*;

@io.swagger.v3.oas.annotations.media.Schema(description = "Decisão LIBERAR ou DESCARTAR, com justificativa obrigatória, não branca, até 1000 caracteres. Autor e horário vêm do servidor.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record DecisaoTecnicaEntradaDto(@NotNull DecisaoTecnica decisao,
        @NotBlank @Size(max = 1000) String justificativa) { }
