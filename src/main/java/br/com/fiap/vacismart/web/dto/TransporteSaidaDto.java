package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.time.OffsetDateTime;
import br.com.fiap.vacismart.domain.enums.*;

public record TransporteSaidaDto(UUID id, String codigo, UUID unidadeSaudeId, EstadoTransporte estado, OffsetDateTime inicioMonitoramentoEm) { }
