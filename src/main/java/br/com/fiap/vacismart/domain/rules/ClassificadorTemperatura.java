package br.com.fiap.vacismart.domain.rules;

import br.com.fiap.vacismart.domain.entity.Vacina;
import br.com.fiap.vacismart.domain.enums.ClassificacaoTemperatura;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Objects;
import static br.com.fiap.vacismart.domain.enums.ClassificacaoTemperatura.*;

@Component
public class ClassificadorTemperatura {
    public ClassificacaoTemperatura classificar(BigDecimal temperaturaC, Vacina vacina) {
        Objects.requireNonNull(vacina, "Vacina obrigatória");
        Validacoes.decimal(temperaturaC);
        BigDecimal min = vacina.getTemperaturaMinC();
        BigDecimal max = vacina.getTemperaturaMaxC();
        BigDecimal margem = vacina.getMargemAtencaoC();
        Validacoes.faixa(min, max, margem);
        if (temperaturaC.compareTo(min) < 0 || temperaturaC.compareTo(max) > 0) return FORA_DA_FAIXA;
        if (temperaturaC.compareTo(min.add(margem)) < 0
                || temperaturaC.compareTo(max.subtract(margem)) > 0) return ATENCAO;
        return NORMAL;
    }
}
