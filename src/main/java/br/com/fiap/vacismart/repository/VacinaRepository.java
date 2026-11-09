package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.Vacina;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface VacinaRepository extends JpaRepository<Vacina, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Vacina e where e.id = :id")
    Optional<Vacina> buscarParaAtualizacao(@Param("id") UUID id);
}
