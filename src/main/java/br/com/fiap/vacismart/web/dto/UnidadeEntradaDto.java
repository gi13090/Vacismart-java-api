package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

public record UnidadeEntradaDto(@NotBlank @Size(max = 150) String nome) { }
