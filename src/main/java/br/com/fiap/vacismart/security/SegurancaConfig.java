package br.com.fiap.vacismart.security;

import br.com.fiap.vacismart.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;

@Configuration
public class SegurancaConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean JwtEncoder jwtEncoder(KeyPair chaves) {
        var rsa = new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) chaves.getPublic()).privateKey(chaves.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }
    @Bean JwtDecoder jwtDecoder(KeyPair chaves, Clock relogio, @Value("${vacismart.jwt.emissor}") String emissor) {
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) chaves.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        var tempo = new JwtTimestampValidator(Duration.ZERO);
        tempo.setClock(relogio);
        OAuth2TokenValidator<Jwt> obrigatorios = jwt -> {
            if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null || jwt.getSubject() == null
                    || jwt.getClaimAsString("perfil") == null || jwt.getIssuedAt().isAfter(relogio.instant()))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            try { UUID.fromString(jwt.getSubject()); }
            catch (IllegalArgumentException ex) { return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")); }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(tempo, new JwtIssuerValidator(emissor), obrigatorios));
        return decoder;
    }
    @Bean CorsConfigurationSource cors(@Value("${vacismart.cors.origens:}") String origens) {
        var cors = new CorsConfiguration();
        List<String> permitidas = Arrays.stream(origens.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
        for (String origem : permitidas) {
            var uri = java.net.URI.create(origem);
            if (origem.contains("*") || !List.of("http", "https").contains(uri.getScheme())
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || (uri.getPath() != null && !uri.getPath().isEmpty()))
                throw new IllegalArgumentException("Origem CORS deve ser explícita, sem caminho ou curinga");
        }
        cors.setAllowedOrigins(permitidas);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        cors.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", cors); return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarios,
            CorsConfigurationSource cors, org.springframework.core.env.Environment ambiente) throws Exception {
        return http.csrf(c -> c.disable()).cors(c -> c.configurationSource(cors))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(a -> {
                    // Só a documentação local é pública; os matchers de negócio abaixo são preservados.
                    var documentacao = a.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                            "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config");
                    if (ambiente.acceptsProfiles(org.springframework.core.env.Profiles.of("(dev | test) & !prod & !production"))) {
                        documentacao.permitAll();
                    } else {
                        documentacao.denyAll();
                    }
                    a
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastro", "/api/v1/auth/login").permitAll()
                    .requestMatchers("/api/v1/auth/me").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidade").hasAnyRole("CIDADAO", "FUNCIONARIO", "GESTOR")
                    .anyRequest().hasAnyRole("FUNCIONARIO", "GESTOR");
                })
                .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) -> ErrosSeguranca.escrever(res, 401))
                    .accessDeniedHandler((req, res, ex) -> ErrosSeguranca.escrever(res, 403)))
                .oauth2ResourceServer(o -> o
                    .authenticationEntryPoint((req, res, ex) -> ErrosSeguranca.escrever(res, 401))
                    .accessDeniedHandler((req, res, ex) -> ErrosSeguranca.escrever(res, 403))
                    .jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                        var usuario = usuarios.findById(UUID.fromString(jwt.getSubject()))
                                .filter(u -> u.isAtivo()).orElseThrow(() -> new BadCredentialsException("Identidade inválida"));
                        // O perfil do token é informativo: privilégios vêm do banco em toda requisição.
                        return new JwtAuthenticationToken(jwt,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())), usuario.getId().toString());
                    }))).build();
    }
}
