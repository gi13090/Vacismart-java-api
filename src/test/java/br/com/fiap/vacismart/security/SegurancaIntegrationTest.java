package br.com.fiap.vacismart.security;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.repository.*;
import br.com.fiap.vacismart.service.CadastroService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:vacismart-seguranca;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "vacismart.cors.origens=http://localhost:5173"})
@ActiveProfiles("test")
@Import(ChavesTesteConfig.class)
class SegurancaIntegrationTest {
    @LocalServerPort int porta;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired UnidadeSaudeRepository unidades;
    @Autowired TransporteRepository transportes;
    @Autowired LoteRepository lotes;
    @Autowired VacinaRepository vacinas;
    @Autowired LeituraTemperaturaRepository leituras;
    @Autowired OcorrenciaRepository ocorrencias;
    @Autowired EventoAuditoriaRepository auditoria;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired CadastroService cadastro;
    @Autowired br.com.fiap.vacismart.service.FluxoOperacionalService fluxo;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager txManager;
    static final String SENHA = "FicticiaSomenteTeste!123";
    static String hash;
    HttpClient cliente;
    UnidadeSaude a, b;
    Transporte ta, tb;
    Lote la, lb;
    Vacina vacina;
    Usuario funcionario, cidadao, gestor, outro;

