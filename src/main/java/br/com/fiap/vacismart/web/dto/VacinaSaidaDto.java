package br.com.fiap.vacismart.web.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record VacinaSaidaDto(UUID id, String nome, BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, BigDecimal margemAtencaoC) { }
