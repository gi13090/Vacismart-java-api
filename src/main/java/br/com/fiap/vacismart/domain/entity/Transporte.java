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
@Table(name = "transporte")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transporte extends Entidade {

    @NotBlank @Size(max = 60)
    @Column(nullable = false, length = 60, unique = true)
    private String codigo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_saude_id", nullable = false)
    private UnidadeSaude unidadeSaude;

    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoTransporte estado = EstadoTransporte.CADASTRADO;

    private OffsetDateTime inicioMonitoramentoEm;

    public Transporte(String codigo, UnidadeSaude unidadeSaude) {
        this.codigo = Validacoes.texto(codigo, 60, "Código");
        this.unidadeSaude = Objects.requireNonNull(unidadeSaude);
    }

    public void iniciarMonitoramento(OffsetDateTime agora) {
        if (estado != EstadoTransporte.CADASTRADO) {
            throw new IllegalStateException("Somente transporte CADASTRADO pode iniciar monitoramento");
        }
        inicioMonitoramentoEm = Objects.requireNonNull(agora);
        estado = EstadoTransporte.EM_TRANSPORTE;
    }

    public void receber() {
        if (estado != EstadoTransporte.EM_TRANSPORTE) {
            throw new IllegalStateException("Recebimento exige transporte EM_TRANSPORTE");
        }
        estado = EstadoTransporte.RECEBIDO;
    }

    public void atualizar(String codigo, UnidadeSaude unidadeSaude) {
        if (estado != EstadoTransporte.CADASTRADO) {
            throw new IllegalStateException("Somente transporte CADASTRADO pode ser atualizado");
        }
        this.codigo = Validacoes.texto(codigo, 60, "Código");
        this.unidadeSaude = Objects.requireNonNull(unidadeSaude);
    }
}
