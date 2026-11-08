package br.com.fiap.vacismart.domain.rules;

import br.com.fiap.vacismart.domain.entity.Vacina;
import br.com.fiap.vacismart.domain.enums.ClassificacaoTemperatura;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class ClassificadorTemperaturaTest {
    private final ClassificadorTemperatura classificador = new ClassificadorTemperatura();
    private final Vacina demo = vacina("2.0", "8.0", "1.0");

    @ParameterizedTest
    @CsvSource({"5.2,NORMAL", "5.2000,NORMAL", "7.5,ATENCAO", "9.3,FORA_DA_FAIXA", "2.0,ATENCAO",
        "8.0,ATENCAO", "3.0,NORMAL", "7.0,NORMAL", "1.999,FORA_DA_FAIXA",
        "8.001,FORA_DA_FAIXA", "2.999,ATENCAO", "7.001,ATENCAO", "5.200,NORMAL"})
    void respeitaFaixasELimites(String temperatura, ClassificacaoTemperatura esperada) {
        assertThat(classificador.classificar(new BigDecimal(temperatura), demo)).isEqualTo(esperada);
    }

    @ParameterizedTest
    @CsvSource({"8,2,1", "2,2,0", "2,8,-1", "2,8,3.001", "2.0001,8,1", "2,10000000,1"})
    void rejeitaConfiguracaoInvalida(String min, String max, String margem) {
        assertThatIllegalArgumentException().isThrownBy(() -> vacina(min, max, margem));
    }

    @Test
    void usaParametrosDaVacinaSemFixarLimitesDemo() {
        assertThat(classificador.classificar(new BigDecimal("9.3"), vacina("8", "12", "1")))
                .isEqualTo(ClassificacaoTemperatura.NORMAL);
        assertThat(classificador.classificar(new BigDecimal("2"), vacina("2", "8", "0")))
                .isEqualTo(ClassificacaoTemperatura.NORMAL);
        assertThat(classificador.classificar(new BigDecimal("5"), vacina("2", "8", "3")))
                .isEqualTo(ClassificacaoTemperatura.NORMAL);
    }

    @Test
    void rejeitaNulosEDecimalQueSeriaArredondado() {
        assertThatNullPointerException().isThrownBy(() -> classificador.classificar(null, demo));
        assertThatNullPointerException().isThrownBy(() -> classificador.classificar(BigDecimal.ONE, null));
        assertThatNullPointerException().isThrownBy(() -> vacina(null, "8", "1"));
        assertThatIllegalArgumentException().isThrownBy(() -> classificador.classificar(new BigDecimal("8.0001"), demo));
        assertThatIllegalArgumentException().isThrownBy(() -> classificador.classificar(new BigDecimal("10000000"), demo));
    }

    private Vacina vacina(String min, String max, String margem) {
        return new Vacina("Demo", min == null ? null : new BigDecimal(min), new BigDecimal(max), new BigDecimal(margem));
    }
}
