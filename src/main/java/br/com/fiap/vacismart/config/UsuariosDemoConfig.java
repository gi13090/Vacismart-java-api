package br.com.fiap.vacismart.config;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
@Profile("dev & !prod & !test")
@ConditionalOnProperty(name = "vacismart.demo.habilitado", havingValue = "true")
public class UsuariosDemoConfig implements ApplicationRunner {
    private final Environment env;
    private final UsuarioRepository usuarios;
    private final UnidadeSaudeRepository unidades;
    private final PasswordEncoder senhas;
    public UsuariosDemoConfig(Environment env, UsuarioRepository usuarios, UnidadeSaudeRepository unidades, PasswordEncoder senhas) {
        this.env = env; this.usuarios = usuarios; this.unidades = unidades; this.senhas = senhas;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (!Set.of(env.getActiveProfiles()).equals(Set.of("dev"))
                || !"DADOS_FICTICIOS_LOCAIS".equals(env.getProperty("VACISMART_DEMO_CONFIRMACAO")))
            throw new IllegalStateException("Provisionamento exige somente dev e confirmação local explícita");
        List<String> papeis = List.of("CIDADAO", "FUNCIONARIO_A", "FUNCIONARIO_B", "GESTOR");
        for (String papel : papeis) {
            String email = env.getRequiredProperty("DEMO_" + papel + "_EMAIL");
            String senha = env.getRequiredProperty("DEMO_" + papel + "_SENHA");
            if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 254
                    || !senha.matches("[\\x20-\\x7E]{12,72}"))
                throw new IllegalArgumentException("Credenciais fictícias inválidas para " + papel);
        }
        for (String papel : papeis) {
            String email = Usuario.normalizarEmail(env.getRequiredProperty("DEMO_" + papel + "_EMAIL"));
            if (usuarios.existsByEmail(email)) throw new IllegalStateException("E-mail de demonstração já existente; desative o bootstrap");
        }
        var a = unidades.save(new UnidadeSaude("Unidade fictícia A"));
        var b = unidades.save(new UnidadeSaude("Unidade fictícia B"));
        for (String papel : papeis) {
            Perfil perfil = papel.startsWith("FUNCIONARIO") ? Perfil.FUNCIONARIO : Perfil.valueOf(papel);
            usuarios.save(new Usuario("Demonstração " + papel, env.getRequiredProperty("DEMO_" + papel + "_EMAIL"),
                    senhas.encode(env.getRequiredProperty("DEMO_" + papel + "_SENHA")), perfil,
                    papel.equals("FUNCIONARIO_A") ? a : papel.equals("FUNCIONARIO_B") ? b : null));
        }
    }
}
