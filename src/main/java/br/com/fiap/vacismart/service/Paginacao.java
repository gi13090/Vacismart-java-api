package br.com.fiap.vacismart.service;

import org.springframework.data.domain.*;
import java.util.Set;

public final class Paginacao {
    private Paginacao() { }
    public static Pageable validar(Pageable pagina, String... campos) {
        Set<String> permitidos = Set.of(campos);
        for (Sort.Order ordem : pagina.getSort()) {
            if (!permitidos.contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação não permitido");
            }
        }
        Sort sort = pagina.getSort();
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        return PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), 100), sort);
    }
}
