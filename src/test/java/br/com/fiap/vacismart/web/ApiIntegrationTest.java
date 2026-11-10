package br.com.fiap.vacismart.web;

import br.com.fiap.vacismart.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:vacismart-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
@Import({br.com.fiap.vacismart.security.ChavesTesteConfig.class, ApiIntegrationTest.Config.class})
class ApiIntegrationTest {
    @LocalServerPort int porta;
    @Autowired br.com.fiap.vacismart.repository.UsuarioRepository usuarios;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder senhas;
    @Autowired br.com.fiap.vacismart.service.AutenticacaoService autenticacao;
    String token;
    @Autowired ObjectMapper json;
    @Autowired EventoAuditoriaRepository auditoria;
    @Autowired OcorrenciaRepository ocorrencias;
    @Autowired LeituraTemperaturaRepository leituras;
    @Autowired LoteRepository lotes;
    @Autowired TransporteRepository transportes;
    @Autowired VacinaRepository vacinas;
    @Autowired UnidadeSaudeRepository unidades;
    final HttpClient cliente = HttpClient.newHttpClient();
    static final String INSTANTE = "2026-11-08T15:00:00Z";

    @TestConfiguration
    static class Config {
        @Bean @Primary Clock clockHttp() { return Clock.fixed(Instant.parse(INSTANTE), ZoneOffset.UTC); }
        @Bean FalhaController falhaController() { return new FalhaController(); }
    }
    @RestController
    static class FalhaController {
        @GetMapping("/__teste/falha")
        String falhar() { throw new RuntimeException("SEGREDO_INTERNO_SQL"); }
    }
    @BeforeEach
    void limpar() {
        usuarios.deleteAllInBatch();
        auditoria.deleteAllInBatch(); ocorrencias.deleteAllInBatch(); leituras.deleteAllInBatch();
        lotes.deleteAllInBatch(); transportes.deleteAllInBatch(); vacinas.deleteAllInBatch(); unidades.deleteAllInBatch();
        usuarios.saveAndFlush(new br.com.fiap.vacismart.domain.entity.Usuario("Gestor fictício", "gestor@teste.invalid",
                senhas.encode("SenhaFicticia!123"), br.com.fiap.vacismart.domain.enums.Perfil.GESTOR, null));
        token = autenticacao.login(new br.com.fiap.vacismart.web.dto.LoginEntradaDto("gestor@teste.invalid", "SenhaFicticia!123")).token();
    }
    record Resposta(int status, JsonNode corpo, HttpHeaders headers) { }
    Resposta requisicao(String metodo, String rota, Object corpo) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + rota))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        String texto = corpo == null ? "" : corpo instanceof String s ? s : json.writeValueAsString(corpo);
        var resposta = cliente.send(builder.method(metodo, HttpRequest.BodyPublishers.ofString(texto)).build(), HttpResponse.BodyHandlers.ofString());
        return new Resposta(resposta.statusCode(), resposta.body().isEmpty() ? json.nullNode() : json.readTree(resposta.body()), resposta.headers());
    }
    JsonNode criar(String rota, Object dto) throws Exception {
        Resposta r = requisicao("POST", "/api/v1/" + rota, dto);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.headers().firstValue("Location")).isPresent();
        assertThat(requisicao("GET", r.headers().firstValue("Location").orElseThrow(), null).status()).isEqualTo(200);
        return r.corpo();
    }
    String id(JsonNode node) { return node.get("id").asString(); }
    Map<String,Object> faixa(String nome, String min, String max, String margem) {
        return Map.of("nome", nome, "temperaturaMinC", min, "temperaturaMaxC", max, "margemAtencaoC", margem);
    }
    record Cenario(String unidade, String vacina, String transporte, String lote) { }
    Cenario cenario(boolean iniciar) throws Exception {
        String u = id(criar("unidades", Map.of("nome", "Unidade fictícia")));
        String v = id(criar("vacinas", faixa("Demo A", "2", "8", "1")));
        String t = id(criar("transportes", Map.of("codigo", "TRP-001", "unidadeSaudeId", u)));
        String l = id(criar("lotes", Map.of("codigo", "VAC-2026-001", "vacinaId", v, "transporteId", t)));
        if (iniciar) assertThat(requisicao("POST", "/api/v1/transportes/" + t + "/monitoramento", null).status()).isEqualTo(204);
        return new Cenario(u,v,t,l);
    }
    Resposta leitura(Cenario c, String temperatura) throws Exception {
        return requisicao("POST", "/api/v1/transportes/" + c.transporte() + "/leituras",
                Map.of("transporteId", c.transporte(), "dispositivoId", "ARDUINO-DEMO-01", "temperaturaC", temperatura, "medidaEm", INSTANTE));
    }
    void erro(Resposta r, int codigo) {
        assertThat(r.status()).isEqualTo(codigo);
        assertThat(r.corpo().get("status").asInt()).isEqualTo(codigo);
        assertThat(r.corpo().get("detail").asString()).isNotBlank();
        assertThat(r.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(r.corpo().toString()).doesNotContain("stackTrace", "SEGREDO_INTERNO_SQL", "org.hibernate");
    }
    @Test
    void criaConsultaEAtualizaCadastrosSemExporEntidades() throws Exception {
        Cenario c = cenario(false);
        var u = requisicao("PUT", "/api/v1/unidades/" + c.unidade(), Map.of("nome", "Novo nome"));
        assertThat(u.status()).isEqualTo(200); assertThat(u.corpo().get("nome").asString()).isEqualTo("Novo nome");
        assertThat(requisicao("PUT", "/api/v1/vacinas/" + c.vacina(), faixa("Renomeada", "2.000", "8", "1")).status()).isEqualTo(200);
        var t = requisicao("PUT", "/api/v1/transportes/" + c.transporte(), Map.of("codigo", "TRP-002", "unidadeSaudeId", c.unidade()));
        assertThat(t.status()).isEqualTo(200); assertThat(t.corpo().has("unidadeSaude")).isFalse();
        var l = requisicao("PUT", "/api/v1/lotes/" + c.lote(), Map.of("codigo", "VAC-002"));
        assertThat(l.status()).isEqualTo(200); assertThat(l.corpo().get("estado").asString()).isEqualTo("EM_MONITORAMENTO");
        assertThat(l.corpo().get("disponibilidade").asString()).isEqualTo("NAO_DISPONIVEL");
    }
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nome\":\" \"}", "{", "{\"nome\":\"Teste\",\"estado\":\"LIBERADO\"}"})
    void rejeitaEntradaInvalidaOuCampoNaoContratado(String body) throws Exception {
        erro(requisicao("POST", "/api/v1/unidades", body), 400);
        assertThat(unidades.count()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings = {"unidades", "vacinas", "transportes", "lotes"})
    void ausenteRetorna404(String recurso) throws Exception {
        erro(requisicao("GET", "/api/v1/" + recurso + "/" + UUID.randomUUID(), null),404);
    }
    @Test
    void rejeitaUuidEnumJsonDataESortInvalidos() throws Exception {
        erro(requisicao("GET", "/api/v1/lotes/nao-uuid", null),400);
        erro(requisicao("GET", "/api/v1/lotes?estado=INVALIDO", null),400);
        erro(requisicao("GET", "/api/v1/lotes?sort=vacina.nome,asc", null),400);
        Cenario c = cenario(true);
        for (String data : List.of("2026-11-08T15:00:00", "2026-11-08T16:00:00Z", "2026-11-07T15:00:00Z")) {
            erro(requisicao("POST", "/api/v1/transportes/" + c.transporte() + "/leituras", Map.of("transporteId", c.transporte(), "dispositivoId", "DEMO", "temperaturaC", 5.2, "medidaEm", data)),400);
        }
        erro(requisicao("POST", "/api/v1/transportes/" + c.transporte() + "/leituras", Map.of("transporteId", UUID.randomUUID(), "dispositivoId", "DEMO", "temperaturaC", 5.2, "medidaEm", INSTANTE)),400);
        assertThat(leituras.count()).isZero();
    }
    @ParameterizedTest
    @CsvSource({"8,2,1", "2,2,0", "2,8,-1", "2,8,3.001", "2.0001,8,1", "2,10000000,1"})
    void rejeitaFaixasInvalidas(String min,String max,String margem) throws Exception {
        erro(requisicao("POST", "/api/v1/vacinas", faixa("Inválida",min,max,margem)),400);
        assertThat(vacinas.count()).isZero();
    }
    @Test
    void alteraFaixaSomenteSemLotesEPreservaFaixaAposRejeicao() throws Exception {
        String v = id(criar("vacinas", faixa("Sem lote","2","8","1")));
        assertThat(requisicao("PUT", "/api/v1/vacinas/"+v, faixa("Alterada","3","9","1")).status()).isEqualTo(200);
        erro(requisicao("PUT", "/api/v1/vacinas/"+v, faixa("Inválida","10","9","1")),400);
        Cenario c = cenario(false);
        erro(requisicao("PUT", "/api/v1/vacinas/"+c.vacina(), faixa("Com lote","3","9","1")),409);
        assertThat(requisicao("GET", "/api/v1/vacinas/"+c.vacina(),null).corpo().get("temperaturaMinC").decimalValue()).isEqualByComparingTo("2");
    }
    @Test
    void paginaOrdenaELimitaTamanho() throws Exception {
        for (String nome : List.of("Zeta", "Alfa", "Beta")) criar("unidades", Map.of("nome",nome));
        var p = requisicao("GET", "/api/v1/unidades?page=1&size=1&sort=nome,asc",null).corpo();
        assertThat(p.get("pagina").asInt()).isEqualTo(1); assertThat(p.get("totalElementos").asInt()).isEqualTo(3);
        assertThat(p.get("conteudo").get(0).get("nome").asString()).isEqualTo("Beta");
        assertThat(requisicao("GET", "/api/v1/unidades?size=10000",null).corpo().get("tamanho").asInt()).isEqualTo(100);
    }
    @Test
    void specificationCombinaFiltrosDeLotesETransportes() throws Exception {
        Cenario c = cenario(false);
        String u = id(criar("unidades",Map.of("nome","Outra")));
        String v = id(criar("vacinas",faixa("Outra","-20","-10","1")));
        String t = id(criar("transportes",Map.of("codigo","TRP-OUTRO","unidadeSaudeId",u)));
        criar("lotes",Map.of("codigo","OUTRO","transporteId",t,"vacinaId",v));
        var p = requisicao("GET", "/api/v1/lotes?transporteId="+c.transporte()+"&vacinaId="+c.vacina()+"&estado=EM_MONITORAMENTO",null).corpo();
        assertThat(p.get("totalElementos").asInt()).isEqualTo(1);
        assertThat(id(p.get("conteudo").get(0))).isEqualTo(c.lote());
        assertThat(requisicao("GET", "/api/v1/lotes?transporteId="+t+"&vacinaId="+c.vacina(),null).corpo().get("totalElementos").asInt()).isZero();
        assertThat(requisicao("GET", "/api/v1/transportes?unidadeId="+c.unidade()+"&estado=CADASTRADO",null).corpo().get("totalElementos").asInt()).isEqualTo(1);
        assertThat(requisicao("GET", "/api/v1/transportes?unidadeId="+u+"&estado=EM_TRANSPORTE",null).corpo().get("totalElementos").asInt()).isZero();
    }
    @ParameterizedTest
    @CsvSource({"5.2,NORMAL,EM_MONITORAMENTO", "5.2000,NORMAL,EM_MONITORAMENTO", "7.5,ATENCAO,EM_MONITORAMENTO", "9.3,FORA_DA_FAIXA,BLOQUEADO_PREVENTIVAMENTE"})
    void fluxoHttpClassificaBloqueiaEAudita(String temperatura,String classificacao,String estado) throws Exception {
        Cenario c = cenario(true);
        Resposta r = leitura(c,temperatura); assertThat(r.status()).isEqualTo(201);
        assertThat(r.corpo().get("classificacao").asString()).isEqualTo(classificacao);
        assertThat(requisicao("GET",r.headers().firstValue("Location").orElseThrow(),null).status()).isEqualTo(200);
        assertThat(leitura(c,"5.2").status()).isEqualTo(201);
        var l = requisicao("GET","/api/v1/lotes/"+c.lote(),null).corpo();
        assertThat(l.get("estado").asString()).isEqualTo(estado);
        assertThat(l.get("disponibilidade").asString()).isEqualTo("NAO_DISPONIVEL");
        var historico = requisicao("GET","/api/v1/lotes/"+c.lote()+"/historico",null).corpo();
        assertThat(historico.get("totalElementos").asInt()).isEqualTo(temperatura.equals("9.3") ? 1 : 0);
        assertThat(requisicao("GET","/api/v1/transportes/"+c.transporte()+"/historico",null).corpo().get("totalElementos").asInt()).isEqualTo(1);
        var lista = requisicao("GET","/api/v1/transportes/"+c.transporte()+"/leituras?size=1&sort=recebidaEm,desc",null).corpo();
        assertThat(lista.get("totalElementos").asInt()).isEqualTo(2);
        assertThat(lista.get("conteudo").size()).isEqualTo(1);
    }
    @Test
    void jpqlFiltraOcorrenciasPorTransporteEEstado() throws Exception {
        Cenario c = cenario(true); leitura(c,"9.3"); leitura(c,"9.3");
        String raiz = "/api/v1/transportes/"+c.transporte()+"/ocorrencias";
        var p = requisicao("GET",raiz+"?estado=ABERTA&sort=abertaEm,desc",null).corpo();
        assertThat(p.get("totalElementos").asInt()).isEqualTo(1);
        assertThat(p.get("conteudo").get(0).get("tipo").asString()).isEqualTo("FORA_DA_FAIXA");
        assertThat(requisicao("GET",raiz+"?estado=ENCERRADA",null).corpo().get("totalElementos").asInt()).isZero();
        String t = id(criar("transportes",Map.of("codigo","SEM-OCORRENCIA","unidadeSaudeId",c.unidade())));
        assertThat(requisicao("GET","/api/v1/transportes/"+t+"/ocorrencias",null).corpo().get("totalElementos").asInt()).isZero();
    }
    @Test
    void impedeAlteracoesDepoisDoInicioEBloqueio() throws Exception {
        Cenario c = cenario(true); leitura(c,"9.3");
        erro(requisicao("PUT","/api/v1/lotes/"+c.lote(),Map.of("codigo","ALTERADO")),409);
        erro(requisicao("PUT","/api/v1/transportes/"+c.transporte(),Map.of("codigo","ALTERADO","unidadeSaudeId",c.unidade())),409);
        erro(requisicao("POST","/api/v1/lotes",Map.of("codigo","NOVO","vacinaId",c.vacina(),"transporteId",c.transporte())),409);
        erro(requisicao("POST","/api/v1/transportes/"+c.transporte()+"/monitoramento",null),409);
        erro(requisicao("PUT","/api/v1/lotes/"+c.lote(),Map.of("codigo","ALTERADO","estado","LIBERADO","disponibilidade","CONFIRMADA")),400);
        assertThat(requisicao("GET","/api/v1/lotes/"+c.lote(),null).corpo().get("codigo").asString()).isEqualTo("VAC-2026-001");
    }
    @Test
    void protegeHistoricoEPermiteExcluirSomenteSemVinculos() throws Exception {
        Cenario c = cenario(false);
        for (String rota : List.of("unidades/"+c.unidade(),"vacinas/"+c.vacina(),"transportes/"+c.transporte())) erro(requisicao("DELETE","/api/v1/"+rota,null),409);
        String u = id(criar("unidades",Map.of("nome","Sem vínculo")));
        String v = id(criar("vacinas",faixa("Sem vínculo","2","8","1")));
        String t = id(criar("transportes",Map.of("codigo","SEM-VINCULO","unidadeSaudeId",u)));
        for (String rota : List.of("transportes/"+t,"vacinas/"+v,"unidades/"+u)) {
            assertThat(requisicao("DELETE","/api/v1/"+rota,null).status()).isEqualTo(204);
            erro(requisicao("GET","/api/v1/"+rota,null),404);
        }
    }
    @Test
    void duplicidadeConhecidaRetorna409SemExporSql() throws Exception {
        Cenario c = cenario(false);
        erro(requisicao("POST","/api/v1/transportes",Map.of("codigo","TRP-001","unidadeSaudeId",c.unidade())),409);
        assertThat(transportes.count()).isEqualTo(1);
    }
    @Test
    void conflitosDeInicioEDadosDeLeituraInvalidosNaoPersistem() throws Exception {
        Cenario c = cenario(false);
        erro(leitura(c,"5.2"),409);
        String t = id(criar("transportes",Map.of("codigo","VAZIO","unidadeSaudeId",c.unidade())));
        erro(requisicao("POST","/api/v1/transportes/"+t+"/monitoramento",null),409);
        erro(requisicao("POST","/api/v1/transportes/"+UUID.randomUUID()+"/monitoramento",null),404);
        assertThat(requisicao("POST","/api/v1/transportes/"+c.transporte()+"/monitoramento",null).status()).isEqualTo(204);
        erro(leitura(c,"8.0001"),400);
        assertThat(leituras.count()).isZero(); assertThat(ocorrencias.count()).isZero();
    }
    @Test
    void erro500SeguroERotasForaDoEscopoAusentes() throws Exception {
        erro(requisicao("GET","/__teste/falha",null),500);
        erro(requisicao("POST","/api/v1/lotes/"+UUID.randomUUID()+"/decisao-tecnica",Map.of()),404);
        erro(requisicao("POST","/api/v1/aplicacoes",Map.of()),404);
        erro(requisicao("GET","/api/v1/unidades/"+UUID.randomUUID()+"/monitoramento",null),404);
    }
    @AfterEach
    void fecharCliente() { cliente.close(); }

    @Test
    void transporteMistoBloqueiaSomenteVacinaAfetada() throws Exception {
        Cenario c = cenario(false);
        String v = id(criar("vacinas",faixa("Outra faixa","8","12","1")));
        String l = id(criar("lotes",Map.of("codigo","LOTE-MISTO","vacinaId",v,"transporteId",c.transporte())));
        assertThat(requisicao("POST","/api/v1/transportes/"+c.transporte()+"/monitoramento",null).status()).isEqualTo(204);
        assertThat(leitura(c,"9.3").status()).isEqualTo(201);
        assertThat(requisicao("GET","/api/v1/lotes/"+l,null).corpo().get("estado").asString()).isEqualTo("EM_MONITORAMENTO");
        assertThat(requisicao("GET","/api/v1/lotes/"+c.lote(),null).corpo().get("estado").asString()).isEqualTo("BLOQUEADO_PREVENTIVAMENTE");
    }
    @Test
    void getNaoIniciaMonitoramentoEAuditoriaNaoAceitaEscrita() throws Exception {
        Cenario c = cenario(false);
        erro(requisicao("GET","/api/v1/transportes/"+c.transporte()+"/monitoramento",null),405);
        assertThat(requisicao("GET","/api/v1/transportes/"+c.transporte(),null).corpo().get("estado").asString()).isEqualTo("CADASTRADO");
        erro(requisicao("POST","/api/v1/lotes/"+c.lote()+"/historico",Map.of()),405);
        erro(requisicao("DELETE","/api/v1/lotes/"+c.lote()+"/historico",null),405);
        assertThat(auditoria.count()).isZero();
    }
    @Test
    void referenciasInexistentesEConsultasFilhasAusentesRetornam404() throws Exception {
        erro(requisicao("POST","/api/v1/transportes",Map.of("codigo","FANTASMA","unidadeSaudeId",UUID.randomUUID())),404);
        Cenario c = cenario(false);
        erro(requisicao("POST","/api/v1/lotes",Map.of("codigo","FANTASMA","vacinaId",UUID.randomUUID(),"transporteId",c.transporte())),404);
        for (String rota : List.of("leituras","ocorrencias","historico")) {
            erro(requisicao("GET","/api/v1/transportes/"+UUID.randomUUID()+"/"+rota,null),404);
        }
        erro(requisicao("GET","/api/v1/lotes/"+UUID.randomUUID()+"/historico",null),404);
        assertThat(requisicao("POST","/api/v1/transportes/"+c.transporte()+"/monitoramento",null).status()).isEqualTo(204);
        var leitura = leitura(c,"5.2");
        String t = id(criar("transportes",Map.of("codigo","OUTRO","unidadeSaudeId",c.unidade())));
        erro(requisicao("GET","/api/v1/transportes/"+t+"/leituras/"+id(leitura.corpo()),null),404);
    }
}
