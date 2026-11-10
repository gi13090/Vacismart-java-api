package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.entity.Usuario;
import br.com.fiap.vacismart.domain.enums.Perfil;
import java.util.UUID;

public record UsuarioSaidaDto(UUID id, String nome, String email, Perfil perfil, UUID unidadeSaudeId) {
    public static UsuarioSaidaDto de(Usuario u) {
        return new UsuarioSaidaDto(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getUnidadeSaude() == null ? null : u.getUnidadeSaude().getId());
    }
}
