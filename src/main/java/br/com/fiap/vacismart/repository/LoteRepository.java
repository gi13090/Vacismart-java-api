package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.Lote;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface LoteRepository extends JpaRepository<Lote, UUID>, JpaSpecificationExecutor<Lote> {
    @EntityGraph(attributePaths = "vacina")
    List<Lote> findByTransporteId(UUID transporteId);
    boolean existsByVacinaId(UUID vacinaId);
    boolean existsByTransporteId(UUID transporteId);
}
