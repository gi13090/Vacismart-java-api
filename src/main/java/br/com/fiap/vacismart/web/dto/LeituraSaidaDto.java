package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import br.com.fiap.vacismart.domain.enums.*;

public record LeituraSaidaDto(UUID id, UUID transporteId, String dispositivoId, BigDecimal temperaturaC, OffsetDateTime medidaEm, OffsetDateTime recebidaEm, ClassificacaoTemperatura classificacao) { }
