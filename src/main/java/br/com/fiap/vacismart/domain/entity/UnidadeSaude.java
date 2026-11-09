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
@Table(name = "unidade_saude")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnidadeSaude extends Entidade {

    @NotBlank @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    public UnidadeSaude(String nome) {
        this.nome = Validacoes.texto(nome, 150, "Nome");
    }

    public void atualizar(String nome) {
        this.nome = Validacoes.texto(nome, 150, "Nome");
    }
}
