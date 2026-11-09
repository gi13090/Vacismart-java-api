package br.com.fiap.vacismart.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.domain.rules.Validacoes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "vacina")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vacina extends Entidade {

    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @NotNull @Digits(integer = 7, fraction = 3)
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal temperaturaMinC;

    @NotNull @Digits(integer = 7, fraction = 3)
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal temperaturaMaxC;

    @NotNull @Digits(integer = 7, fraction = 3)
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal margemAtencaoC;

    public Vacina(String nome, BigDecimal min, BigDecimal max, BigDecimal margem) {
        this.nome = Validacoes.texto(nome, 150, "Nome");
        Validacoes.faixa(min, max, margem);
        this.temperaturaMinC = Validacoes.decimal(min);
        this.temperaturaMaxC = Validacoes.decimal(max);
        this.margemAtencaoC = Validacoes.decimal(margem);
    }

    @PrePersist @PreUpdate
    private void validarFaixa() {
        Validacoes.faixa(temperaturaMinC, temperaturaMaxC, margemAtencaoC);
    }

    public void atualizar(String nome, BigDecimal min, BigDecimal max, BigDecimal margem) {
        Validacoes.faixa(min, max, margem);
        this.nome = Validacoes.texto(nome, 150, "Nome");
        this.temperaturaMinC = Validacoes.decimal(min);
        this.temperaturaMaxC = Validacoes.decimal(max);
        this.margemAtencaoC = Validacoes.decimal(margem);
    }
}
