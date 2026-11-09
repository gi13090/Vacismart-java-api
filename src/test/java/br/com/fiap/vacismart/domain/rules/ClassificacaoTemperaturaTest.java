package br.com.fiap.vacismart.domain.rules;

import br.com.fiap.vacismart.domain.enums.ClassificacaoTemperatura;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoTemperaturaTest {
    @ParameterizedTest
    @CsvSource({"NORMAL,0", "ATENCAO,10", "FORA_DA_FAIXA,20"})
    void prioridadeExplicita(ClassificacaoTemperatura classificacao, int prioridade) {
        assertThat(classificacao.getPrioridade()).isEqualTo(prioridade);
    }

    @ParameterizedTest
    @CsvSource({"NORMAL,NORMAL,false", "NORMAL,ATENCAO,false", "NORMAL,FORA_DA_FAIXA,false",
        "ATENCAO,NORMAL,true", "ATENCAO,ATENCAO,false", "ATENCAO,FORA_DA_FAIXA,false",
        "FORA_DA_FAIXA,NORMAL,true", "FORA_DA_FAIXA,ATENCAO,true", "FORA_DA_FAIXA,FORA_DA_FAIXA,false"})
    void comparaGravidade(ClassificacaoTemperatura atual, ClassificacaoTemperatura outra, boolean esperada) {
        assertThat(atual.maisGraveQue(outra)).isEqualTo(esperada);
    }
}
