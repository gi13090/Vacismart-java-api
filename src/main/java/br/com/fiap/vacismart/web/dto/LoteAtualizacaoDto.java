package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

public record LoteAtualizacaoDto(@NotBlank @Size(max = 60) String codigo) { }
