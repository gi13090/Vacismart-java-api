package br.com.fiap.vacismart.web;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.repository.*;
import br.com.fiap.vacismart.security.ChavesTesteConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;

// Requisições HTTP reais, sem transação de teste: a consulta posterior observa o commit do servidor.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:vacismart-aplicacao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"})
@ActiveProfiles("test")
@Import({ChavesTesteConfig.class, AplicacaoIntegrationTest.Config.class})
class AplicacaoIntegrationTest {
    static final Instant AGORA = Instant.parse("2026-11-16T03:00:00Z");
    @TestConfiguration
    static class Config {
        @Bean @Primary Clock clockAplicacao() { return Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")); }
    }
    @LocalServerPort int porta;
    @Autowired ObjectMapper json;
    @Autowired JwtEncoder encoder;
    @Autowired UsuarioRepository usuarios;
    @Autowired UnidadeSaudeRepository unidades;
    @Autowired VacinaRepository vacinas;
    @Autowired TransporteRepository transportes;
    @Autowired LoteRepository lotes;
    @Autowired EventoAuditoriaRepository auditoria;
    @Autowired PlatformTransactionManager txManager;
    @Autowired br.com.fiap.vacismart.service.FluxoOperacionalService fluxo;
    HttpClient cliente;
    UnidadeSaude unidade, outra;
    Transporte transporte;
    Lote lote;
    Usuario funcionario, gestor, cidadao, externo;

