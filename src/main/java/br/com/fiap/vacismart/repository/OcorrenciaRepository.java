package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.Ocorrencia;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {
    Optional<Ocorrencia> findByTransporteIdAndTipoAndEstado(UUID transporteId, TipoOcorrencia tipo, EstadoOcorrencia estado);
}
