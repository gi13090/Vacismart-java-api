package br.com.fiap.vacismart.web;

import br.com.fiap.vacismart.domain.entity.Usuario;
import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.repository.UsuarioRepository;
import br.com.fiap.vacismart.security.ChavesTesteConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:vacismart-openapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@ActiveProfiles("test")
@Import(ChavesTesteConfig.class)
class OpenApiIntegrationTest {
    @LocalServerPort int porta;
    @Autowired ObjectMapper json;
    @Autowired RequestMappingHandlerMapping mappings;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder senhas;
    HttpClient cliente;
    @BeforeEach void abrir() { cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(); }
    @AfterEach void fechar() { cliente.close(); }

    HttpResponse<String> get(String rota) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + rota)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode documento() throws Exception {
        var res = get("/v3/api-docs");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Content-Type").orElse("")).contains("application/json");
        return json.readTree(res.body());
    }
    JsonNode schema(JsonNode doc, String nome) { return doc.at("/components/schemas/" + nome); }

    @Test void documentaExatamenteOs34ParesReaisSemRotasFuturasOuDeTeste() throws Exception {
        var doc = documento();
        Set<String> reais = new TreeSet<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (handler.getBeanType().getPackageName().equals("br.com.fiap.vacismart.web.controller")) {
                mapping.getPatternValues().forEach(rota -> mapping.getMethodsCondition().getMethods()
                        .forEach(metodo -> reais.add(metodo.name().toLowerCase(Locale.ROOT) + " " + rota)));
            }
        });
        Set<String> documentadas = new TreeSet<>();
        doc.get("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
            documentadas.add(op.getKey() + " " + path.getKey());
            assertThat(op.getValue().path("summary").asString()).isNotBlank();
            assertThat(op.getValue().path("description").asString()).isNotBlank();
        }));
        assertThat(reais).hasSize(34);
        assertThat(documentadas).isEqualTo(reais);
        assertThat(doc.get("paths").toString()).doesNotContain("/__teste", "/finalizacao", "/caderneta");
        assertThat(doc.path("openapi").asString()).startsWith("3.0.");
        assertThat(doc.at("/info/title").asString()).contains("VaciSmart");
        assertThat(doc.at("/info/version").asString()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test void documentaAplicacaoSimuladaComEntradaResultadoEErros() throws Exception {
        var doc = documento();
        var op = doc.get("paths").get("/api/v1/aplicacoes").get("post");
        assertThat(op.path("description").asString()).contains("demonstrativa", "FUNCIONARIO", "GESTOR", "commit");
        assertThat(op.at("/requestBody/content/application~1json/schema/$ref").asString()).endsWith("AplicacaoEntradaDto");
        var entrada = schema(doc, "AplicacaoEntradaDto");
        assertThat(entrada.path("properties").propertyNames()).containsExactly("loteId");
        assertThat(entrada.path("required").toString()).contains("loteId");
        assertThat(entrada.at("/properties/loteId/format").asString()).isEqualTo("uuid");
        assertThat(entrada.path("additionalProperties").asBoolean()).isFalse();
        assertThat(op.at("/responses/200/content/application~1json/schema/$ref").asString()).endsWith("AplicacaoSaidaDto");
        assertThat(schema(doc, "AplicacaoSaidaDto").path("properties").propertyNames()).containsExactlyInAnyOrder("resultado", "mensagem");
        for (String codigo : List.of("400", "401", "403", "404", "409"))
            assertThat(op.at("/responses/" + codigo + "/content/application~1problem+json/schema/$ref").asString()).isEqualTo("#/components/schemas/ProblemaApi");
        assertThat(op.at("/responses/409/description").asString()).contains("auditoria persistida");
    }

    @Test void todasAsReferenciasDeSchemasSaoResolvidas() throws Exception {
        var doc = documento();
        conferirReferencias(doc, doc);
    }
    private void conferirReferencias(JsonNode raiz, JsonNode atual) {
        if (atual.isObject() && atual.has("$ref")) {
            String ref = atual.get("$ref").asString();
            assertThat(ref).startsWith("#/components/");
            assertThat(raiz.at(ref.substring(1)).isMissingNode()).as(ref).isFalse();
        }
        if (atual.isArray() || atual.isObject()) atual.forEach(filho -> conferirReferencias(raiz, filho));
    }

    @Test void descreveBearerSemExigirTokenNoCadastroOuLogin() throws Exception {
        var doc = documento();
        assertThat(doc.at("/components/securitySchemes/bearerJwt/type").asString()).isEqualTo("http");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/scheme").asString()).isEqualTo("bearer");
        assertThat(doc.at("/components/securitySchemes/bearerJwt/bearerFormat").asString()).isEqualTo("JWT");
        assertThat(doc.get("security").get(0).has("bearerJwt")).isTrue();
        for (String rota : List.of("/api/v1/auth/login", "/api/v1/auth/cadastro"))
            assertThat(doc.get("paths").get(rota).get("post").get("security").size()).isZero();
        assertThat(doc.get("paths").get("/api/v1/auth/me").get("get").has("security")).isFalse(); // herda o requisito global
    }

    @Test void respostasPreservamCriacaoSemCorpoEPaginacaoReal() throws Exception {
        var doc = documento();
        var paths = doc.get("paths");
        var criado = paths.get("/api/v1/lotes").get("post").get("responses");
        assertThat(criado.has("201")).isTrue();
        assertThat(criado.has("404")).isTrue(); // vacina/transporte do corpo inexistentes
        assertThat(paths.get("/api/v1/transportes").get("post").get("responses").has("404")).isTrue();
        assertThat(criado.has("200")).isFalse();
        assertThat(criado.at("/201/headers/Location").isMissingNode()).isFalse();
        assertThat(criado.at("/201/content/application~1json/schema").toString()).contains("LoteSaidaDto");
        for (String rota : List.of("/api/v1/transportes/{id}/recebimento", "/api/v1/lotes/{id}/decisao-tecnica", "/api/v1/lotes/{id}/disponibilidade")) {
            var responses = paths.get(rota).get("post").get("responses");
            assertThat(responses.has("204")).isTrue();
            assertThat(responses.get("204").has("content")).isFalse();
        }
        var disponibilidade = paths.get("/api/v1/disponibilidade").get("get");
        Set<String> parametros = new HashSet<>();
        disponibilidade.get("parameters").forEach(p -> parametros.add(p.get("name").asString()));
        assertThat(parametros).containsExactlyInAnyOrder("unidadeId", "vacinaId", "page", "size", "sort");
        var schemaPagina = disponibilidade.at("/responses/200/content/application~1json/schema/$ref").asString();
        var pagina = doc.at(schemaPagina.substring(1));
        assertThat(pagina.path("properties").propertyNames()).contains("conteudo", "pagina", "tamanho", "totalElementos", "totalPaginas");
        assertThat(disponibilidade.get("description").asString()).contains("unidadeNome", "vacinaNome", "CIDADAO");
    }

    @Test void schemasSaoDtosComValidacoesNulabilidadeEPrivacidade() throws Exception {
        var doc = documento();
        var cadastro = schema(doc, "CadastroEntradaDto");
        assertThat(cadastro.path("additionalProperties").asBoolean()).isFalse();
        assertThat(cadastro.get("required").toString()).contains("nome", "email", "senha");
        assertThat(cadastro.at("/properties/senha/writeOnly").asBoolean()).isTrue();
        assertThat(cadastro.at("/properties/senha/minLength").asInt()).isEqualTo(12);
        assertThat(cadastro.at("/properties/senha/maxLength").asInt()).isEqualTo(72);
        assertThat(schema(doc,"LeituraEntradaDto").at("/properties/temperaturaC/maximum").asDouble()).isEqualTo(9999999.999);
        assertThat(schema(doc,"LeituraEntradaDto").at("/properties/transporteId/format").asString()).isEqualTo("uuid");
        assertThat(schema(doc,"DisponibilidadeSaidaDto").at("/properties/atualizadoEm/nullable").asBoolean()).isTrue();
        assertThat(schema(doc,"DisponibilidadeSaidaDto").path("properties").propertyNames())
                .containsExactlyInAnyOrder("vacina", "unidade", "situacao", "atualizadoEm", "mensagem");
        assertThat(doc.at("/components/schemas").propertyNames()).doesNotContain("Usuario", "Lote", "Transporte", "Vacina", "EventoAuditoria", "Pageable");
        assertThat(doc.toString()).doesNotContain("senhaHash", "senha_hash", "BEGIN PRIVATE KEY", "jdbc:", "JWT_CHAVE_PRIVADA", "$2a$", "$2b$");
    }

    @Test void documentaProblemDetailSemObrigarInstanceOuCamposNosFiltros() throws Exception {
        var doc = documento();
        var problema = schema(doc,"ProblemaApi");
        assertThat(problema.get("required").toString()).doesNotContain("instance", "campos");
        var respostas = doc.get("paths").get("/api/v1/lotes/{id}/decisao-tecnica").get("post").get("responses");
        for (String status : List.of("400", "401", "403", "404", "409", "415", "500"))
            assertThat(respostas.get(status).at("/content/application~1problem+json/schema/$ref").asString()).isEqualTo("#/components/schemas/ProblemaApi");
        assertThat(respostas.get("401").at("/headers/WWW-Authenticate").isMissingNode()).isFalse();
    }

    @Test void swaggerUiESeusRecursosCarregamComConfiguracaoLocal() throws Exception {
        var html = get("/swagger-ui.html");
        assertThat(html.statusCode()).isEqualTo(200);
        assertThat(html.body()).contains("swagger-ui-bundle.js", "swagger-initializer.js");
        for (String asset : List.of("swagger-ui-bundle.js", "swagger-ui.css", "swagger-initializer.js")) {
            var res = get("/swagger-ui/" + asset);
            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body()).isNotBlank();
        }
        var config = json.readTree(get("/v3/api-docs/swagger-config").body());
        assertThat(config.get("url").asString()).isEqualTo("/v3/api-docs");
        assertThat(config.path("persistAuthorization").asBoolean()).isFalse();
        assertThat(get("/v3/api-docs.yaml").statusCode()).isEqualTo(200);
    }

    @Test void documentacaoPublicaNaoLiberaRotasOperacionaisAoCidadao() throws Exception {
        assertThat(get("/api/v1/lotes").statusCode()).isEqualTo(401);
        String senha = "SomenteTesteFicticio!123";
        Usuario cidadao = usuarios.saveAndFlush(new Usuario("Cidadão teste Swagger", "swagger-"+UUID.randomUUID()+"@teste.invalid", senhas.encode(senha), Perfil.CIDADAO, null));
        var login = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/auth/login"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",cidadao.getEmail(),"senha",senha)))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        String token = json.readTree(login.body()).get("token").asString();
        var resposta = cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/lotes"))
                .header("Authorization", "Bearer "+token).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(resposta.statusCode()).isEqualTo(403);
    }
}
