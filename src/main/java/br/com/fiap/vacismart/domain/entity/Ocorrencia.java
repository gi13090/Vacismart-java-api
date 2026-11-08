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
@Table(name = "ocorrencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ocorrencia extends Entidade {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporte_id", nullable = false)
    private Transporte transporte;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoOcorrencia tipo;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOcorrencia estado = EstadoOcorrencia.ABERTA;

    @NotNull @Column(nullable = false)
    private OffsetDateTime abertaEm;

    private OffsetDateTime encerradaEm;

    public Ocorrencia(Transporte transporte, TipoOcorrencia tipo, OffsetDateTime abertaEm) {
        this.transporte = Objects.requireNonNull(transporte);
        this.tipo = Objects.requireNonNull(tipo);
        this.abertaEm = Objects.requireNonNull(abertaEm);
    }

    public void encerrar(OffsetDateTime agora) {
        Objects.requireNonNull(agora);
        if (agora.isBefore(abertaEm)) throw new IllegalArgumentException("Encerramento anterior à abertura");
        if (estado != EstadoOcorrencia.ABERTA) throw new IllegalStateException("Ocorrência já encerrada");
        encerradaEm = agora;
        estado = EstadoOcorrencia.ENCERRADA;
    }

}
