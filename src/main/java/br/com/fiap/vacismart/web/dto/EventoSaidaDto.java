package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public record EventoSaidaDto(UUID id, String entidade, UUID referenciaId, String acao, String autor, OffsetDateTime dataHora, String justificativa) { }
