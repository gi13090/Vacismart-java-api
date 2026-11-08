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
@Table(name = "evento_auditoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@org.hibernate.annotations.Immutable
public class EventoAuditoria extends Entidade {

    @NotBlank @Size(max = 60)
    @Column(nullable = false, length = 60, updatable = false)
    private String entidade;

    @NotNull @Column(nullable = false, updatable = false)
    private java.util.UUID referenciaId;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100, updatable = false)
    private String acao;

    @NotBlank @Size(max = 100)
    @Column(nullable = false, length = 100, updatable = false)
    private String autor;

    @NotNull @Column(nullable = false, updatable = false)
    private OffsetDateTime dataHora;

    @Size(max = 1000)
    @Column(length = 1000, updatable = false)
    private String justificativa;

    public EventoAuditoria(String entidade, java.util.UUID referenciaId, String acao,
            String autor, OffsetDateTime dataHora, String justificativa) {
        this.entidade = Validacoes.texto(entidade, 60, "Entidade");
        this.referenciaId = Objects.requireNonNull(referenciaId);
        this.acao = Validacoes.texto(acao, 100, "Ação");
        this.autor = Validacoes.texto(autor, 100, "Autor");
        this.dataHora = Objects.requireNonNull(dataHora);
        if (justificativa != null && justificativa.length() > 1000) {
            throw new IllegalArgumentException("Justificativa com até 1000 caracteres");
        }
        this.justificativa = justificativa;
    }

}
