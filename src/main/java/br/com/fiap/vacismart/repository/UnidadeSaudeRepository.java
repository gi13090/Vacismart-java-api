package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.UnidadeSaude;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface UnidadeSaudeRepository extends JpaRepository<UnidadeSaude, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from UnidadeSaude e where e.id = :id")
    Optional<UnidadeSaude> buscarParaAtualizacao(@Param("id") UUID id);
}
