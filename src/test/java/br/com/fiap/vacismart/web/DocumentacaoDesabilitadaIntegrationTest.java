package br.com.fiap.vacismart.web;

import br.com.fiap.vacismart.security.ChavesTesteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:vacismart-openapi-desabilitada;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@ActiveProfiles({"test", "prod"})
@Import(ChavesTesteConfig.class)
class DocumentacaoDesabilitadaIntegrationTest {
    @Autowired Environment ambiente;
    @Autowired RequestMappingHandlerMapping mappings;
    @LocalServerPort int porta;
    @Autowired br.com.fiap.vacismart.repository.UsuarioRepository usuarios;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder senhas;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder jwt;
    @Autowired java.time.Clock relogio;

    @Test void prodPrevaleceSobreTestSemRegistrarOuLiberarDocumentacao() throws Exception {
        assertThat(ambiente.getProperty("springdoc.api-docs.enabled",Boolean.class)).isFalse();
        assertThat(ambiente.getProperty("springdoc.swagger-ui.enabled",Boolean.class)).isFalse();
        assertThat(mappings.getHandlerMethods().values()).noneMatch(h -> h.getBeanType().getPackageName().startsWith("org.springdoc"));
        var gestor = usuarios.saveAndFlush(new br.com.fiap.vacismart.domain.entity.Usuario("Gestor fictício", "swagger-prod@teste.invalid",
                senhas.encode("FicticiaSomenteTeste!123"), br.com.fiap.vacismart.domain.enums.Perfil.GESTOR, null));
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().subject(gestor.getId().toString())
                .issuer("vacismart").issuedAt(relogio.instant()).expiresAt(relogio.instant().plusSeconds(900)).claim("perfil", "GESTOR").build();
        String token = jwt.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        try (var cliente = HttpClient.newHttpClient()) {
            for (String rota : new String[]{"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/swagger-ui-bundle.js"}) {
                var res = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+rota)).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertThat(res.statusCode()).isEqualTo(401);
                assertThat(res.body()).doesNotContain("openapi", "SwaggerUIBundle");
                var autenticada = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+rota))
                        .header("Authorization", "Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertThat(autenticada.statusCode()).isEqualTo(403);
                assertThat(autenticada.body()).doesNotContain("openapi", "SwaggerUIBundle");
            }
            var conta = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/auth/me"))
                    .header("Authorization", "Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(conta.statusCode()).isEqualTo(200);
        }
    }
}
