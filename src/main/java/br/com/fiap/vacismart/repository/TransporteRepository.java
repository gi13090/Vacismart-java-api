package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.Transporte;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface TransporteRepository extends JpaRepository<Transporte, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transporte t where t.id = :id")
    Optional<Transporte> buscarParaAtualizacao(@Param("id") UUID id);
}
