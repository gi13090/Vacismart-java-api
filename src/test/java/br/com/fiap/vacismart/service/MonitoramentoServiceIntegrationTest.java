package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.entity.*;
import br.com.fiap.vacismart.domain.enums.*;
import br.com.fiap.vacismart.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static br.com.fiap.vacismart.domain.enums.EstadoLote.*;
import static br.com.fiap.vacismart.domain.enums.EstadoOcorrencia.*;
import static br.com.fiap.vacismart.domain.enums.TipoOcorrencia.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({br.com.fiap.vacismart.security.ChavesTesteConfig.class, MonitoramentoServiceIntegrationTest.RelogioTesteConfig.class})
@Transactional
class MonitoramentoServiceIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    br.com.fiap.vacismart.security.AcessoService acesso;
    @Autowired MonitoramentoService service;
    @Autowired FluxoOperacionalService fluxo;
    @Autowired TransporteRepository transportes;
    @Autowired UnidadeSaudeRepository unidades;
    @Autowired VacinaRepository vacinas;
    @Autowired LoteRepository lotes;
    @Autowired LeituraTemperaturaRepository leituras;
    @Autowired OcorrenciaRepository ocorrencias;
    @Autowired EventoAuditoriaRepository auditoria;
    @Autowired EntityManager em;
    @Autowired RelogioMutavel relogio;
    @Autowired PlatformTransactionManager transactionManager;
    private static final Instant INSTANTE = Instant.parse("2026-11-08T15:00:00Z");

    @BeforeEach
    void ajustarRelogio() { relogio.definir(INSTANTE); }

    @ParameterizedTest
    @CsvSource({"5.2,NORMAL", "5.2000,NORMAL", "7.5,ATENCAO", "2.0,ATENCAO", "8.0,ATENCAO"})
    void persisteLeiturasSemBloquearNemDescartar(String temperatura, ClassificacaoTemperatura esperada) {
        Cenario c = cenario(true);
        LeituraTemperatura leitura = registrar(c, temperatura);
        em.flush(); em.clear();
        LeituraTemperatura salva = leituras.findById(leitura.getId()).orElseThrow();
        assertThat(salva.getClassificacao()).isEqualTo(esperada);
        assertThat(salva.getTemperaturaC()).isEqualByComparingTo(temperatura);
        assertThat(salva.getMedidaEm().toInstant()).isEqualTo(INSTANTE);
        assertThat(salva.getRecebidaEm().toInstant()).isEqualTo(INSTANTE);
        assertThat(salva.getDispositivoId()).isEqualTo("ARDUINO-DEMO-01");
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
        assertThat(ocorrencias.count()).isZero();
    }

    @Test
    void desvioAbreOcorrenciaBloqueiaTodosAfetadosEAuditaUmaVez() {
        Cenario c = cenario(true);
        Lote segundo = lotes.save(new Lote("L-" + UUID.randomUUID(), vacinas.findById(c.vacinaId()).orElseThrow(),
                transportes.findById(c.transporteId()).orElseThrow()));
        registrar(c, "9.3");
        registrar(c, "9.3");
        em.flush(); em.clear();
        List<Lote> bloqueados = lotes.findByTransporteId(c.transporteId());
        assertThat(bloqueados).hasSize(2).allSatisfy(lote -> {
            assertThat(lote.getEstado()).isEqualTo(BLOQUEADO_PREVENTIVAMENTE);
            assertThat(lote.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
            assertThat(lote.getEstado()).isNotEqualTo(DESCARTADO);
        });
        Ocorrencia ocorrencia = ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), FORA_DA_FAIXA, ABERTA).orElseThrow();
        assertThat(ocorrencias.count()).isEqualTo(1);
        assertThat(leituras.findByTransporteIdOrderByRecebidaEmAsc(c.transporteId())).hasSize(2);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).singleElement().satisfies(evento -> {
            assertThat(evento.getAcao()).isEqualTo("BLOQUEADO_PREVENTIVAMENTE");
            assertThat(evento.getEntidade()).isEqualTo("Lote");
            assertThat(evento.getAutor()).isEqualTo("SISTEMA");
            assertThat(evento.getDataHora().toInstant()).isEqualTo(INSTANTE);
            assertThat(evento.getJustificativa()).contains("leitura");
        });
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(segundo.getId())).hasSize(1);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(ocorrencia.getId())).singleElement()
                .extracting(EventoAuditoria::getAcao).isEqualTo("ABERTA");
    }

    @Test
    void normalizacaoEEncerramentoNaoLiberamNemDescartam() {
        Cenario c = cenario(true);
        registrar(c, "9.3");
        registrar(c, "5.2");
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), FORA_DA_FAIXA, ABERTA)).isPresent();
        service.encerrarOcorrencia(c.transporteId(), FORA_DA_FAIXA, "Conferência pendente");
        em.flush(); em.clear();
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(BLOQUEADO_PREVENTIVAMENTE);
        assertThat(lotes.findById(c.loteId()).orElseThrow().getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
        Ocorrencia encerrada = ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), FORA_DA_FAIXA, ENCERRADA).orElseThrow();
        assertThat(encerrada.getEncerradaEm()).isNotNull();
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(encerrada.getId()))
                .extracting(EventoAuditoria::getAcao).containsExactlyInAnyOrder("ABERTA", "ENCERRADA");
        registrar(c, "9.3");
        assertThat(ocorrencias.count()).isEqualTo(2);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).hasSize(1);
    }

    @Test
    void classificaPorVacinaEBloqueiaSomenteLotesAfetadosDoTransporte() {
        Cenario c = cenario(true);
        Vacina outra = vacinas.save(new Vacina("Outra", new BigDecimal("8"), new BigDecimal("12"), BigDecimal.ONE));
        Lote naoAfetado = lotes.save(new Lote("L-" + UUID.randomUUID(), outra,
                transportes.findById(c.transporteId()).orElseThrow()));
        Cenario outroTransporte = cenario(true);
        assertThat(registrar(c, "9.3").getClassificacao()).isEqualTo(ClassificacaoTemperatura.FORA_DA_FAIXA);
        em.flush(); em.clear();
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(BLOQUEADO_PREVENTIVAMENTE);
        assertThat(lotes.findById(naoAfetado.getId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
        assertThat(lotes.findById(outroTransporte.loteId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoLote.class, names = {"AGUARDANDO_CONFERENCIA", "LIBERADO", "DESCARTADO", "BLOQUEADO_PREVENTIVAMENTE"})
    void preservaLotesQueNaoEstaoEmMonitoramento(EstadoLote estado) {
        Cenario c = cenario(true);
        ReflectionTestUtils.setField(lotes.findById(c.loteId()).orElseThrow(), "estado", estado);
        registrar(c, "9.3");
        em.flush(); em.clear();
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(estado);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTransporte.class, names = {"CADASTRADO", "RECEBIDO", "FINALIZADO"})
    void rejeitaEstadoIncompativelSemPersistirLeitura(EstadoTransporte estado) {
        Cenario c = cenario(false);
        // Estados de etapas futuras são preparados apenas no teste, sem ampliar a API de domínio.
        ReflectionTestUtils.setField(transportes.findById(c.transporteId()).orElseThrow(), "estado", estado);
        assertThatIllegalStateException().isThrownBy(() -> registrar(c, "9.3"));
        assertThat(leituras.count()).isZero();
        assertThat(ocorrencias.count()).isZero();
    }

    @Test
    void rejeitaTransporteInexistenteEDadosInvalidos() {
        Cenario c = cenario(true);
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> service.registrarLeitura(UUID.randomUUID(),
                "ARDUINO-DEMO-01", BigDecimal.TEN, agora()));
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrarLeitura(c.transporteId(), " ", BigDecimal.TEN, agora()));
        assertThatNullPointerException().isThrownBy(() -> service.registrarLeitura(c.transporteId(), "D", null, agora()));
        assertThatNullPointerException().isThrownBy(() -> service.registrarLeitura(c.transporteId(), "D", BigDecimal.TEN, null));
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrarLeitura(c.transporteId(), "D", BigDecimal.TEN, agora().plusSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.registrarLeitura(c.transporteId(), "D", BigDecimal.TEN, agora().minusSeconds(11)));
        assertThatIllegalArgumentException().isThrownBy(() -> registrar(c, "9.3001"));
        assertThat(leituras.count()).isZero();
        assertThat(ocorrencias.count()).isZero();
    }

    @Test
    void inicioExigeLotesECadastradoEAuditaTransicao() {
        Cenario c = cenario(false);
        service.iniciarMonitoramento(c.transporteId());
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.transporteId())).singleElement()
                .extracting(EventoAuditoria::getAcao).isEqualTo("EM_TRANSPORTE");
        assertThatIllegalStateException().isThrownBy(() -> service.iniciarMonitoramento(c.transporteId()));
        Transporte vazio = transportes.save(new Transporte("T-" + UUID.randomUUID(), unidades.findById(c.unidadeId()).orElseThrow()));
        assertThatIllegalStateException().isThrownBy(() -> service.iniciarMonitoramento(vazio.getId()));
    }

    @Test
    void relacionamentosPersistemSemCascadeEResolvemAposLimparContexto() {
        Cenario c = cenario(true);
        LeituraTemperatura leitura = registrar(c, "9.3");
        em.flush(); em.clear();
        Lote lote = lotes.findById(c.loteId()).orElseThrow();
        assertThat(lote.getVacina().getNome()).isEqualTo("Vacina Demo A");
        assertThat(lote.getTransporte().getUnidadeSaude().getNome()).isEqualTo("Unidade Demonstrativa 01");
        assertThat(leituras.findById(leitura.getId()).orElseThrow().getTransporte().getId()).isEqualTo(c.transporteId());
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), FORA_DA_FAIXA, ABERTA)
                .orElseThrow().getTransporte().getId()).isEqualTo(c.transporteId());
        assertThat(lote.getVacina().getId()).isEqualTo(c.vacinaId());
    }

    @Test
    void telemetriaUsaInicioSemLeituraELimiteEstritoSemBloquear() {
        Cenario c = cenario(false);
        service.iniciarMonitoramento(c.transporteId());
        relogio.definir(INSTANTE.plusSeconds(180));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isFalse();
        relogio.definir(INSTANTE.plusSeconds(181));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isTrue();
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isFalse();
        em.flush(); em.clear();
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), PERDA_DE_TELEMETRIA, ABERTA)).isPresent();
        assertThat(ocorrencias.count()).isEqualTo(1);
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
    }

    @Test
    void telemetriaUsaRecebimentoMesmoComMedicaoAtrasadaEEncerraNaRetomada() {
        Cenario c = cenario(true);
        registrar(c, "9.3");
        relogio.definir(INSTANTE.plusSeconds(181));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isTrue();
        service.registrarLeitura(c.transporteId(), "D", new BigDecimal("5.2"), agora().minusSeconds(100));
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), PERDA_DE_TELEMETRIA, ABERTA)).isEmpty();
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), PERDA_DE_TELEMETRIA, ENCERRADA)).isPresent();
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), FORA_DA_FAIXA, ABERTA)).isPresent();
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(BLOQUEADO_PREVENTIVAMENTE);
        relogio.definir(INSTANTE.plusSeconds(361));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isFalse();
        relogio.definir(INSTANTE.plusSeconds(362));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isTrue();
    }

    @Test
    void telemetriaNaoAbreForaDeTransporte() {
        Cenario c = cenario(false);
        relogio.definir(INSTANTE.plusSeconds(999));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isFalse();
        assertThat(ocorrencias.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackDesfazLeituraOcorrenciaBloqueioEAuditoria() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Cenario c = tx.execute(status -> cenario(true));
        try {
            long eventosAntes = auditoria.count();
            assertThatIllegalStateException().isThrownBy(() -> tx.executeWithoutResult(status -> {
                registrar(c, "9.3");
                em.flush();
                throw new IllegalStateException("Falha simulada após todas as alterações");
            }));
            assertThat(leituras.count()).isZero();
            assertThat(ocorrencias.count()).isZero();
            assertThat(auditoria.count()).isEqualTo(eventosAntes);
            assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
        } finally { limparComprometidos(tx); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void leiturasConcorrentesNaoDuplicamOcorrenciaAberta() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Cenario c = tx.execute(status -> cenario(true));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        Callable<UUID> tarefa = () -> {
            if (!largada.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de largada");
            return registrar(c, "9.3").getId();
        };
        try {
            Future<UUID> primeira = executor.submit(tarefa);
            Future<UUID> segunda = executor.submit(tarefa);
            largada.countDown();
            assertThat(primeira.get(15, TimeUnit.SECONDS)).isNotEqualTo(segunda.get(15, TimeUnit.SECONDS));
            assertThat(leituras.count()).isEqualTo(2);
            assertThat(ocorrencias.count()).isEqualTo(1);
            assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).hasSize(1);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            limparComprometidos(tx);
        }
    }

    private UUID autorRecebimento() {
        Usuario usuario = org.mockito.Mockito.mock(Usuario.class);
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.when(usuario.getId()).thenReturn(id);
        org.mockito.Mockito.when(acesso.atual()).thenReturn(usuario);
        return id;
    }

    @ParameterizedTest
    @CsvSource({"180,false", "181,true"})
    void recebimentoVerificaSilencioSemPrimeiraLeitura(long segundos, boolean perda) {
        UUID autor = autorRecebimento();
        Cenario c = cenario(false);
        service.iniciarMonitoramento(c.transporteId());
        relogio.definir(INSTANTE.plusSeconds(segundos));
        fluxo.receberTransporte(c.transporteId());
        em.flush(); em.clear();
        assertThat(ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), PERDA_DE_TELEMETRIA, ABERTA).isPresent()).isEqualTo(perda);
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(AGUARDANDO_CONFERENCIA);
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).singleElement().satisfies(e -> {
            assertThat(e.getAutor()).isEqualTo(autor.toString());
            assertThat(e.getDataHora().toInstant()).isEqualTo(INSTANTE.plusSeconds(segundos));
        });
    }

    @Test
    void recebimentoUsaUltimaRecepcaoEPreservaTelemetriaAbertaSemDuplicar() {
        autorRecebimento();
        Cenario c = cenario(true);
        registrar(c,"5.2");
        relogio.definir(INSTANTE.plusSeconds(181));
        assertThat(service.verificarPerdaTelemetria(c.transporteId())).isTrue();
        fluxo.receberTransporte(c.transporteId());
        em.flush(); em.clear();
        assertThat(ocorrencias.count()).isEqualTo(1);
        Ocorrencia aberta = ocorrencias.findByTransporteIdAndTipoAndEstado(c.transporteId(), PERDA_DE_TELEMETRIA, ABERTA).orElseThrow();
        assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(aberta.getId())).singleElement()
                .extracting(EventoAuditoria::getAcao).isEqualTo("ABERTA");
        assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(AGUARDANDO_CONFERENCIA);
    }

    @Test
    void recebimentoNaoConfundeMedicaoAtrasadaComSilencioNaRecepcao() {
        autorRecebimento();
        Cenario c = cenario(false);
        service.iniciarMonitoramento(c.transporteId());
        relogio.definir(INSTANTE.plusSeconds(170));
        service.registrarLeitura(c.transporteId(),"DEMO",new BigDecimal("5.2"),OffsetDateTime.ofInstant(INSTANTE, relogio.getZone()));
        relogio.definir(INSTANTE.plusSeconds(181));
        fluxo.receberTransporte(c.transporteId());
        em.flush(); em.clear();
        assertThat(ocorrencias.count()).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackDoRecebimentoDesfazTransporteLotesTelemetriaEAuditoria() {
        autorRecebimento();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Cenario c = tx.execute(status -> cenario(true));
        relogio.definir(INSTANTE.plusSeconds(181));
        try {
            long eventosAntes = auditoria.count();
            assertThatIllegalStateException().isThrownBy(() -> tx.executeWithoutResult(status -> {
                fluxo.receberTransporte(c.transporteId());
                em.flush();
                throw new IllegalStateException("Falha simulada após persistência do recebimento");
            }));
            assertThat(transportes.findById(c.transporteId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.EM_TRANSPORTE);
            assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(EM_MONITORAMENTO);
            assertThat(ocorrencias.count()).isZero();
            assertThat(auditoria.count()).isEqualTo(eventosAntes);
        } finally { limparComprometidos(tx); }
    }

    @Test
    void decisaoTecnicaValidaComandoEJustificativaTambemNoDominio() {
        Cenario c=cenario(true);
        Transporte transporte=transportes.findById(c.transporteId()).orElseThrow();
        transporte.receber();
        Lote lote=lotes.findById(c.loteId()).orElseThrow();
        lote.encaminharParaConferencia();
        assertThatIllegalArgumentException().isThrownBy(() -> lote.decidirTecnicamente(null,"Conferência"));
        for(String invalida:Arrays.asList(null,"","   ","J".repeat(1001))) {
            assertThatIllegalArgumentException().isThrownBy(() -> lote.decidirTecnicamente(DecisaoTecnica.LIBERAR,invalida));
        }
        assertThat(lote.getEstado()).isEqualTo(AGUARDANDO_CONFERENCIA);
        assertThat(lote.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void decisaoTecnicaRollbackPreservaLoteHistoricoEOcorrencias() {
        UUID autor=autorRecebimento();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        Cenario c=tx.execute(status -> {
            Cenario criado=cenario(true);
            registrar(criado,"9.3");
            relogio.definir(INSTANTE.plusSeconds(181));
            fluxo.receberTransporte(criado.transporteId());
            return criado;
        });
        try {
            long eventosAntes=auditoria.count(), leiturasAntes=leituras.count();
            assertThatIllegalStateException().isThrownBy(() -> tx.executeWithoutResult(status -> {
                fluxo.decidirLote(c.loteId(),DecisaoTecnica.LIBERAR,"Conferência humana");
                em.flush();
                assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId()).stream().filter(e -> e.getAcao().equals("LIBERADO")).toList())
                        .singleElement().satisfies(e -> {
                            assertThat(e.getAutor()).isEqualTo(autor.toString());
                            assertThat(e.getDataHora().toInstant()).isEqualTo(INSTANTE.plusSeconds(181));
                            assertThat(e.getJustificativa()).isEqualTo("Conferência humana");
                        });
                throw new IllegalStateException("Falha simulada após flush de lote e decisão auditada");
            }));
            Lote salvo=lotes.findById(c.loteId()).orElseThrow();
            assertThat(salvo.getEstado()).isEqualTo(BLOQUEADO_PREVENTIVAMENTE);
            assertThat(salvo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
            assertThat(transportes.findById(c.transporteId()).orElseThrow().getEstado()).isEqualTo(EstadoTransporte.RECEBIDO);
            assertThat(auditoria.count()).isEqualTo(eventosAntes);
            assertThat(leituras.count()).isEqualTo(leiturasAntes);
            assertThat(ocorrencias.findAll()).hasSize(2).allSatisfy(o -> assertThat(o.getEstado()).isEqualTo(ABERTA));
        } finally { limparComprometidos(tx); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void decisaoTecnicaRecarregaLoteAposLockMesmoComEstadoAntigoNoContextoJpa() throws Exception {
        autorRecebimento();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        Cenario c=tx.execute(status -> {
            Cenario criado=cenario(true);
            fluxo.receberTransporte(criado.transporteId());
            return criado;
        });
        try(var executor=Executors.newSingleThreadExecutor()) {
            long eventosAntes=auditoria.count();
            assertThatIllegalStateException().isThrownBy(() -> tx.executeWithoutResult(status -> {
                Lote antigo=lotes.findById(c.loteId()).orElseThrow();
                assertThat(antigo.getEstado()).isEqualTo(AGUARDANDO_CONFERENCIA);
                var concorrente=executor.submit(() -> fluxo.decidirLote(c.loteId(),DecisaoTecnica.DESCARTAR,"Decisão concorrente"));
                try { concorrente.get(10,TimeUnit.SECONDS); }
                catch(Exception ex) { throw new IllegalStateException("Falha ao preparar decisão concorrente",ex); }
                assertThat(antigo.getEstado()).isEqualTo(AGUARDANDO_CONFERENCIA);
                fluxo.decidirLote(c.loteId(),DecisaoTecnica.LIBERAR,"Não deve sobrescrever descarte");
            }));
            assertThat(lotes.findById(c.loteId()).orElseThrow().getEstado()).isEqualTo(DESCARTADO);
            assertThat(auditoria.count()).isEqualTo(eventosAntes+1);
            assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId())).extracting(EventoAuditoria::getAcao)
                    .containsExactlyInAnyOrder("AGUARDANDO_CONFERENCIA","DESCARTADO");
        } finally { limparComprometidos(tx); }
    }

    private Cenario cenarioLiberado() {
        Cenario c=cenario(true);
        fluxo.receberTransporte(c.transporteId());
        fluxo.decidirLote(c.loteId(),DecisaoTecnica.LIBERAR,"Conferência humana");
        return c;
    }

    @Test
    void disponibilidadeDominioRejeitaNuloERepeticaoNaoRenovaHorario() {
        UUID autor=autorRecebimento();
        Cenario c=cenarioLiberado();
        Lote lote=lotes.findById(c.loteId()).orElseThrow();
        assertThatIllegalArgumentException().isThrownBy(() -> lote.alterarDisponibilidade(null));
        long eventosAntes=auditoria.count();
        fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.NAO_DISPONIVEL);
        assertThat(auditoria.count()).isEqualTo(eventosAntes);
        fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.CONFIRMADA);
        em.flush();
        var confirmacao=auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId()).stream()
                .filter(e -> e.getAcao().equals("DISPONIBILIDADE_CONFIRMADA")).findFirst().orElseThrow();
        assertThat(confirmacao.getDataHora().toInstant()).isEqualTo(INSTANTE);
        assertThat(confirmacao.getAutor()).isEqualTo(autor.toString());
        relogio.definir(INSTANTE.plusSeconds(60));
        fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.CONFIRMADA);
        em.flush();em.clear();
        assertThat(auditoria.count()).isEqualTo(eventosAntes+1);
        assertThat(auditoria.findById(confirmacao.getId()).orElseThrow().getDataHora().toInstant()).isEqualTo(INSTANTE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void disponibilidadeRollbackReverteCampoEAuditoriaJuntos() {
        autorRecebimento();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        Cenario c=tx.execute(status -> cenarioLiberado());
        try {
            long eventos=auditoria.count();
            assertThatIllegalStateException().isThrownBy(() -> tx.executeWithoutResult(status -> {
                fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.CONFIRMADA);
                em.flush();
                assertThat(auditoria.count()).isEqualTo(eventos+1);
                throw new IllegalStateException("Falha simulada após flush de disponibilidade e evento");
            }));
            Lote salvo=lotes.findById(c.loteId()).orElseThrow();
            assertThat(salvo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
            assertThat(salvo.getEstado()).isEqualTo(LIBERADO);
            assertThat(auditoria.count()).isEqualTo(eventos);
        } finally { limparComprometidos(tx); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void disponibilidadeRefreshAposLockEvitaEventoDuplicadoComContextoAntigo() {
        autorRecebimento();
        TransactionTemplate tx=new TransactionTemplate(transactionManager);
        Cenario c=tx.execute(status -> cenarioLiberado());
        try(var executor=Executors.newSingleThreadExecutor()) {
            long eventos=auditoria.count();
            tx.executeWithoutResult(status -> {
                Lote antigo=lotes.findById(c.loteId()).orElseThrow();
                assertThat(antigo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
                var concorrente=executor.submit(() -> fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.CONFIRMADA));
                try { concorrente.get(10,TimeUnit.SECONDS); }
                catch(Exception ex) { throw new IllegalStateException("Falha ao preparar confirmação concorrente",ex); }
                assertThat(antigo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.NAO_DISPONIVEL);
                fluxo.alterarDisponibilidade(c.loteId(),DisponibilidadeLote.CONFIRMADA);
                assertThat(antigo.getDisponibilidade()).isEqualTo(DisponibilidadeLote.CONFIRMADA);
            });
            assertThat(auditoria.count()).isEqualTo(eventos+1);
            assertThat(auditoria.findByReferenciaIdOrderByDataHoraAsc(c.loteId()).stream()
                    .filter(e -> e.getAcao().equals("DISPONIBILIDADE_CONFIRMADA")).toList()).hasSize(1);
        } finally { limparComprometidos(tx); }
    }

    private void limparComprometidos(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            auditoria.deleteAllInBatch(); ocorrencias.deleteAllInBatch(); leituras.deleteAllInBatch();
            lotes.deleteAllInBatch(); transportes.deleteAllInBatch(); vacinas.deleteAllInBatch(); unidades.deleteAllInBatch();
        });
    }

    private Cenario cenario(boolean iniciar) {
        UnidadeSaude unidade = unidades.save(new UnidadeSaude("Unidade Demonstrativa 01"));
        Vacina vacina = vacinas.save(new Vacina("Vacina Demo A", new BigDecimal("2.0"), new BigDecimal("8.0"), BigDecimal.ONE));
        Transporte transporte = transportes.save(new Transporte("T-" + UUID.randomUUID(), unidade));
        Lote lote = lotes.save(new Lote("L-" + UUID.randomUUID(), vacina, transporte));
        if (iniciar) {
            relogio.definir(INSTANTE.minusSeconds(10));
            service.iniciarMonitoramento(transporte.getId());
            relogio.definir(INSTANTE);
        }
        return new Cenario(transporte.getId(), lote.getId(), vacina.getId(), unidade.getId());
    }

    private LeituraTemperatura registrar(Cenario c, String temperatura) {
        return service.registrarLeitura(c.transporteId(), "ARDUINO-DEMO-01", new BigDecimal(temperatura), agora());
    }

    private OffsetDateTime agora() { return OffsetDateTime.now(relogio); }
    private record Cenario(UUID transporteId, UUID loteId, UUID vacinaId, UUID unidadeId) { }

    @TestConfiguration
    static class RelogioTesteConfig {
        @Bean @Primary
        RelogioMutavel relogioTeste() { return new RelogioMutavel(); }
    }

    static class RelogioMutavel extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>(INSTANTE);
        void definir(Instant valor) { instante.set(valor); }
        @Override public ZoneId getZone() { return ZoneId.of("America/Sao_Paulo"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return instante.get(); }
    }
}
