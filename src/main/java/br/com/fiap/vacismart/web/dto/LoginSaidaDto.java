package br.com.fiap.vacismart.web.dto;

import java.time.Instant;

public record LoginSaidaDto(String token, String tipo, Instant expiraEm, UsuarioSaidaDto usuario) {
    @Override public String toString() { return "LoginSaidaDto[dados omitidos]"; }
}
