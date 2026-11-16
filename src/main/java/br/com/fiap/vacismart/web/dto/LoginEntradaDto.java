package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

@io.swagger.v3.oas.annotations.media.Schema(description = "Autenticação de conta ativa. E-mail e senha obrigatórios; login não exige o mínimo de 12 caracteres do cadastro.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record LoginEntradaDto(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) @Pattern(regexp = "[\\x20-\\x7E]+") @io.swagger.v3.oas.annotations.media.Schema(accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY, format = "password", description = "Senha fictícia informada localmente; nunca registrar em logs") String senha) {
    @Override public String toString() { return "LoginEntradaDto[dados omitidos]"; }
}
