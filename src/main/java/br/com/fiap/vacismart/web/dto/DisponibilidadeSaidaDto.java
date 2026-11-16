package br.com.fiap.vacismart.web.dto;

import br.com.fiap.vacismart.domain.enums.SituacaoDisponibilidade;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DisponibilidadeSaidaDto(ReferenciaPublica vacina, ReferenciaPublica unidade,
        SituacaoDisponibilidade situacao, @io.swagger.v3.oas.annotations.media.Schema(nullable = true, description = "Maior horário de confirmação/retirada dos lotes do par; null sem evento pertinente. Não é o horário da consulta.") OffsetDateTime atualizadoEm, String mensagem) {
    public record ReferenciaPublica(UUID id, String nome) { }
}
