package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import br.com.fiap.vacismart.domain.enums.*;

public record LoteSaidaDto(UUID id, String codigo, UUID vacinaId, UUID transporteId, EstadoLote estado, DisponibilidadeLote disponibilidade) { }
