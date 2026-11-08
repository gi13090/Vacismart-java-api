package br.com.fiap.vacismart.domain.rules;

import java.math.BigDecimal;
import java.util.Objects;

/** Valida dados antes de qualquer arredondamento pelo banco (numeric(10,3)). */
public final class Validacoes {
    private Validacoes() { }

    public static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " obrigatório, com até " + maximo + " caracteres");
        }
        return valor;
    }

    public static BigDecimal decimal(BigDecimal valor) {
        Objects.requireNonNull(valor, "Temperatura/parâmetro obrigatório");
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (normalizado.scale() > 3 || normalizado.precision() - normalizado.scale() > 7) {
            throw new IllegalArgumentException("Decimal deve caber em numeric(10,3), sem arredondamento");
        }
        return normalizado;
    }

    public static void faixa(BigDecimal min, BigDecimal max, BigDecimal margem) {
        decimal(min); decimal(max); decimal(margem);
        if (min.compareTo(max) >= 0 || margem.signum() < 0
                || margem.multiply(BigDecimal.valueOf(2)).compareTo(max.subtract(min)) > 0) {
            throw new IllegalArgumentException("Faixa inválida: min < max, margem >= 0 e 2*margem <= max-min");
        }
    }
}
