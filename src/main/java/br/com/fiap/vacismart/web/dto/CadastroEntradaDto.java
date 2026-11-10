package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

public record CadastroEntradaDto(@NotBlank @Size(max = 150) String nome, @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(min = 12, max = 72) @Pattern(regexp = "[\\x20-\\x7E]+", message = "Use caracteres ASCII imprimíveis") String senha) {
    @Override public String toString() { return "CadastroEntradaDto[dados omitidos]"; }
}
