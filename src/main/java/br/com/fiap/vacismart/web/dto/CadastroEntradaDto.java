package br.com.fiap.vacismart.web.dto;

import jakarta.validation.constraints.*;

@io.swagger.v3.oas.annotations.media.Schema(description = "Cadastro público exclusivo de cidadão. Nome até 150, e-mail até 254, único após normalização; senha 12–72 ASCII imprimíveis.", additionalProperties = io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue.FALSE)
public record CadastroEntradaDto(@NotBlank @Size(max = 150) String nome, @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(min = 12, max = 72) @Pattern(regexp = "[\\x20-\\x7E]+", message = "Use caracteres ASCII imprimíveis") @io.swagger.v3.oas.annotations.media.Schema(accessMode = io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY, format = "password", description = "Senha fictícia informada localmente; nunca registrar em logs") String senha) {
    @Override public String toString() { return "CadastroEntradaDto[dados omitidos]"; }
}
