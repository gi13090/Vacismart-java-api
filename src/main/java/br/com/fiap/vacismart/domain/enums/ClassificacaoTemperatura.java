package br.com.fiap.vacismart.domain.enums;

public enum ClassificacaoTemperatura {
    NORMAL(0), ATENCAO(10), FORA_DA_FAIXA(20);

    private final int prioridade;

    ClassificacaoTemperatura(int prioridade) { this.prioridade = prioridade; }

    public int getPrioridade() { return prioridade; }

    public boolean maisGraveQue(ClassificacaoTemperatura outra) {
        return prioridade > outra.prioridade;
    }
}
