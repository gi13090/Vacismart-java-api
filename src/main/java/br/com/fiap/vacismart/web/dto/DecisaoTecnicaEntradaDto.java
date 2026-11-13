package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.DecisaoTecnica;
import jakarta.validation.constraints.*;

public record DecisaoTecnicaEntradaDto(@NotNull DecisaoTecnica decisao,
        @NotBlank @Size(max = 1000) String justificativa) { }
