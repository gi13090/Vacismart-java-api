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
@Table(name = "leitura_temperatura")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeituraTemperatura extends Entidade {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporte_id", nullable = false)
    private Transporte transporte;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String dispositivoId;

    @NotNull @Digits(integer = 7, fraction = 3)
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal temperaturaC;

    @NotNull @Column(nullable = false)
    private OffsetDateTime medidaEm;

    @NotNull @Column(nullable = false)
    private OffsetDateTime recebidaEm;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClassificacaoTemperatura classificacao;

    public LeituraTemperatura(Transporte transporte, String dispositivoId, BigDecimal temperaturaC,
            OffsetDateTime medidaEm, OffsetDateTime recebidaEm, ClassificacaoTemperatura classificacao) {
        this.transporte = Objects.requireNonNull(transporte);
        this.dispositivoId = Validacoes.texto(dispositivoId, 100, "Dispositivo");
        this.temperaturaC = Validacoes.decimal(temperaturaC);
        this.medidaEm = Objects.requireNonNull(medidaEm);
        this.recebidaEm = Objects.requireNonNull(recebidaEm);
        if (medidaEm.isAfter(recebidaEm)) throw new IllegalArgumentException("Medição futura");
        this.classificacao = Objects.requireNonNull(classificacao);
    }

}
