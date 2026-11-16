package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public record EventoSaidaDto(UUID id, String entidade, UUID referenciaId, String acao, String autor, OffsetDateTime dataHora, @io.swagger.v3.oas.annotations.media.Schema(nullable = true, description = "Justificativa do evento, quando existente.") String justificativa) { }
