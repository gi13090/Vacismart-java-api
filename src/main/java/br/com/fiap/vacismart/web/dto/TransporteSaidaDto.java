package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.time.OffsetDateTime;
import br.com.fiap.vacismart.domain.enums.*;

public record TransporteSaidaDto(UUID id, String codigo, UUID unidadeSaudeId, EstadoTransporte estado, @io.swagger.v3.oas.annotations.media.Schema(nullable = true, description = "Null enquanto o monitoramento não foi iniciado.") OffsetDateTime inicioMonitoramentoEm) { }
