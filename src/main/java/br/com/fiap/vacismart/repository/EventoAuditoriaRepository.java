package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.EventoAuditoria;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, UUID> {
    List<EventoAuditoria> findByReferenciaIdOrderByDataHoraAsc(UUID referenciaId);
    org.springframework.data.domain.Page<EventoAuditoria> findByEntidadeAndReferenciaId(
            String entidade, UUID referenciaId, org.springframework.data.domain.Pageable pageable);
    boolean existsByReferenciaId(UUID referenciaId);
}
