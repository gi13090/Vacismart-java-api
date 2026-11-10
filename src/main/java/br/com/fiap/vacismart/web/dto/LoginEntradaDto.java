package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

public record LoginEntradaDto(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) @Pattern(regexp = "[\\x20-\\x7E]+") String senha) {
    @Override public String toString() { return "LoginEntradaDto[dados omitidos]"; }
}
