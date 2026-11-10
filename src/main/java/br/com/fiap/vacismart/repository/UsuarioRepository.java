package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
