package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.SituacaoDisponibilidade;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DisponibilidadeSaidaDto(ReferenciaPublica vacina, ReferenciaPublica unidade,
        SituacaoDisponibilidade situacao, OffsetDateTime atualizadoEm, String mensagem) {
    public record ReferenciaPublica(UUID id, String nome) { }
}
