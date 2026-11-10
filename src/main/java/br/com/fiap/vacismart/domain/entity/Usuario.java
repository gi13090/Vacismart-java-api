package br.com.fiap.vacismart.domain.entity;

import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.domain.rules.Validacoes;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends Entidade {
    @Column(nullable = false, length = 150) private String nome;
    @Column(nullable = false, length = 254) private String email;
    @Getter(AccessLevel.NONE)
    @Column(name = "senha_hash", nullable = false, length = 100) private String senhaHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Perfil perfil;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_saude_id") private UnidadeSaude unidadeSaude;
    @Column(nullable = false) private boolean ativo = true;

    public Usuario(String nome, String email, String senhaHash, Perfil perfil, UnidadeSaude unidade) {
        this.nome = Validacoes.texto(nome, 150, "Nome");
        this.email = normalizarEmail(email);
        this.senhaHash = Objects.requireNonNull(senhaHash);
        alterarVinculo(perfil, unidade);
    }
    public static String normalizarEmail(String email) { return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT); }
    public boolean conferirSenha(org.springframework.security.crypto.password.PasswordEncoder encoder, String senha) {
        return encoder.matches(senha, senhaHash);
    }
    public void desativar() { ativo = false; }
    public void alterarVinculo(Perfil perfil, UnidadeSaude unidade) {
        Objects.requireNonNull(perfil);
        if ((perfil == Perfil.FUNCIONARIO) != (unidade != null))
            throw new IllegalArgumentException("Somente funcionário deve possuir unidade obrigatória");
        this.perfil = perfil; this.unidadeSaude = unidade;
    }
}
