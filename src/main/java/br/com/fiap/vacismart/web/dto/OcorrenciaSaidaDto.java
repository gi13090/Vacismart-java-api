package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.time.OffsetDateTime;
import br.com.fiap.vacismart.domain.enums.*;

public record OcorrenciaSaidaDto(UUID id, UUID transporteId, TipoOcorrencia tipo, EstadoOcorrencia estado, OffsetDateTime abertaEm, OffsetDateTime encerradaEm) { }
