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

    public boolean encaminharParaConferencia() {
        if (estado != EstadoLote.EM_MONITORAMENTO) return false;
        estado = EstadoLote.AGUARDANDO_CONFERENCIA;
        return true;
    }

    public void decidirTecnicamente(DecisaoTecnica decisao, String justificativa) {
        if (decisao == null) throw new IllegalArgumentException("Decisão técnica obrigatória");
        Validacoes.texto(justificativa, 1000, "Justificativa");
        if (transporte.getEstado() != EstadoTransporte.RECEBIDO) {
            throw new IllegalStateException("Decisão técnica exige transporte RECEBIDO");
        }
        if (estado != EstadoLote.AGUARDANDO_CONFERENCIA && estado != EstadoLote.BLOQUEADO_PREVENTIVAMENTE) {
            throw new IllegalStateException("Decisão técnica exige lote AGUARDANDO_CONFERENCIA ou BLOQUEADO_PREVENTIVAMENTE");
        }
        estado = switch (decisao) {
            case LIBERAR -> EstadoLote.LIBERADO;
            case DESCARTAR -> EstadoLote.DESCARTADO;
        };
        disponibilidade = DisponibilidadeLote.NAO_DISPONIVEL;
    }

    public boolean alterarDisponibilidade(DisponibilidadeLote destino) {
        if (destino == null) throw new IllegalArgumentException("Disponibilidade obrigatória");
        if (transporte.getEstado() != EstadoTransporte.RECEBIDO) {
            throw new IllegalStateException("Alteração de disponibilidade exige transporte RECEBIDO");
        }
        if (estado != EstadoLote.LIBERADO) {
            throw new IllegalStateException("Alteração de disponibilidade exige lote LIBERADO");
        }
        if (disponibilidade == destino) return false;
        disponibilidade = destino;
        return true;
    }

    public void atualizarCodigo(String codigo) {
        if (estado != EstadoLote.EM_MONITORAMENTO || transporte.getEstado() != EstadoTransporte.CADASTRADO) {
            throw new IllegalStateException("Lote somente pode ser atualizado antes do monitoramento");
        }
        this.codigo = Validacoes.texto(codigo, 60, "Código");
    }
}
