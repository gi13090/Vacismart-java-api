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
@Table(name = "lote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lote extends Entidade {

    @NotBlank @Size(max = 60)
    @Column(nullable = false, length = 60, unique = true)
    private String codigo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacina_id", nullable = false)
    private Vacina vacina;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporte_id", nullable = false)
    private Transporte transporte;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoLote estado = EstadoLote.EM_MONITORAMENTO;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisponibilidadeLote disponibilidade = DisponibilidadeLote.NAO_DISPONIVEL;

    public Lote(String codigo, Vacina vacina, Transporte transporte) {
        this.codigo = Validacoes.texto(codigo, 60, "Código");
        this.vacina = Objects.requireNonNull(vacina);
        this.transporte = Objects.requireNonNull(transporte);
    }

    public boolean bloquearPreventivamente() {
        if (estado != EstadoLote.EM_MONITORAMENTO) return false;
        estado = EstadoLote.BLOQUEADO_PREVENTIVAMENTE;
        disponibilidade = DisponibilidadeLote.NAO_DISPONIVEL;
        return true;
    }

}
