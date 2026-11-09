package br.com.fiap.vacismart.web.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PaginaDto<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {
    public static <T> PaginaDto<T> de(Page<T> pagina) {
        return new PaginaDto<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