    @BeforeEach void preparar() {
        cliente = HttpClient.newHttpClient();
        // Banco exclusivo desta classe; não remove evidências de execuções anteriores.
        usuarios.deleteAllInBatch(); auditoria.deleteAllInBatch(); lotes.deleteAllInBatch();
        transportes.deleteAllInBatch(); vacinas.deleteAllInBatch(); unidades.deleteAllInBatch();
        unidade = unidades.saveAndFlush(new UnidadeSaude("Unidade fictícia A"));
        outra = unidades.saveAndFlush(new UnidadeSaude("Unidade fictícia B"));
        var vacina = vacinas.saveAndFlush(new Vacina("Demo", new BigDecimal("2"), new BigDecimal("8"), BigDecimal.ONE));
        transporte = transportes.saveAndFlush(new Transporte("TR-J6", unidade));
        lote = lotes.saveAndFlush(new Lote("LOTE-J6", vacina, transporte));
        funcionario = usuario("funcionario", Perfil.FUNCIONARIO, unidade);
        externo = usuario("externo", Perfil.FUNCIONARIO, outra);
        gestor = usuario("gestor", Perfil.GESTOR, null);
        cidadao = usuario("cidadao", Perfil.CIDADAO, null);
    }
    @AfterEach void fechar() { cliente.close(); }
    Usuario usuario(String nome, Perfil perfil, UnidadeSaude vinculo) {
        // Sem login neste teste: o token é assinado com o par efêmero do perfil test.
        return usuarios.saveAndFlush(new Usuario(nome, nome + "@teste.invalid", "hash-ficticio", perfil, vinculo));
    }
    String token(Usuario usuario) {
        var claims = JwtClaimsSet.builder().subject(usuario.getId().toString()).issuer("vacismart")
                .issuedAt(AGORA).expiresAt(AGORA.plusSeconds(900)).claim("perfil", usuario.getPerfil().name()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
    HttpResponse<String> post(String corpo, String token) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api/v1/aplicacoes"))
                .header("Content-Type", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        return cliente.send(req.POST(HttpRequest.BodyPublishers.ofString(corpo)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String entrada(UUID id) { return "{\"loteId\":\"" + id + "\"}"; }
    JsonNode problema(HttpResponse<String> res, int status) throws Exception {
        assertThat(res.statusCode()).isEqualTo(status);
        assertThat(res.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        var body = json.readTree(res.body());
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("detail").asString()).isNotBlank();
        assertThat(res.body()).doesNotContain("org.hibernate", "stackTrace", "hash-ficticio");
        return body;
    }
    void estado(EstadoLote destino, DisponibilidadeLote disponibilidade) {
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            var l = lotes.findById(lote.getId()).orElseThrow();
            var t = transportes.findById(transporte.getId()).orElseThrow();
            if (destino != EstadoLote.EM_MONITORAMENTO) {
                t.iniciarMonitoramento(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC));
                t.receber();
                if (destino == EstadoLote.BLOQUEADO_PREVENTIVAMENTE) l.bloquearPreventivamente();
                else {
                    l.encaminharParaConferencia();
                    if (destino == EstadoLote.LIBERADO || destino == EstadoLote.DESCARTADO)
                        l.decidirTecnicamente(destino == EstadoLote.LIBERADO ? DecisaoTecnica.LIBERAR : DecisaoTecnica.DESCARTAR, "Fixture fictícia");
                }
            }
            if (disponibilidade == DisponibilidadeLote.CONFIRMADA) l.alterarDisponibilidade(disponibilidade);
        });
    }
    void preservado(EstadoLote estado, DisponibilidadeLote disponibilidade, EstadoTransporte estadoTransporte) {
        var salvo = lotes.findById(lote.getId()).orElseThrow();
        assertThat(salvo.getEstado()).isEqualTo(estado);
        assertThat(salvo.getDisponibilidade()).isEqualTo(disponibilidade);
        assertThat(salvo.getCodigo()).isEqualTo("LOTE-J6");
        assertThat(transportes.findById(transporte.getId()).orElseThrow().getEstado()).isEqualTo(estadoTransporte);
        assertThat(lotes.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoLote.class, names = "LIBERADO", mode = EnumSource.Mode.EXCLUDE)
    void naoLiberadoRetorna409EAuditoriaSobreviveEmOutraTransacao(EstadoLote estado) throws Exception {
        estado(estado, DisponibilidadeLote.NAO_DISPONIVEL);
        var anterior = auditoria.saveAndFlush(new EventoAuditoria("Lote", lote.getId(), "EVIDENCIA_ANTERIOR", "SISTEMA",
                OffsetDateTime.ofInstant(AGORA.minusSeconds(60), ZoneOffset.UTC), "Registro anterior preservado"));
        var res = post(entrada(lote.getId()), token(funcionario));
        problema(res, 409);
        assertThat(res.body()).contains("não LIBERADO");
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            var eventos = auditoria.findByReferenciaIdOrderByDataHoraAsc(lote.getId());
            assertThat(eventos).hasSize(2);
            assertThat(eventos.getFirst().getId()).isEqualTo(anterior.getId());
            assertThat(eventos.getFirst().getJustificativa()).isEqualTo("Registro anterior preservado");
            var evento = eventos.getLast();
            assertThat(evento.getEntidade()).isEqualTo("Lote");
            assertThat(evento.getReferenciaId()).isEqualTo(lote.getId());
            assertThat(evento.getAcao()).isEqualTo("APLICACAO_REJEITADA");
            assertThat(evento.getAutor()).isEqualTo(funcionario.getId().toString());
            assertThat(evento.getDataHora().toInstant()).isEqualTo(AGORA);
            assertThat(evento.getJustificativa()).contains(estado.name(), "exige LIBERADO");
        });
        preservado(estado, DisponibilidadeLote.NAO_DISPONIVEL,
                estado == EstadoLote.EM_MONITORAMENTO ? EstadoTransporte.CADASTRADO : EstadoTransporte.RECEBIDO);
        // Cada nova tentativa rejeitada produz seu próprio evento, sem substituir os anteriores.
        problema(post(entrada(lote.getId()), token(funcionario)), 409);
        assertThat(auditoria.count()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"FUNCIONARIO,CONFIRMADA", "FUNCIONARIO,NAO_DISPONIVEL", "GESTOR,CONFIRMADA", "GESTOR,NAO_DISPONIVEL"})
    void liberadoPermiteSomenteSimulacaoSemMudarDisponibilidade(Perfil perfil, DisponibilidadeLote disponibilidade) throws Exception {
        estado(EstadoLote.LIBERADO, disponibilidade);
        var res = post(entrada(lote.getId()), token(perfil == Perfil.GESTOR ? gestor : funcionario));
        assertThat(res.statusCode()).isEqualTo(200);
        var body = json.readTree(res.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("resultado", "mensagem");
        assertThat(body.get("resultado").asString()).isEqualTo("PERMITIDA");
        assertThat(body.get("mensagem").asString()).contains("demonstrativa", "Não autoriza nem registra administração real");
        preservado(EstadoLote.LIBERADO, disponibilidade, EstadoTransporte.RECEBIDO);
        assertThat(auditoria.count()).isZero();
    }

    @Test void gestorPodeVerificarOutraUnidadeEAuditaSuaIdentidade() throws Exception {
        problema(post(entrada(lote.getId()), token(gestor)), 409);
        assertThat(auditoria.findAll()).singleElement().satisfies(e -> assertThat(e.getAutor()).isEqualTo(gestor.getId().toString()));
    }
    @Test void recarregaLotePreviamenteCarregadoAposDecisaoEmOutraTransacao() throws Exception {
        estado(EstadoLote.BLOQUEADO_PREVENTIVAMENTE, DisponibilidadeLote.NAO_DISPONIVEL);
        var carregado = new CountDownLatch(1);
        var decidido = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var verificacao = executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        funcionario.getId().toString(), null, List.of()));
                try {
                    return new TransactionTemplate(txManager).execute(tx -> {
                        assertThat(lotes.findById(lote.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.BLOQUEADO_PREVENTIVAMENTE);
                        carregado.countDown();
                        aguardar(decidido);
                        return fluxo.verificarAplicacao(lote.getId());
                    });
                } finally { SecurityContextHolder.clearContext(); }
            });
            try {
                assertThat(carregado.await(10, TimeUnit.SECONDS)).isTrue();
                new TransactionTemplate(txManager).executeWithoutResult(tx -> {
                    transportes.buscarParaAtualizacao(transporte.getId()).orElseThrow();
                    lotes.findById(lote.getId()).orElseThrow().decidirTecnicamente(DecisaoTecnica.LIBERAR, "Conferência fictícia concorrente");
                });
            } finally { decidido.countDown(); }
            assertThat(verificacao.get(10, TimeUnit.SECONDS)).isTrue();
        }
        preservado(EstadoLote.LIBERADO, DisponibilidadeLote.NAO_DISPONIVEL, EstadoTransporte.RECEBIDO);
        assertThat(auditoria.count()).isZero();
    }
    static void aguardar(CountDownLatch latch) {
        try { assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
    }
    @Test void cidadaoNaoPodeVerificarNemGerarAuditoria() throws Exception {
        problema(post(entrada(lote.getId()), token(cidadao)), 403);
        assertThat(auditoria.count()).isZero();
    }
    @Test void outraUnidadeEAusenteTemMesmaNegacaoSemAuditoria() throws Exception {
        String token = token(externo);
        var alheio = problema(post(entrada(lote.getId()), token), 403);
        var ausente = problema(post(entrada(UUID.randomUUID()), token), 403);
        assertThat(alheio).isEqualTo(ausente);
        assertThat(auditoria.count()).isZero();
        preservado(EstadoLote.EM_MONITORAMENTO, DisponibilidadeLote.NAO_DISPONIVEL, EstadoTransporte.CADASTRADO);
    }
    @Test void gestorRecebe404ParaLoteAusenteSemAuditoria() throws Exception {
        problema(post(entrada(UUID.randomUUID()), token(gestor)), 404);
        assertThat(auditoria.count()).isZero();
    }
    @ParameterizedTest @ValueSource(strings = {"ausente", "invalido", "inativo"})
    void exigeAutenticacaoValidaEUsuarioAtivo(String caso) throws Exception {
        String token = caso.equals("ausente") ? null : caso.equals("invalido") ? "invalido" : token(funcionario);
        if (caso.equals("inativo")) new TransactionTemplate(txManager).executeWithoutResult(tx ->
                usuarios.findById(funcionario.getId()).orElseThrow().desativar());
        var res = post(entrada(lote.getId()), token);
        problema(res, 401);
        assertThat(res.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(auditoria.count()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{", "[]", "{\"loteId\":null}", "{\"loteId\":\"\"}",
            "{\"loteId\":\"nao-uuid\"}", "{\"loteId\":123}"})
    void corpoEUuidInvalidosNaoGeramAuditoria(String corpo) throws Exception {
        problema(post(corpo, token(funcionario)), 400);
        assertThat(auditoria.count()).isZero();
    }
    @ParameterizedTest @ValueSource(strings = {"autor", "unidadeId", "perfil", "dataHora", "estado"})
    void rejeitaCamposNaoContratados(String campo) throws Exception {
        String corpo = json.writeValueAsString(Map.of("loteId", lote.getId(), campo, "arbitrario"));
        problema(post(corpo, token(funcionario)), 400);
        assertThat(auditoria.count()).isZero();
    }
}