    @BeforeEach void preparar() {
        cliente = HttpClient.newHttpClient();
        usuarios.deleteAllInBatch(); auditoria.deleteAllInBatch(); ocorrencias.deleteAllInBatch(); leituras.deleteAllInBatch();
        lotes.deleteAllInBatch(); transportes.deleteAllInBatch(); vacinas.deleteAllInBatch(); unidades.deleteAllInBatch();
        if (hash == null) hash = encoder.encode(SENHA);
        a = unidades.save(new UnidadeSaude("Unidade A")); b = unidades.save(new UnidadeSaude("Unidade B"));
        vacina = vacinas.save(new Vacina("Demo", new BigDecimal("2"), new BigDecimal("8"), BigDecimal.ONE));
        ta = transportes.save(new Transporte("TR-A", a)); tb = transportes.save(new Transporte("TR-B", b));
        la = lotes.save(new Lote("LA", vacina, ta)); lb = lotes.save(new Lote("LB", vacina, tb));
        cidadao = usuario("cidadao", Perfil.CIDADAO, null); gestor = usuario("gestor", Perfil.GESTOR, null);
        funcionario = usuario("funcionario", Perfil.FUNCIONARIO, a); outro = usuario("outro", Perfil.FUNCIONARIO, b);
    }
    @AfterEach void fechar() { cliente.close(); SecurityContextHolder.clearContext(); }
    Usuario usuario(String email, Perfil perfil, UnidadeSaude unidade) {
        return usuarios.saveAndFlush(new Usuario("Usuário fictício", email + "@teste.invalid", hash, perfil, unidade));
    }
    record Resposta(int status, JsonNode corpo, HttpHeaders headers) { }
    Resposta req(String metodo, String rota, Object dados, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + rota)).header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        String body = dados == null ? "" : json.writeValueAsString(dados);
        var res = cliente.send(builder.method(metodo, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        return new Resposta(res.statusCode(), res.body().isEmpty() ? json.nullNode() : json.readTree(res.body()), res.headers());
    }
    Resposta login(String email, String senha) throws Exception { return req("POST", "/api/v1/auth/login", Map.of("email",email,"senha",senha),null); }
    String token(Usuario u) throws Exception {
        var resposta = login(u.getEmail(), SENHA); assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().toString()).doesNotContain(hash, "senha", "senhaHash");
        return resposta.corpo().get("token").asString();
    }
    void status(Resposta res, int esperado) {
        assertThat(res.status()).isEqualTo(esperado);
        if (esperado >= 400) {
            assertThat(res.corpo().get("status").asInt()).isEqualTo(esperado);
            assertThat(res.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
            assertThat(res.corpo().toString()).doesNotContain(hash, SENHA, "org.hibernate", "stackTrace");
        }
        if (esperado == 401) assertThat(res.headers().firstValue("WWW-Authenticate")).contains("Bearer");
    }
    @Test void cadastroPublicoCriaSomenteCidadaoComHashENormalizaEmail() throws Exception {
        var res = req("POST", "/api/v1/auth/cadastro", Map.of("nome","Cidadão", "email","NOVO@teste.invalid", "senha",SENHA),null);
        status(res,201); assertThat(res.corpo().get("perfil").asString()).isEqualTo("CIDADAO");
        assertThat(res.corpo().toString()).doesNotContain("senha", SENHA);
        Usuario salvo = usuarios.findByEmail("novo@teste.invalid").orElseThrow();
        assertThat(salvo.conferirSenha(encoder,SENHA)).isTrue();
        String persistido = jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, salvo.getId());
        assertThat(persistido).isNotEqualTo(SENHA).startsWith("$2");
        assertThat(encoder.matches(SENHA,persistido)).isTrue();
    }
    @Test void cadastroDuplicadoRetorna409() throws Exception {
        status(req("POST","/api/v1/auth/cadastro",Map.of("nome","Outro","email","CIDADAO@teste.invalid","senha",SENHA),null),409);
        assertThat(usuarios.count()).isEqualTo(4);
    }
    @ParameterizedTest @ValueSource(strings={"GESTOR","FUNCIONARIO","CIDADAO"})
    void cadastroRejeitaCampoPerfil(String perfil) throws Exception {
        status(req("POST","/api/v1/auth/cadastro",Map.of("nome","Novo","email","novo@teste.invalid","senha",SENHA,"perfil",perfil),null),400);
        assertThat(usuarios.count()).isEqualTo(4);
    }
    @ParameterizedTest @ValueSource(strings={"nome","email","senha","unidadeSaudeId","ativo"})
    void cadastroValidaDtoERejeitaVinculos(String campo) throws Exception {
        Map<String,Object> dados = new HashMap<>(Map.of("nome","Novo","email","novo@teste.invalid","senha",SENHA));
        dados.put(campo, campo.equals("email") ? "invalido" : campo.equals("senha") ? "curta" : campo.equals("nome") ? " " : "arbitrario");
        status(req("POST","/api/v1/auth/cadastro",dados,null),400);
    }
    @Test void loginValidoEClaimsAssinadas() throws Exception {
        String token = token(funcionario); Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(funcionario.getId().toString());
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("FUNCIONARIO");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("vacismart");
        assertThat(Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        status(req("GET","/api/v1/auth/me",null,token),200);
    }
    @ParameterizedTest @ValueSource(strings={"incorreta","inexistente"})
    void loginRejeitaCredenciais(String caso) throws Exception {
        status(login(caso.equals("inexistente") ? "ausente@teste.invalid" : cidadao.getEmail(),"SenhaIncorreta!123"),401);
    }
    @Test void inativoNaoEntraETokenAnteriorPerdeAcesso() throws Exception {
        String token = token(funcionario);
        new TransactionTemplate(txManager).executeWithoutResult(tx -> usuarios.findById(funcionario.getId()).orElseThrow().desativar());
        status(login(funcionario.getEmail(),SENHA),401);
        status(req("GET","/api/v1/transportes/"+ta.getId(),null,token),401);
    }
    String assinado(String sujeito, String perfil, String emissor, Instant inicio, Instant fim) {
        var claims = JwtClaimsSet.builder().subject(sujeito).issuer(emissor).issuedAt(inicio).expiresAt(fim).claim("perfil",perfil).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),claims)).getTokenValue();
    }
    @ParameterizedTest @ValueSource(strings={"expirado","emissor","sujeito","futuro","adulterado","sem-assinatura","sem-expiracao","sem-emissao","sem-perfil","malformado","ausente","excluido"})
    void rejeitaTokensInvalidosCom401(String caso) throws Exception {
        Instant agora = clock.instant();
        String token = switch (caso) {
            case "expirado" -> assinado(funcionario.getId().toString(),"FUNCIONARIO","vacismart",agora.minusSeconds(100),agora.minusSeconds(1));
            case "emissor" -> assinado(funcionario.getId().toString(),"FUNCIONARIO","outro",agora,agora.plusSeconds(900));
            case "sujeito" -> assinado("invalido","FUNCIONARIO","vacismart",agora,agora.plusSeconds(900));
            case "futuro" -> assinado(funcionario.getId().toString(),"FUNCIONARIO","vacismart",agora.plusSeconds(60),agora.plusSeconds(900));
            case "excluido" -> assinado(UUID.randomUUID().toString(),"GESTOR","vacismart",agora,agora.plusSeconds(900));
            case "adulterado" -> { String valido = token(funcionario); var partes = valido.split("\\."); yield partes[0]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"falso\"}".getBytes())+"."+partes[2]; }
            case "sem-expiracao", "sem-emissao", "sem-perfil" -> {
                var claims = JwtClaimsSet.builder().subject(funcionario.getId().toString()).issuer("vacismart");
                if (!caso.equals("sem-expiracao")) claims.expiresAt(agora.plusSeconds(900));
                if (!caso.equals("sem-emissao")) claims.issuedAt(agora);
                if (!caso.equals("sem-perfil")) claims.claim("perfil","FUNCIONARIO");
                yield jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),claims.build())).getTokenValue();
            }
            case "sem-assinatura" -> "eyJhbGciOiJub25lIn0.eyJzdWIiOiJmYWxzbyJ9.";
            case "ausente" -> null;
            default -> "token.invalido";
        };
        status(req("GET","/api/v1/transportes",null,token),401);
    }
    @ParameterizedTest @CsvSource({
        "GET,/unidades", "GET,/unidades/ID", "GET,/vacinas", "GET,/vacinas/ID", "GET,/transportes", "GET,/lotes",
        "POST,/unidades", "PUT,/unidades/ID", "DELETE,/unidades/ID",
        "POST,/vacinas", "PUT,/vacinas/ID", "DELETE,/vacinas/ID",
        "POST,/transportes", "GET,/transportes/ID", "PUT,/transportes/ID", "DELETE,/transportes/ID",
        "POST,/transportes/ID/monitoramento", "POST,/transportes/ID/leituras", "GET,/transportes/ID/leituras",
        "GET,/transportes/ID/leituras/ID", "GET,/transportes/ID/ocorrencias", "GET,/transportes/ID/historico",
        "POST,/lotes", "GET,/lotes/ID", "PUT,/lotes/ID", "GET,/lotes/ID/historico"})
    void cidadaoBloqueadoEmTodaOperacaoInterna(String metodo,String rota) throws Exception {
        status(req(metodo,"/api/v1"+rota.replace("ID",ta.getId().toString()),Map.of(),token(cidadao)),403);
    }
    @Test void cidadaoConsultaSomentePropriaContaESemSessao() throws Exception {
        var res=req("GET","/api/v1/auth/me",null,token(cidadao));status(res,200);
        assertThat(res.corpo().get("id").asString()).isEqualTo(cidadao.getId().toString());
        assertThat(res.headers().firstValue("Set-Cookie")).isEmpty();
        status(req("GET","/api/v1/auth/me",null,null),401);
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/transportes"))
            .header("Authorization","Basic "+Base64.getEncoder().encodeToString((gestor.getEmail()+":"+SENHA).getBytes())).GET().build();
        assertThat(cliente.send(request,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }
    @Test void claimPrivilegiadaNaoSubstituiPerfilPersistido() throws Exception {
        String token = assinado(cidadao.getId().toString(),"GESTOR","vacismart",clock.instant(),clock.instant().plusSeconds(900));
        status(req("GET","/api/v1/transportes",null,token),403);
    }
    @Test void alteracaoDePerfilEVinculoRevogaAcessoComTokenAntigo() throws Exception {
        String token = token(funcionario);
        new TransactionTemplate(txManager).executeWithoutResult(tx -> usuarios.findById(funcionario.getId()).orElseThrow()
                .alterarVinculo(Perfil.FUNCIONARIO,unidades.findById(b.getId()).orElseThrow()));
        status(req("GET","/api/v1/transportes/"+ta.getId(),null,token),403);
        status(req("GET","/api/v1/transportes/"+tb.getId(),null,token),200);
        new TransactionTemplate(txManager).executeWithoutResult(tx -> usuarios.findById(funcionario.getId()).orElseThrow().alterarVinculo(Perfil.CIDADAO,null));
        status(req("GET","/api/v1/transportes",null,token),403);
    }
    @Test void paginaEContagemSaoRestritasNoBanco() throws Exception {
        String token = token(funcionario);
        for (String rota : List.of("/transportes?size=1&estado=CADASTRADO","/lotes?size=1&estado=EM_MONITORAMENTO&vacinaId="+vacina.getId(),"/unidades?size=1")) {
            var res=req("GET","/api/v1"+rota,null,token);status(res,200);
            assertThat(res.corpo().get("totalElementos").asInt()).isEqualTo(1);
            assertThat(res.corpo().get("conteudo").size()).isEqualTo(1);
        }
        status(req("GET","/api/v1/transportes?unidadeId="+b.getId(),null,token),403);
        status(req("GET","/api/v1/lotes?transporteId="+tb.getId(),null,token),403);
        assertThat(req("GET","/api/v1/transportes",null,token(gestor)).corpo().get("totalElementos").asInt()).isEqualTo(2);
    }
    @ParameterizedTest @CsvSource({"GET,transportes,T", "PUT,transportes,T", "DELETE,transportes,T", "POST,transportes,T/monitoramento",
        "POST,transportes,T/leituras", "GET,transportes,T/leituras", "GET,transportes,T/leituras/L",
        "GET,transportes,T/ocorrencias", "GET,transportes,T/historico", "GET,lotes,L", "PUT,lotes,L", "GET,lotes,L/historico"})
    void funcionarioNaoAcessaOutraUnidadeNemUuidsInexistentes(String metodo,String recurso,String sufixo) throws Exception {
        String token=token(funcionario);
        String rota="/api/v1/"+recurso+"/"+sufixo.replace("T",tb.getId().toString()).replace("L",lb.getId().toString());
        Object body=metodo.equals("PUT") ? recurso.equals("lotes") ? Map.of("codigo","ALTERADO") : Map.of("codigo","ALTERADO","unidadeSaudeId",b.getId())
            : metodo.equals("POST") && sufixo.contains("leituras") ? Map.of("transporteId",tb.getId(),"dispositivoId","DEMO","temperaturaC",5,"medidaEm",clock.instant().toString()) : null;
        status(req(metodo,rota,body,token),403);
        status(req("GET","/api/v1/transportes/"+UUID.randomUUID(),null,token),403);
        status(req("GET","/api/v1/transportes/"+UUID.randomUUID(),null,token(gestor)),404);
    }
    @Test void funcionarioNaoMoveTransporteParaOutraUnidade() throws Exception {
        status(req("PUT","/api/v1/transportes/"+ta.getId(),Map.of("codigo","NOVO","unidadeSaudeId",b.getId()),token(funcionario)),403);
        status(req("POST","/api/v1/transportes",Map.of("codigo","NOVO","unidadeSaudeId",b.getId()),token(funcionario)),403);
        status(req("POST","/api/v1/transportes",Map.of("codigo","NOVO","unidadeSaudeId",a.getId()),token(funcionario)),201);
        assertThat(transportes.findById(ta.getId()).orElseThrow().getCodigo()).isEqualTo("TR-A");
    }
    @Test void cadastroDeLoteEGlobaisExigemGestor() throws Exception {
        String token=token(funcionario);
        status(req("POST","/api/v1/unidades",Map.of("nome","Nova"),token),403);
        status(req("POST","/api/v1/vacinas",Map.of("nome","Nova","temperaturaMinC",2,"temperaturaMaxC",8,"margemAtencaoC",1),token),403);
        status(req("POST","/api/v1/lotes",Map.of("codigo","NOVO","vacinaId",vacina.getId(),"transporteId",ta.getId()),token),403);
        status(req("POST","/api/v1/lotes",Map.of("codigo","NOVO","vacinaId",vacina.getId(),"transporteId",ta.getId()),token(gestor)),201);
    }
    @Test void funcionarioExecutaFluxoTermicoDaPropriaUnidadeSemLiberarLotes() throws Exception {
        String token=token(funcionario);
        status(req("PUT","/api/v1/lotes/"+la.getId(),Map.of("codigo","LA-ATUALIZADO"),token),200);
        status(req("POST","/api/v1/transportes/"+ta.getId()+"/monitoramento",null,token),204);
        String leituraId=null;
        for (String temperatura : List.of("9.3","5.2")) {
            var leitura=req("POST","/api/v1/transportes/"+ta.getId()+"/leituras",Map.of("transporteId",ta.getId(),"dispositivoId","DEMO","temperaturaC",temperatura,"medidaEm",clock.instant().toString()),token);
            status(leitura,201); leituraId=leitura.corpo().get("id").asString();
        }
        for (String sufixo: List.of("leituras","leituras/"+leituraId,"ocorrencias","historico"))
            status(req("GET","/api/v1/transportes/"+ta.getId()+"/"+sufixo,null,token),200);
        status(req("GET","/api/v1/lotes/"+la.getId()+"/historico",null,token),200);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.BLOQUEADO_PREVENTIVAMENTE);
        assertThat(lotes.findById(lb.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.EM_MONITORAMENTO);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId())).hasSize(1);
        assertThat(ocorrencias.count()).isEqualTo(1);
        status(req("PUT","/api/v1/lotes/"+la.getId(),Map.of("codigo","PROIBIDO"),token(gestor)),409);
    }
    @Test void servicesNaoDependemDaProtecaoDasRotas() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(funcionario.getId().toString(),null,List.of()));
        assertThatThrownBy(() -> cadastro.transporte(tb.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> cadastro.lote(lb.getId())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void transferenciaConcorrenteRevalidaEntidadeJaCarregadaSobLock() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(funcionario.getId().toString(),null,List.of()));
        try (var executor=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> new TransactionTemplate(txManager).executeWithoutResult(tx -> {
                // Preenche o cache de primeiro nível com a unidade A, sem bloquear o transporte.
                assertThat(transportes.findById(ta.getId()).orElseThrow().getUnidadeSaude().getId()).isEqualTo(a.getId());
                var transferencia=executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(gestor.getId().toString(),null,List.of()));
                    try {
                        return cadastro.atualizarTransporte(ta.getId(),new br.com.fiap.vacismart.web.dto.TransporteEntradaDto("TRANSFERIDO",b.getId()));
                    } finally { SecurityContextHolder.clearContext(); }
                });
                try { transferencia.get(10,java.util.concurrent.TimeUnit.SECONDS); }
                catch(Exception ex) { throw new IllegalStateException("Falha ao preparar transferência concorrente",ex); }
                cadastro.atualizarTransporte(ta.getId(),new br.com.fiap.vacismart.web.dto.TransporteEntradaDto("INDEVIDO",a.getId()));
            })).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(transportes.findById(ta.getId()).orElseThrow().getCodigo()).isEqualTo("TRANSFERIDO");
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void recebimentoPorFuncionarioOuGestorAuditaSemDisponibilizar(boolean peloGestor) throws Exception {
        Usuario autor = peloGestor ? gestor : funcionario;
        String token = token(autor);
        status(req("POST","/api/v1/transportes/"+ta.getId()+"/monitoramento",null,token),204);
        Instant antes = clock.instant();
        var res = req("POST","/api/v1/transportes/"+ta.getId()+"/recebimento",null,token);
        status(res,204);
        assertThat(res.corpo().isNull()).isTrue();
        assertThat(transportes.findById(ta.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.RECEBIDO);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.AGUARDANDO_CONFERENCIA);
        assertThat(lotes.findById(la.getId()).orElseThrow().getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
        var eventos = auditoria.findByReferenciaIdOrderByDataHoraAsc(ta.getId()).stream()
                .filter(e -> e.getAcao().equals("RECEBIDO")).toList();
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.getAutor()).isEqualTo(autor.getId().toString());
            assertThat(e.getDataHora().toInstant()).isBetween(antes,clock.instant());
        });
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId())).singleElement().satisfies(e -> {
            assertThat(e.getAcao()).isEqualTo("AGUARDANDO_CONFERENCIA");
            assertThat(e.getAutor()).isEqualTo(autor.getId().toString());
            assertThat(e.getDataHora()).isEqualTo(eventos.getFirst().getDataHora());
        });
        long eventosAntes = auditoria.count();
        status(req("POST","/api/v1/transportes/"+ta.getId()+"/recebimento",null,token),409);
        status(req("POST","/api/v1/transportes/"+ta.getId()+"/leituras",Map.of("transporteId",ta.getId(),
                "dispositivoId","DEMO","temperaturaC",9.3,"medidaEm",clock.instant().toString()),token),409);
        assertThat(auditoria.count()).isEqualTo(eventosAntes);
        assertThat(leituras.count()).isZero();
    }
    @Test void recebimentoRespeitaAutenticacaoEscopoEEstado() throws Exception {
        String rota="/api/v1/transportes/"+ta.getId()+"/recebimento";
        status(req("POST",rota,null,null),401);
        status(req("POST",rota,null,"invalido"),401);
        status(req("POST",rota,null,token(cidadao)),403);
        status(req("POST",rota,null,token(outro)),403);
        status(req("POST",rota,null,token(funcionario)),409);
        String ausente="/api/v1/transportes/"+UUID.randomUUID()+"/recebimento";
        status(req("POST",ausente,null,token(funcionario)),403);
        status(req("POST",ausente,null,token(gestor)),404);
        assertThat(auditoria.count()).isZero();
        assertThat(transportes.findById(ta.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.CADASTRADO);
    }
    @Test void recebimentoPreservaBloqueioEOcorrenciaTermica() throws Exception {
        String token=token(funcionario);
        String base="/api/v1/transportes/"+ta.getId();
        status(req("POST",base+"/monitoramento",null,token),204);
        status(req("POST",base+"/leituras",Map.of("transporteId",ta.getId(),"dispositivoId","DEMO",
                "temperaturaC",9.3,"medidaEm",clock.instant().toString()),token),201);
        status(req("POST",base+"/recebimento",null,token),204);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.BLOQUEADO_PREVENTIVAMENTE);
        assertThat(lotes.findById(la.getId()).orElseThrow().getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId())).singleElement()
                .extracting(EventoAuditoria::getAcao).isEqualTo("BLOQUEADO_PREVENTIVAMENTE");
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(ta.getId(),TipoOcorrencia.FORA_DA_FAIXA,
                EstadoOcorrencia.ABERTA)).isPresent();
    }
    @Test void recebimentosConcorrentesPersistemSomenteUmaTransicao() throws Exception {
        String token=token(funcionario);
        String base="/api/v1/transportes/"+ta.getId();
        status(req("POST",base+"/monitoramento",null,token),204);
        var largada=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> tarefa=() -> {
                if(!largada.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return req("POST",base+"/recebimento",null,token).status();
            };
            var um=executor.submit(tarefa); var dois=executor.submit(tarefa); largada.countDown();
            assertThat(List.of(um.get(15,java.util.concurrent.TimeUnit.SECONDS),dois.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204,409);
        }
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(ta.getId())).extracting(EventoAuditoria::getAcao)
                .containsExactlyInAnyOrder("EM_TRANSPORTE","RECEBIDO");
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId())).hasSize(1);
    }
    @Test void leituraConcorrenteAoRecebimentoPreservaOrdemDosEstados() throws Exception {
        String token=token(funcionario);
        String base="/api/v1/transportes/"+ta.getId();
        status(req("POST",base+"/monitoramento",null,token),204);
        var largada=new java.util.concurrent.CountDownLatch(1);
        int statusLeitura;
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var recebimento=executor.submit(() -> {
                if(!largada.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return req("POST",base+"/recebimento",null,token).status();
            });
            var leitura=executor.submit(() -> {
                if(!largada.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return req("POST",base+"/leituras",Map.of("transporteId",ta.getId(),"dispositivoId","DEMO",
                        "temperaturaC",9.3,"medidaEm",clock.instant().toString()),token).status();
            });
            largada.countDown();
            assertThat(recebimento.get(15,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(204);
            statusLeitura=leitura.get(15,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(statusLeitura).isIn(201,409);
        assertThat(transportes.findById(ta.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.RECEBIDO);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(statusLeitura==201
                ? EstadoLote.BLOQUEADO_PREVENTIVAMENTE : EstadoLote.AGUARDANDO_CONFERENCIA);
        assertThat(leituras.count()).isEqualTo(statusLeitura==201 ? 1 : 0);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId())).hasSize(1);
    }
    @Test void recebimentoRevalidaUnidadeAposTransferenciaComContextoAntigo() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(funcionario.getId().toString(),null,List.of()));
        try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()) {
            assertThatThrownBy(() -> new TransactionTemplate(txManager).executeWithoutResult(tx -> {
                assertThat(transportes.findById(ta.getId()).orElseThrow().getUnidadeSaude().getId()).isEqualTo(a.getId());
                var transferencia=executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(gestor.getId().toString(),null,List.of()));
                    try {
                        cadastro.atualizarTransporte(ta.getId(),new br.com.fiap.vacismart.web.dto.TransporteEntradaDto("TRANSFERIDO",b.getId()));
                        // O destino mudou e o monitoramento começou enquanto o primeiro contexto guardava A.
                        new TransactionTemplate(txManager).executeWithoutResult(t -> transportes.findById(ta.getId()).orElseThrow()
                                .iniciarMonitoramento(OffsetDateTime.now(clock)));
                    } finally { SecurityContextHolder.clearContext(); }
                });
                try { transferencia.get(10,java.util.concurrent.TimeUnit.SECONDS); }
                catch(Exception ex) { throw new IllegalStateException("Falha ao preparar transferência",ex); }
                fluxo.receberTransporte(ta.getId());
            })).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(transportes.findById(ta.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.EM_TRANSPORTE);
        assertThat(auditoria.count()).isZero();
    }
    String rotaDecisao(UUID id) { return "/api/v1/lotes/"+id+"/decisao-tecnica"; }
    void prepararDecisao(String token, boolean bloquear) throws Exception {
        String base="/api/v1/transportes/"+ta.getId();
        status(req("POST",base+"/monitoramento",null,token),204);
        if (bloquear) status(req("POST",base+"/leituras",Map.of("transporteId",ta.getId(),"dispositivoId","DEMO",
                "temperaturaC",9.3,"medidaEm",clock.instant().toString()),token),201);
        status(req("POST",base+"/recebimento",null,token),204);
    }
    @ParameterizedTest @CsvSource({"false,LIBERAR,LIBERADO", "false,DESCARTAR,DESCARTADO",
            "true,LIBERAR,LIBERADO", "true,DESCARTAR,DESCARTADO"})
    void decisaoTecnicaAplicaSomenteAsQuatroTransicoesEAudita(boolean bloquear, DecisaoTecnica decisao, EstadoLote destino) throws Exception {
        Lote vizinho=lotes.save(new Lote("LA-VIZINHO",vacina,ta));
        String token=token(gestor);
        prepararDecisao(token,bloquear);
        EstadoLote estadoVizinho=lotes.findById(vizinho.getId()).orElseThrow().getEstado();
        var eventosAntes=auditoria.findAll();
        long leiturasAntes=leituras.count(), ocorrenciasAntes=ocorrencias.count();
        String justificativa=!bloquear && decisao==DecisaoTecnica.LIBERAR ? "J".repeat(1000) : " Conferência humana justificada. ";
        Instant antes=clock.instant();
        var resposta=req("POST",rotaDecisao(la.getId()),Map.of("decisao",decisao,"justificativa",justificativa),token);
        status(resposta,204); assertThat(resposta.corpo().isNull()).isTrue();
        Lote salvo=lotes.findById(la.getId()).orElseThrow();
        assertThat(salvo.getEstado()).isEqualTo(destino);
        assertThat(salvo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
        assertThat(lotes.findById(vizinho.getId()).orElseThrow().getEstado()).isEqualTo(estadoVizinho);
        assertThat(transportes.findById(ta.getId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.RECEBIDO);
        assertThat(leituras.count()).isEqualTo(leiturasAntes);
        assertThat(ocorrencias.count()).isEqualTo(ocorrenciasAntes);
        assertThat(ocorrencias.findAll()).allSatisfy(o -> assertThat(o.getEstado()).isEqualTo(EstadoOcorrencia.ABERTA));
        assertThat(auditoria.count()).isEqualTo(eventosAntes.size()+1);
        assertThat(auditoria.findAll()).extracting(EventoAuditoria::getId).containsAll(eventosAntes.stream().map(EventoAuditoria::getId).toList());
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId()).stream()
                .filter(e -> e.getAcao().equals(destino.name())).toList()).singleElement().satisfies(e -> {
            assertThat(e.getEntidade()).isEqualTo("Lote");
            assertThat(e.getAutor()).isEqualTo(gestor.getId().toString());
            assertThat(e.getJustificativa()).isEqualTo(justificativa);
            assertThat(e.getDataHora().toInstant()).isBetween(antes,clock.instant());
        });
        long eventosDepois=auditoria.count();
        // Os dois destinos são terminais, mesmo para decisão contrária à primeira.
        status(req("POST",rotaDecisao(la.getId()),Map.of("decisao",decisao,"justificativa","Repetição"),token),409);
        status(req("POST",rotaDecisao(la.getId()),Map.of("decisao",decisao==DecisaoTecnica.LIBERAR ? "DESCARTAR" : "LIBERAR",
                "justificativa","Tentativa de revisão"),token),409);
        assertThat(auditoria.count()).isEqualTo(eventosDepois);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(destino);
        var consulta=req("GET","/api/v1/lotes/"+la.getId(),null,token); status(consulta,200);
        assertThat(consulta.corpo().get("estado").asString()).isEqualTo(destino.name());
    }
    @Test void decisaoTecnicaExigeGestorAtivoAntesDeConsultarLote() throws Exception {
        var dados=Map.of("decisao","LIBERAR","justificativa","Conferência humana");
        status(req("POST",rotaDecisao(la.getId()),dados,null),401);
        status(req("POST",rotaDecisao(la.getId()),dados,"invalido"),401);
        for (Usuario usuario:List.of(cidadao,funcionario,outro)) {
            String token=token(usuario);
            status(req("POST",rotaDecisao(la.getId()),dados,token),403);
            status(req("POST",rotaDecisao(UUID.randomUUID()),dados,token),403);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario.getId().toString(),null,List.of()));
            assertThatThrownBy(() -> fluxo.decidirLote(UUID.randomUUID(),DecisaoTecnica.LIBERAR,"Conferência"))
                    .isInstanceOf(AccessDeniedException.class);
        }
        status(req("POST",rotaDecisao(UUID.randomUUID()),dados,token(gestor)),404);
        String token=token(gestor);
        new TransactionTemplate(txManager).executeWithoutResult(tx -> usuarios.findById(gestor.getId()).orElseThrow().desativar());
        status(req("POST",rotaDecisao(la.getId()),dados,token),401);
        assertThat(auditoria.count()).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"decisao-ausente","decisao-nula","decisao-invalida",
            "justificativa-ausente","justificativa-nula","justificativa-branca","justificativa-excessiva","campos-extras","corpo-ausente"})
    void decisaoTecnicaValidaDtoSemAceitarDadosConfiaveisDoCliente(String caso) throws Exception {
        Map<String,Object> dados=new HashMap<>(Map.of("decisao","LIBERAR","justificativa","Conferência"));
        switch(caso) {
            case "decisao-ausente" -> dados.remove("decisao");
            case "decisao-nula" -> dados.put("decisao",null);
            case "decisao-invalida" -> dados.put("decisao","LIBERADO");
            case "justificativa-ausente" -> dados.remove("justificativa");
            case "justificativa-nula" -> dados.put("justificativa",null);
            case "justificativa-branca" -> dados.put("justificativa","   ");
            case "justificativa-excessiva" -> dados.put("justificativa","J".repeat(1001));
            case "campos-extras" -> dados.putAll(Map.of("autor",gestor.getId(),"perfil","GESTOR","unidadeSaudeId",a.getId(),
                    "dataHora",clock.instant().toString(),"disponibilidade","CONFIRMADA","estado","LIBERADO"));
            case "corpo-ausente" -> dados=null;
        }
        status(req("POST",rotaDecisao(la.getId()),dados,token(gestor)),400);
        assertThat(auditoria.count()).isZero();
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(EstadoLote.EM_MONITORAMENTO);
    }
    @ParameterizedTest @ValueSource(strings={"CADASTRADO","EM_TRANSPORTE","FINALIZADO","EM_MONITORAMENTO"})
    void decisaoTecnicaRejeitaTransporteOuLoteIncompativel(String estado) throws Exception {
        String token=token(gestor);
        prepararDecisao(token,false);
        // Estados incompatíveis são fixtures de teste; nenhuma transição artificial entra na API.
        new TransactionTemplate(txManager).executeWithoutResult(tx -> {
            if(estado.equals("EM_MONITORAMENTO")) org.springframework.test.util.ReflectionTestUtils.setField(
                    lotes.findById(la.getId()).orElseThrow(),"estado",EstadoLote.EM_MONITORAMENTO);
            else org.springframework.test.util.ReflectionTestUtils.setField(transportes.findById(ta.getId()).orElseThrow(),
                    "estado",EstadoTransporte.valueOf(estado));
        });
        long eventosAntes=auditoria.count();
        status(req("POST",rotaDecisao(la.getId()),Map.of("decisao","LIBERAR","justificativa","Conferência"),token),409);
        assertThat(auditoria.count()).isEqualTo(eventosAntes);
        assertThat(lotes.findById(la.getId()).orElseThrow().getEstado()).isEqualTo(estado.equals("EM_MONITORAMENTO")
                ? EstadoLote.EM_MONITORAMENTO : EstadoLote.AGUARDANDO_CONFERENCIA);
    }
    @Test void decisoesTecnicasConcorrentesGeramUmaUnicaTransicao() throws Exception {
        String token=token(gestor);
        prepararDecisao(token,false);
        long eventosAntes=auditoria.count();
        var largada=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var liberar=executor.submit(() -> {
                if(!largada.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return req("POST",rotaDecisao(la.getId()),Map.of("decisao","LIBERAR","justificativa","Liberar após conferência"),token).status();
            });
            var descartar=executor.submit(() -> {
                if(!largada.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
                return req("POST",rotaDecisao(la.getId()),Map.of("decisao","DESCARTAR","justificativa","Descartar após conferência"),token).status();
            });
            largada.countDown();
            assertThat(List.of(liberar.get(15,java.util.concurrent.TimeUnit.SECONDS),descartar.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204,409);
        }
        assertThat(auditoria.count()).isEqualTo(eventosAntes+1);
        EstadoLote destino=lotes.findById(la.getId()).orElseThrow().getEstado();
        assertThat(destino).isIn(EstadoLote.LIBERADO,EstadoLote.DESCARTADO);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(la.getId()).stream()
                .filter(e -> e.getAcao().equals(destino.name())).toList()).hasSize(1);
    }
    @Test void corsPermiteApenasOrigemExplicitaEAuthorization() throws Exception {
        for(String origem: List.of("http://localhost:5173","https://nao-autorizada.invalid")) {
            var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+porta+"/api/v1/transportes"))
                .header("Origin",origem).header("Access-Control-Request-Method","GET")
                .header("Access-Control-Request-Headers","Authorization").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
            var res=cliente.send(request,HttpResponse.BodyHandlers.ofString());
            if(origem.contains("localhost")) {
                assertThat(res.statusCode()).isEqualTo(200);
                assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).contains(origem);
                assertThat(res.headers().firstValue("Access-Control-Allow-Headers").orElse("")).containsIgnoringCase("Authorization");
            } else { assertThat(res.statusCode()).isEqualTo(403); assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).isEmpty(); }
        }
        status(req("GET","/api/v1/transportes",null,null),401);
    }
}
