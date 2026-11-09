package br.com.fiap.vacismart.repository;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.*;

public final class Filtros {
    private Filtros() { }
    public static Specification<Lote> lotes(UUID transporteId, UUID vacinaId, EstadoLote estado) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (transporteId != null) filtros.add(cb.equal(root.get("transporte").get("id"), transporteId));
            if (vacinaId != null) filtros.add(cb.equal(root.get("vacina").get("id"), vacinaId));
            if (estado != null) filtros.add(cb.equal(root.get("estado"), estado));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
    public static Specification<Transporte> transportes(UUID unidadeId, EstadoTransporte estado) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (unidadeId != null) filtros.add(cb.equal(root.get("unidadeSaude").get("id"), unidadeId));
            if (estado != null) filtros.add(cb.equal(root.get("estado"), estado));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
}
