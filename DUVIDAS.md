# Dúvidas e decisões — VaciSmart

Registros iniciados em 06/11/2026; atualização documental em **12/11/2026** (America/Sao_Paulo). As seções datadas preservam as decisões e limitações de suas etapas; as atualizações posteriores indicam o estado vigente. ABERTA indica confirmação pendente, mesmo quando a opção provisória já foi adotada. A J4 está oficialmente encerrada e aprovada pelo responsável.

| ID | Data | Contexto | Dúvida ou decisão | Opção adotada | Alternativas | Impacto | Status |
|---|---|---|---|---|---|---|---|
| D-001 | 2026-11-06 | Versão do Spring Boot | Qual versão estável usar? | 4.1.2, mais recente estável indicada em spring.io em 06/11/2026, compatível com Java 21. | 3.5.16 ou 4.0.9; reverter alterando parent e adequando módulos/configuração. | Boot 4 usa Jackson 3 e módulos de autoconfiguração separados. | ABERTA |
| D-002 | 2026-11-06; revista em 2026-11-12 | Contrato compartilhado ausente | 02_MODELO_E_CONTRATOS.md continua ausente do workspace. O documento externo VaciSmart_Integracao_Mobile_Gemini_J4_2026-11-10.md foi informado pelo responsável, mas não localizado no workspace; não houve busca indiscriminada em diretórios privados. | Criado [contrato interno Mobile/Gemini](docs/INTEGRACAO_MOBILE_GEMINI.md) a partir dos controllers/DTOs reais; contratos HTTP preservados. /lotes/{id}/decisao-tecnica segue planejado. | Comparar com o contrato original da equipe e validar o aplicativo conjuntamente; não presumir equivalência entre os documentos. | A versão interna está disponível para revisão/compartilhamento posterior, ainda não adicionada ao Git. Sua criação não comprova alinhamento bilateral nem encerra a integração. | ABERTA |
| D-003 | 2026-11-06 | Banco de testes | H2 em modo PostgreSQL em vez de Testcontainers. | H2 em memória no perfil test. | Testcontainers; reverter substituindo datasource/dependência de teste. | Não comprova compatibilidade SQL com PostgreSQL. | ABERTA |
| D-004 | 2026-11-06 | Migrations e seed | Como versionar banco e dados fictícios? | Flyway com core e módulo PostgreSQL; V0 apenas comentário, sem seed na J1. | SQL manual; reverter removendo Flyway e definindo outro processo antes de J2. | Esquema e seed serão migrations futuras; não editar migrations já aplicadas. | ABERTA |
| D-005 | 2026-11-06 | Idioma | Como nomear domínio e classes técnicas? | Domínio/contrato em português e sufixos técnicos em inglês. | Tudo em português; reverter antes de publicar contrato, renomeando referências. | Contrato deve permanecer fiel à especificação. | ABERTA |
| D-006 | 2026-11-06 | Regras futuras ambíguas | Aceite de leituras e ciclo de ocorrências. | Só aceitar leitura EM_TRANSPORTE; no máximo uma ABERTA por tipo/transporte; encerrar perda de telemetria quando leituras voltarem. Nenhuma regra implementada na J1. | Rever com grupo antes de J2; alterar decisão e testes antes de implementar. | Exige confirmação antes das regras de negócio. | ABERTA |
| D-007 | 2026-11-06; superada em 2026-11-12 | Conflito histórico de escopo | Em 06/11, a dispensa inicial de Cache, HATEOAS e Actuator foi revertida pelo solicitante diante dos materiais oficiais. | Naquela data, os três recursos eram previstos para etapas posteriores, sem implementação na J1. Este planejamento foi revogado pela decisão definitiva D-046. | Nova orientação explícita do responsável em 12/11 excluiu os três recursos; não integram o planejamento vigente. | Registro histórico preservado; AGENTS.md alinhado na consolidação autorizada de 12/11, sem alterar as demais regras de governança. | SUPERADA por D-046 |
| D-008 | 2026-11-06 | Autoconfiguração Flyway no Boot 4 | core sozinho não ativa integração Spring no Boot 4. | Adicionar spring-boot-flyway, somente módulo técnico da integração já solicitada, além de flyway-core e flyway-database-postgresql. | Starter Flyway (traz o mesmo módulo); reverter trocando as declarações equivalentes. | Adaptação necessária à versão mais recente, sem recurso funcional adicional. | ABERTA |
| D-009 | 2026-11-06 | Perfil e ambiente local | Como impedir defaults locais fora de dev? | Sem perfil padrão; credenciais e defaults apenas em dev. .env é lido pelo Compose; exportar as mesmas variáveis para API. | Perfil dev padrão; reverter em application.yml e instruções. | Execução requer selecionar dev; testes selecionam test explicitamente. | ABERTA |
| D-010 | 2026-11-06 | Banco local | Qual imagem e persistência usar? | postgres:17.6, imagem oficial com versão fixa, volume nomeado e healthcheck; porta 5432. | Outra versão suportada; reverter imagem com migração/backup do volume, nunca apagar dados automaticamente. | Somente infraestrutura local; não é implantação final. | ABERTA |
| D-011 | 2026-11-06 | Fuso e Jackson 3 | Como aplicar fuso padrão sem criar classes nos pacotes reservados? | TimeZone no main; Surefire com user.timezone; Jackson timezone e datatype.datetime.write-dates-as-timestamps=false. | TZ/JVM externamente; reverter main/configuração/plugin em conjunto. | Datas com offset em ISO-8601; perfil test usa mesmo fuso. | ABERTA |

| D-012 | 2026-11-06 | Ferramentas Java locais | Runtime Java 21 instalado sem javac causou falha inicial: release version 21 not supported. | Com autorização explícita, usar Eclipse Temurin 21.0.13+9 em instalação temporária e JAVA_HOME/PATH por comando; SHA-256 verificado na API oficial Adoptium. | JDK completo instalado pelo usuário; para reverter basta deixar de selecionar o JDK temporário, sem mudança global a desfazer. | java e javac 21.0.13, Maven 3.9.11 conferidos antes dos testes; nenhum ajuste permanente no sistema. | CONFIRMADA |
| D-013 | 2026-11-06 | Docker e validação dev | Em 06/11, Docker CLI presente, mas daemon indisponível; /var/run/docker.sock ausente. | Registro daquela etapa: Compose validado sintaticamente, PostgreSQL/Flyway/startup dev ainda não validados. | Acesso e validação do esquema existente foram obtidos em 09/11 (D-034); fechamento operacional J4 em 12/11 abaixo. | Impedimento histórico resolvido; não equivale a validação de produção ou carga/concorrência PostgreSQL. | RESOLVIDA no escopo de acesso/startup/esquema existente |
| D-014 | 2026-11-06 | Maven Wrapper | Qual versão e distribuição fixar? | Wrapper oficial 3.3.4 only-script com Maven 3.9.11; scripts Unix e Windows, sem JAR adicional. | Outro Maven compatível; reverter regenerando o wrapper com versão fixada e repetindo testes. | Primeiro uso precisa acesso ao Maven Central; testado em Linux. Script Windows não executado neste ambiente. | ABERTA |

## Fontes técnicas consultadas
- https://spring.io/projects/spring-boot/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://spring.io/blog/2025/10/28/modularizing-spring-boot/
- https://docs.spring.io/spring-boot/appendix/application-properties/index.html

## Validação histórica da J1 — 06/11/2026
- `java -version`: Eclipse Temurin 21.0.13+9; `javac -version`: 21.0.13; `./mvnw -version`: Apache Maven 3.9.11 usando Temurin 21.0.13. Verificações anteriores à execução final de testes.
- JDK obtido de https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse ; checksum confirmado via https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse . SHA-256: `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`.
- `./mvnw -B test`, com JAVA_HOME/PATH do JDK temporário selecionados para a execução: BUILD SUCCESS; 1 teste, 0 falhas/erros/ignorados. Contexto test com H2 carregado. Avisos não fatais de annotation processing e Mockito/Byte Buddy registrados; nenhum teste falhou.
- Primeira tentativa falhou pela falta de javac 21 (D-012); tentativa final resolveu a dificuldade com JDK temporário, sem alteração do sistema.
- `docker compose -f compose.dev.yml config --quiet`: sucesso. `docker compose -f compose.dev.yml up -d --wait`: falha por daemon indisponível (D-013). Não foram validados PostgreSQL, aplicação da V0 ou inicialização dev. `spring-boot:run` com dev não foi executado.
- `git diff --check`: sucesso. `.env` e target ignorados. docs/ESPECIFICACAO.md comparado por igualdade com o conteúdo do pedido: idêntico.
- Sem entidades, regras ou recursos de J2 implementados naquela data. O planejamento então vigente para Cache, HATEOAS e Actuator (D-007) foi posteriormente revogado pela decisão definitiva D-046, de 12/11/2026.

## Decisões e ambiguidades da J2 — 08/11/2026

O pedido J2 confirma os critérios de D-006: leitura somente EM_TRANSPORTE, uma ABERTA por transporte/tipo e retomada encerrando telemetria. As evidências anteriores são históricas da J1; nenhuma afirma validação atual de PostgreSQL.

| ID | Decisão adotada | Alternativa / reversão | Impacto e situação |
|---|---|---|---|
| D-015 | Faixas distintas: classificar individualmente por vacina; leitura registra a pior classificação, bloqueia só lotes afetados EM_MONITORAMENTO. | Rejeitar remessas mistas ou criar avaliação persistida por vacina/leitura; exige alinhar contrato e migration adicional. | Conservadora para um sensor ambiental compartilhado, sem inferência clínica. Associação individual fica nos eventos de bloqueio; leitura agregada não é classificação universal. Provisória para revisão. |
| D-016 | Adicionar inicioMonitoramentoEm; início interno CADASTRADO → EM_TRANSPORTE exige lotes e audita SISTEMA. | Referência externa ao cadastro/início; substituir com migration e alinhamento da origem do relógio. | Evita ausência de referência para primeira leitura na RN05. Ações de recebimento/decisão e seus perfis não implementadas. Provisória. |
| D-017 | Aceitar medidaEm entre início e recebimento, inclusive atrasada; rejeitar futura, anterior ao início, dispositivo vazio ou decimal incompatível com numeric(10,3). | Tolerância de relógio do dispositivo e janela de atraso configurável; exigem decisão de contrato. | Sem arredondamento na classificação; precisão de três casas não limita as faixas a valores demo. Provisória. |
| D-018 | Lock PESSIMISTIC_WRITE no transporte para todas as operações do serviço e índice único parcial PostgreSQL para ocorrência ABERTA. | Versionamento otimista com retry; exige implementação e novos testes. | Evita corrida leitura/verificação e duplicação por instâncias da aplicação. H2 testa lock; índice parcial requer PostgreSQL real. Provisória. |
| D-019 | Normalização não encerra ocorrência térmica; encerramento interno explícito exige justificativa e não libera lote. Retomada encerra somente telemetria. | Encerrar térmica automaticamente mantendo lote bloqueado; mudar serviço/testes após decisão. | Preserva conferência humana. Autor SISTEMA nesta simulação, sem usuário/autenticação/decisão técnica. Provisória. |
| D-020 | Auditoria: entidade imutável no Hibernate, colunas não atualizáveis, sem setters/serviço de exclusão. | Trigger/permissões PostgreSQL para impedir alteração SQL por usuários privilegiados; migration e infraestrutura futuras. | JpaRepository tem métodos genéricos de exclusão, que não são expostos por endpoints/serviços; não há promessa de proteção contra SQL direto. Provisória. |
| D-021 | Sem seed nesta J2; fixtures criam dados fictícios nos testes. | Migration exclusiva de seed demo após alinhamento. | Preserva domínio e não presume campos ainda não definidos. Provisória. |
| D-022 | blogBackend consultado no commit 2f3f5aaba185267023f4aace702fd2a4c6be81cf. Manter arquitetura existente e Boot 4.1.2. | Adotar CRUD genérico, QueryDSL ou cascades do exemplo só com necessidade posterior demonstrada. | Uso didático de JPA/UUID/validação/repos/services/testes; nenhuma classe copiada integralmente, dependência nova ou versão alterada. Confirmada pelo escopo J2. |

### Pendências e testes fora desta etapa

- PostgreSQL/Flyway V0+V1, índice parcial, checks e startup dev: validação operacional ainda pendente. O perfil test continua H2/create-drop/Flyway desabilitado. Não tratar H2 como evidência da migration. D-013 permanece aberta.
- Recebimento, liberação/descarte por gestor, aplicação rejeitada, confirmação de disponibilidade e acesso por unidade/papel ainda não implementados. A restrição de liberação pela perda de telemetria será verificada no fluxo futuro de decisão/conferência; a J2 não oferece liberação.
- Não executados: endpoints HTTP, JWT, Windows Wrapper, integração Arduino/Azure e agendamento (fora de escopo). Sem push e sem avanço à J3.
- docs/ESPECIFICACAO.md preservada integralmente; as decisões acima documentam escolhas de implementação sem substituir requisitos funcionais.

## Regressão histórica da J2 — 08/11/2026

| Rodada | Testes | Falhas | Erros | Ignorados | Resultado | Duração Maven | Conclusão (America/Sao_Paulo) |
|---|---|---|---|---|---|---|---|
| 1 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 14.871 s | 2026-11-08T00:56:43-03:00 |
| 2 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 11.321 s | 2026-11-08T00:59:35-03:00 |
| 3 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 11.353 s | 2026-11-08T00:59:47-03:00 |

Cada rodada executou `./mvnw -B test` com JAVA_HOME/PATH do JDK 21 temporário. Execução preliminar: 43 testes aprovados. Relatórios Surefire locais; PostgreSQL/Flyway e testes de funcionalidades futuras permanecem pendentes conforme D-013 e lista acima.

## Decisões e ambiguidades da J3 — 08/11/2026

Pedido J3 autoriza a etapa e commits locais, preservando master e sem push. HEAD inicial confirmado 0fc7717, árvore limpa. A etapa foi executada exclusivamente na raiz do clone VaciSmart confirmado, com preservação das demais cópias e do histórico.

| ID | Decisão adotada | Alternativa / reversão | Impacto e situação |
|---|---|---|---|
| D-023 | Gravidade explícita NORMAL=0, ATENCAO=10, FORA_DA_FAIXA=20; maisGraveQue substitui ordinal. | Outra escala explícita; preservar ordem de gravidade e testes. | Confirmada pelo pedido; parâmetros térmicos inalterados; 12 casos adicionais. |
| D-024 | Rotas complementares /vacinas, CRUD por ID, POST /transportes/{id}/monitoramento e GET de leituras/ocorrências/historico. Mantidas /unidades, /lotes e POST /transportes/{id}/leituras da especificação. | Alinhar nomes complementares com grupo antes de consumo externo; ajustar Controllers/testes/documentação sem editar silenciosamente a especificação. | Especificação não detalha os contratos complementares. Provisória para revisão; nenhuma rota definida foi renomeada. |
| D-025 | transporteId obrigatório no DTO de leitura como no exemplo da especificação; deve coincidir com a rota. | Remover redundância somente após alinhamento de contrato; alterar DTO/testes/documentação. | Divergência ou ausência retorna 400; UUID e datas com offset. Provisória. |
| D-026 | Faixas congeladas para toda vacina com lotes, mesmo antes do monitoramento. Nome atualizável; parâmetros numericamente iguais permitidos. | Snapshot de parâmetros por lote ou versionamento, exigindo migration/novo desenho; revisão futura explícita. | Evita reinterpretação histórica. Margem informada explicitamente, demo=1; numeric(10,3) mantém precisão J2 (5.2000 aceito). Provisória conservadora. |
| D-027 | Transporte editável somente CADASTRADO. Lote: só código, EM_MONITORAMENTO e transporte CADASTRADO; vacina/transporte imutáveis; nenhum DELETE de lote. Cadastro de lote só antes do início. | Fluxo de retificação/remoção com auditoria e revisão de ciclo; fora da J3. | Impede inserção tardia de lote e mudanças em lotes bloqueados. Provisória conservadora. |
| D-028 | Bind 127.0.0.1, sem autenticação provisória, JWT, CORS irrestrito ou papéis simulados. | J4 deve implementar segurança antes de qualquer publicação; rever bind somente nessa etapa autorizada. | Somente testes locais; roles da especificação ainda não protegidas. Confirmada pelo escopo J3. |
| D-029 | PaginaDto próprio com conteudo/pagina/tamanho/totalElementos/totalPaginas; Pageable padrão20/máximo100, sort permitido por recurso e id como desempate. Filtros com AND. | Contrato Page Spring/Data ou validação estrita de page/size; mudar DTO/resolver e testes mediante alinhamento. | Resolver Spring normaliza parâmetros de paginação inválidos; sort de campo interno retorna400. Sem paginação em memória. Provisória. |
| D-030 | JPQL de ocorrências por transporte/estado; Specifications para lotes e transportes; derivadas paginadas para leituras/auditoria e exists para dependências. | QBE atende filtros simples, mas Specification facilita composição; substituir sem alterar contrato HTTP. | Consultas com utilidade operacional; sem QueryDSL ou nova dependência. Confirmada pelo pedido. |
| D-031 | Histórico por entidade/referência em /lotes/{id}/historico e /transportes/{id}/historico; transporte não agrega eventos dos filhos. Ocorrências somente consultadas. | Timeline agregada futura com filtros/paginação próprios e escopo de acesso da J4. | Sem CRUD de eventos ou endpoint de encerramento/liberação/descarte. Provisória. |
| D-032 | Locks de unidade/vacina em referências e exclusões; mesmo lock de transporte da J2 nas mutações de transporte/lote. FKs e constraints reforçam proteção; conflitos de integridade/concorrência retornam409 seguro. | Versionamento otimista/retry exige análise adicional; manter invariantes/testes ao reverter. | Não altera migrations. Concorrência de cadastro em PostgreSQL não testada; SQL direto fica fora da proteção do Service. Provisória. |
| D-033 | Testes HTTP com servidor real RANDOM_PORT, HttpClient JDK, H2 separado e Clock fixo; Advice ProblemDetail. | MockMvc com módulo de testes adequado ao Boot4; exigiria dependência adicional desnecessária aqui. | 32 casos HTTP reais; 45 da J2 preservados; sem testes superficiais substituindo domínio. Confirmada pela execução. |

Referência didática: blogBackend, mesmo commit 2f3f5aaba185267023f4aace702fd2a4c6be81cf. Consultados CommentResource, AbstractCrudResource, CommentDTO, CommentService, CommentRepository e GlobalExceptionHandler. Aproveitados records/validação, ResponseEntity/Location, Pageable e Advice, adaptados ao VaciSmart sem copiar classes. Boot4.1.2 e pom preservados; sem Swagger/QueryDSL/Security do exemplo. Resposta500 não expõe a mensagem interna usada pelo exemplo.

Nenhuma falha de implementação/teste foi encontrada nas execuções Maven da J3. Não é evidência de ausência de defeitos; resultados completos abaixo.

Recursos deliberadamente não implementados no marco J3: autenticação/JWT, autorização por unidade/papel, Swagger/OpenAPI, cidadão, recebimento, decisão técnica, confirmação de disponibilidade, aplicações, liberação/descarte, seed, agendador, Arduino/Mobile/Azure. Naquele marco não se avançou à J4 nem realizou push, e a revisão externa era pendente; autenticação/autorização e cadastro de cidadão foram entregues na J4, aprovada em 12/11. Especificação e AGENTS preservados.

Pendências: PostgreSQL real, Flyway V0/V1, índice único parcial/checks e startup dev continuam NÃO TESTADOS nesta etapa; D-013 segue aberta. Três regressões H2 não encerram essa pendência. Novos locks cadastros foram implementados e exercitados sequencialmente via HTTP; não houve stress de concorrência de cadastro nem teste desses locks no PostgreSQL real. Os testes concorrentes originais de monitoramento continuam passando.

### Regressões finais da J3

| Rodada | Testes | Falhas | Erros | Ignorados | Resultado | Duração Maven | Conclusão (America/Sao_Paulo) |
|---|---|---|---|---|---|---|---|
| 1 | 89 | 0 | 0 | 0 | BUILD SUCCESS | 17.187 s | 2026-11-08T22:47:57-03:00 |
| 2 | 89 | 0 | 0 | 0 | BUILD SUCCESS | 16.076 s | 2026-11-08T22:48:15-03:00 |
| 3 | 89 | 0 | 0 | 0 | BUILD SUCCESS | 16.016 s | 2026-11-08T22:48:33-03:00 |

Compilação/empacotamento `./mvnw -B -DskipTests package`: BUILD SUCCESS após as regressões, com JDK Temurin21.0.13 temporário e Maven3.9.11. Logs reais e limitações no README. Relatório A–L exclusivamente local, ignorado e fora do índice.

## D-034 — Correção de mapeamento JPA/PostgreSQL — 09/11/2026

Causa confirmada: Boot4.1.2 configura PhysicalNamingStrategySnakeCaseImpl/SpringImplicitNamingStrategy na ausência de override. A estratégia física não separa o C final isolado; temperaturaC virava temperaturac, temperaturaMinC virava temperatura_minc, temperaturaMaxC virava temperatura_maxc e margemAtencaoC virava margem_atencaoc. A V1 já usa respectivamente temperatura_c, temperatura_min_c, temperatura_max_c e margem_atencao_c. Cinco entidades restantes, nomes de tabela, IDs herdados e FKs revisados sem divergências de nomes (38 colunas no total).

Decisão confirmada pelo objetivo: quatro nomes explícitos em @Column nas duas entidades afetadas; preservar nomes Java/JSON, regras, precisão/escala, dependências, perfis e migrations. Alternativa descartada: estratégia global customizada, que ampliaria o alcance para todas as entidades. Reversão: retirar anotações somente se uma estratégia equivalente for previamente validada contra a V1 e o banco real, sem editar migrations aplicadas.

H2/create-drop reproduzia o mesmo erro de nomenclatura no esquema gerado, ocultando a divergência com a migration. Adicionado MapeamentoSchemaTest usando metadados efetivos Hibernate e o recurso V1: falhou antes da correção nas duas entidades e passou depois em todas as sete. Suíte final96/0/0/0, BUILD SUCCESS (22.018s, 2026-11-09T00:01:01-03:00). Os89 testes anteriores foram preservados integralmente. O teste cobre nomes de tabela/colunas; não comprova constraints/índices nem substitui PostgreSQL.

Atualização da pendência D-013: nesta execução o Docker Desktop/contexto desktop-linux e o PostgreSQL17.6 existente estavam acessíveis mediante autorização do ambiente. Consultas somente leitura confirmaram container saudável, versão e histórico V0/V1 success=true. Startup dev real confirmado às2026-11-09T00:03:16-03:00, porta local temporária41293: Flyway validou2 migrations, public versão1, nenhuma migration necessária; Hibernate concluiu validação com ddl-auto=validate e a aplicação iniciou em5.558s. Processo de API criado para teste foi encerrado; sem exclusão/recriação do banco, migração reaplicada, mudança de dados de negócio ou container interrompido. A aplicação inicial das migrations ocorreu anteriormente, conforme histórico consultado; nesta correção validou-se o esquema existente.

D-013 fica resolvida para acesso ao banco, validação Flyway/esquema existente e startup dev. Testes funcionais/concorrência no PostgreSQL real, comportamento dos checks/índice parcial sob carga, segurança J4 e demais etapas continuam pendentes; não foram executados nesta correção. Documentação anterior registra limitações históricas de J1/J2/J3 e deve ser lida junto desta atualização. Sem push, sem J4, sem mudanças de escopo. Relatório A–L local atualizado e fora do índice.


## Decisões da J4 — 10/11/2026

| ID | Decisão adotada | Alternativa / reversão | Impacto e situação |
|---|---|---|---|
| D-035 | Resource Server e NimbusJwtEncoder oficiais, RS256, par RSA externo, emissor configurado, validade padrão15min e máxima1h. Sem chave padrão/refresh token. | JWKS/rotação com múltiplas chaves ou provedor externo exigem desenho próprio futuro. | Implementado; tokens sem assinatura, expirados, sem claims ou de outro emissor negados. Sem dependência JWT do exemplo do professor. |
| D-036 | Perfil e ativo consultados no banco em toda requisição; AcessoService usa identidade e vínculo persistidos dentro dos serviços, sem confiar no perfil/unidade informados pelo cliente. | Claims isoladas teriam janela de privilégios antigos; não adotadas. Manter a consulta ao estado persistido. | Implementado e testado: usuário desativado, perfil e unidade alterados perdem acesso na requisição seguinte com token anterior. Operação já em execução não é cancelada. |
| D-037 | GETs de unidades restritos à própria unidade para funcionário; vacinas são catálogo operacional comum. Globais unidade/vacina e POST /lotes exclusivos de GESTOR; transporte e atualização de código de lote na própria unidade permitidos a funcionário. | Tornar cadastros de transporte também administrativos mediante alinhamento/testes. | Conservadora para globais; POST /lotes segue a especificação expressa. Cidadão só cadastro/login/própria conta. |
| D-038 | Funcionário recebe403 tanto para UUID de transporte/lote alheio quanto inexistente; consulta filha inexistente sob pai autorizado retorna404. Gestor recebe404 se recurso não existe. | Usar404 uniforme para alheios/inexistentes também é viável, mas mudaria os testes/contrato desta etapa. | Impede distinguir existência de recurso fora do escopo. Filtros explícitos de outra unidade também403. Nenhuma resposta revela unidade/dados alheios. |
| D-039 | Specifications aplicam unidade antes da paginação/contagem. Leituras/ocorrências/auditoria exigem acesso ao pai. Verificação de unidade repetida sob lock de transporte nas mutações. | Consultas dedicadas com unidade em todos os repositories exigem refatoração sem mudança funcional. | Preserva paginação SQL e locks J2/J3. Sem filtragem posterior em memória. |
| D-040 | Provisionamento opt-in de duas unidades e quatro usuários apenas em dev exclusivo, flag e confirmação explícitas; credenciais externas sem defaults, nunca sobrescritas. | Comando administrativo offline/migration seed exclusiva exigiriam nova autorização. | Pendente em 10/11; **validado e encerrado no escopo local em 12/11**: quatro usuários/duas unidades novos no PostgreSQL 17.6 pelo runner existente, bootstrap desabilitado no reinício e registros preexistentes preservados. Não ativar em produção. |
| D-041 | CORS recebe origens exatas externas, vazio por padrão, sem wildcard/cookies; Authorization permitido. Senhas públicas12–72 ASCII imprimíveis por limite de bytes do BCrypt12. | PasswordEncoder alternativo ou validação customizada por bytes permitiria Unicode; exigiria política/migração compatível. | Documentado no README e validado em DTOs; CORS não substitui JWT. |
| D-042 | Arduino/gateway precisa autenticação própria, revogável e vinculada ao dispositivo/transporte autorizado. Ingestão atual permanece simulação autenticada de funcionário/gestor, restrita por unidade. | Futuro client credentials/certificados/chaves por dispositivo exige desenho e testes próprios; não reutilizar credencial pessoal. | PENDENTE futura etapa; não criar quarto perfil humano nem liberar POST /leituras publicamente. Especificação preservada. |
| D-043 | Auditoria automática térmica continua SISTEMA, como na J2; escopo e leitura protegidos. Não há recebimento/decisão técnica nesta etapa. | Autoria humana em futuros comandos explícitos deverá usar UUID autenticado e ser auditada. | Regras antigas preservadas; não confundir simulação de ingestão com identidade autenticada de hardware. |
| D-044 | Em 10/11, os sockets Docker default/desktop-linux estavam ausentes; V2 e startup dev ficaram NÃO TESTADOS naquela execução. | Acesso obtido na validação posterior de 12/11 com chaves externas, preservando banco/volumes. | **Impedimento resolvido em 12/11**: V0/V1/V2 já aplicadas e bem-sucedidas, dois startups validando as três migrations sem nova aplicação, Hibernate validate e fluxo térmico J4 aprovados no PostgreSQL 17.6. H2 não é evidência desses resultados; aplicação inédita de V2 e testes adversos de todos os checks/FKs não foram comprovados. |

Referência didática revisitada: blogBackend no commit2f3f5aaba185267023f4aace702fd2a4c6be81cf, incluindo SecurityConfiguration, JwtHelper, AuthFilter e AuthControllerIntegrationTest. Aproveitados conceitos de SecurityFilterChain, stateless, BCrypt, autenticação e testes positivos/negativos. Classes próprias, sem cópia integral: aqui não há JJWT, chave fixa, filtro de parsing caseiro, refresh, console ou Swagger público. Boot4.1.2, Java21, JPA e contratos J3 preservados. AGENTS e docs/ESPECIFICACAO.md não alterados. Sem push ou execução da próxima etapa.


### D-045 — Revalidação após transferência concorrente

Decisão: CadastroService e MonitoramentoService recarregam o transporte com `EntityManager.refresh(..., PESSIMISTIC_WRITE)` após adquirir o lock e antes de conferir novamente a unidade. Somente repetir find/query pode reaproveitar uma instância já carregada no contexto JPA com unidade antiga. Teste de integração usa duas transações: funcionário A carrega a entidade, gestor transfere para B, funcionário A tenta alterá-la e é negado; a transferência do gestor permanece intacta. Alternativa: centralizar uma consulta autorizada com recarregamento explícito numa camada própria; reversão exige preservar a mesma garantia e o teste. Mais uma consulta por mutação é o custo conservador adotado. Teste isolado aprovado; resultados completos finais seguem abaixo. Comportamento de locks no PostgreSQL real continua NÃO TESTADO.


Validação automatizada histórica da J4 em 10/11/2026: três execuções independentes de `./mvnw -B clean test`, todas180 testes, zero falhas/erros/ignorados e BUILD SUCCESS; durações 58.924 s, 59.176 s, 59.327 s. Empacotamento `./mvnw -B -DskipTests package` aprovado em 2.549 s. Java/Javac21.0.13, Maven3.9.11. Resultados detalhados e horários no README. V2/startup dev não foram testados naquela sessão (D-044), cuja limitação foi resolvida no escopo da atualização de 12/11 abaixo.

## Atualização de encerramento da J4 — 12/11/2026

**J4 concluída e oficialmente aprovada pelo responsável**, após a validação operacional final. As evidências abaixo vêm das atividades anteriores de 12/11 e da auditoria preventiva, sem nova execução de testes, API ou banco nesta edição documental.

| Item | Situação comprovada / limite |
|---|---|
| D-035 a D-039, D-041 e D-043 | Implementação J4 aceita; cadastro público de cidadão validado na atividade anterior; login RS256, BCrypt, perfis, isolamento A/B, proteção operacional, auditoria/histórico e paginação conferidos. Não se atribui às 109 verificações cobertura integral de todo cenário automatizado. |
| D-040 | Validação operacional local encerrada: cidadão, funcionários A/B e gestor, quatro contas de três perfis humanos, duas unidades e bootstrap desabilitado no reinício. |
| D-044 | Acesso/startup e V0–V2 no esquema existente validados no PostgreSQL 17.6, com Java 21/Boot 4.1.2; preservada a limitação histórica de 10/11. |
| Testes automatizados | 180 aprovados, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESS em 12/11 às 00:03:56 (57.854 s, H2). Na auditoria preventiva posterior, mesma contagem e BUILD SUCCESS em execução isolada do mesmo código (56.235 s, H2). Não são execuções desta edição. |
| Verificações operacionais | 109 aprovadas e 0 falhas no escopo solicitado sobre PostgreSQL: login/JWT/própria conta, perfis, isolamento, fluxo normal → atenção → fora → normal com bloqueio/auditoria sem liberação, JPQL sem/com estado e paginação/contagem, preservação dos três registros preexistentes. São verificações HTTP/criptográficas/estado/integridade, não 109 testes JUnit. |
| D-002 e D-042 | Permanecem pendentes: alinhamento bilateral Mobile/contrato da equipe e autenticação própria de dispositivos. O [contrato interno para Gemini](docs/INTEGRACAO_MOBILE_GEMINI.md), reconstruído do backend na consolidação posterior, não comprova equivalência ou presença de 02_MODELO_E_CONTRATOS.md no Git nem validação conjunta do aplicativo. |
| D-045 e limites PostgreSQL | Regressão de transferência concorrente validada em H2; carga/concorrência e testes adversos de locks/checks/índice parcial no PostgreSQL continuam não comprovados. A aplicação inédita de V2 não foi testada na validação final, pois já estava aplicada. |

O aceite da etapa não encerra por inferência decisões abertas de integração, escolhas provisórias antigas ou funcionalidades futuras. Recebimento, decisão técnica, liberação/descarte, disponibilidade informativa e caderneta digital não foram implementados. H2 não substitui PostgreSQL; demonstração local não é validação de produção, hardware ou implantação. Não houve mudança de contrato HTTP.

### D-046 — Decisão definitiva de escopo — 12/11/2026

**CONFIRMADA pelo responsável:** Spring Cache, Spring HATEOAS e Spring Boot Actuator não serão implementados. D-007 fica superada; os três recursos estão removidos das funcionalidades futuras, objetivos de J5/J6/J7, checklists e recomendações vigentes. OpenAPI/Swagger permanece previsto, sem implementação nesta atividade. J5/J6/J7 e DevOps não estão concluídos nem autorizados automaticamente.

A orientação antiga em AGENTS.md foi preservada na edição documental anterior, que não autorizava sua alteração. Na consolidação posterior de 12/11, o responsável autorizou o alinhamento pontual: os três recursos foram excluídos dos próximos prompts, de J5/J6/J7 e dos critérios internos de aceite. As demais regras de governança foram preservadas; a decisão D-046 não foi reaberta.

Continuam fora do MVP GPS/rastreamento de veículos, rotas/localização de transporte, agendamento/reserva de vacinas, integrações SUS/RNDS, notificações externas, QR Code, IA e funcionalidades clínicas reais. Publicação/remoto, commits e avanço de etapa continuam dependendo de autorização explícita própria.


### D-047 — Consolidação preventiva e Maven Wrapper — 12/11/2026

Consolidação autorizada sem implementação nova: AGENTS alinhado à D-046; contrato interno Mobile/Gemini reconstruído dos 29 pares método/rota e DTOs reais, sem encerrar D-002; bind de publicação PostgreSQL limitado a 127.0.0.1:5432:5432; ignore ampliado para nomes privados específicos, dumps/backups e .pfx, sem ignorar migrations .sql. Nenhum container foi recriado ou banco/volume alterado. Nome da equipe, integrantes completos/RMs/turma, pitch e links de entrega continuam pendentes; Giovanni foi confirmado pelo responsável para Java Advanced/DevOps.

Wrapper 3.3.4 only-script, Maven **3.9.11**, URL original preservada: [ZIP exato no Maven Central](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip). O ZIP foi baixado por HTTPS e seu SHA-512 comparado integralmente ao [checksum oficial do mesmo artefato](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip.sha512), com correspondência. Só então foi calculado localmente o SHA-256 desse ZIP validado e adicionado como distributionSha256Sum: `0d7125e8c91097b36edb990ea5934e6c68b4440eef4ea96510a0f6815e7eeadb`. Trata-se de SHA-256 derivado do arquivo autenticado pelo SHA-512 oficial, não de um valor de outra versão ou de tar.gz. O Wrapper confere esse checksum ao baixar a distribuição; instalações já em cache podem seguir diretamente para execução. Nenhuma dependência/versão/URL foi atualizada.

A origem oficial, os algoritmos e o método acima permitem reproduzir a verificação de integridade da distribuição. O limite do bind só passa a valer para containers criados/reconfigurados com o Compose atualizado; esta atividade não altera o serviço em execução.


## Decisões da J5-A — 13/11/2026

### D-048 — Recebimento humano e atomicidade

Implementado somente recebimento (RN09/RN10/RN16/RN21): FluxoOperacionalService transacional reutiliza o método de lock de MonitoramentoService, agora interno ao pacote, com refresh pessimista e nova validação de unidade da J4. Mantém estado, lotes e eventos na mesma transação. Alternativa: duplicar o lock ou criar uma nova camada compartilhada; descartadas nesta etapa por aumentarem código sem necessidade. Reversão futura deve preservar serialização e teste de contexto antigo após transferência. GESTOR mantém acesso a todas as unidades do MVP (RN07); não se inventou vínculo de unidade para esse perfil.

### D-049 — Telemetria na fronteira do recebimento

O recebimento verifica a RN05 antes de mudar o transporte para RECEBIDO, sob o mesmo lock e usando o mesmo instante de Clock dos eventos humanos. Extraída uma sobrecarga interna da regra existente, sem duplicar duração, consulta de última recepção ou abertura de ocorrência. Mais que o limite configurado abre perda com autor SISTEMA; igualdade não abre. Ocorrências abertas não são duplicadas nem encerradas. Alternativa de não verificar perderia o silêncio vencido, pois a verificação posterior ignora transportes recebidos. Sem agendador, liberação ou bloqueio por telemetria.

### D-050 — Autoria e contrato de recebimento

POST sem DTO/corpo, com UUID da rota; identidade humana vem de AcessoService.atual e horário de Clock, sem novas colunas. Cada transição efetiva recebe um evento imutável; lote já bloqueado não recebe evento redundante de conferência. Abertura automática de ocorrência preserva SISTEMA. Alternativa de novas colunas autor/recebidoEm exigiria migration e duplicaria informação já auditada. Especificação e contratos J4 preservados; apenas nova rota interna, vedada ao cidadão. O adendo Mobile não encerra D-002.

Workspace localizado em /home/gio/Documentos/Global Solution/VaciSmart, remoto oficial, master/16d0bab0e7e11462a52d780402a1ce5c1b1a97fa e árvore inicial limpa, com autorização explícita para essa cópia. Sem commits/push. J5-A em revisão; nenhuma outra parte de J5 ou etapa posterior foi implementada. Testes atuais e limitações constam no relatório privado; registros J1–J4 acima são históricos.


## Decisões da J5-B — 13/11/2026

A J5-A foi aprovada pelo responsável após revisão no ChatGPT; commit local 08fdb7e5a2ec763fd539d9a3261a6dede96e5c9d, sem push. Seus 192 testes são evidência histórica. A revisão/aprovação do planejamento autorizou exclusivamente a implementação J5-B; PostgreSQL e três regressões independentes continuam reservados à consolidação J5-E. As referências anteriores a decisão técnica planejada são registros dos marcos J1–J5-A.

### D-051 — Contrato e estados terminais da decisão humana

CONFIRMADA pelo pedido: enum não persistido DecisaoTecnica com LIBERAR/DESCARTAR e record de entrada decisao/justificativa. POST /api/v1/lotes/{id}/decisao-tecnica retorna 204 sem corpo. Somente GESTOR ativo; autorização no serviço antes de consultar existência. Justificativa obrigatória/não branca/até 1000, reforçada no domínio. Apenas AGUARDANDO_CONFERENCIA/BLOQUEADO_PREVENTIVAMENTE em transporte RECEBIDO podem ir a LIBERADO/DESCARTADO; ambos destinos terminais nesta entrega e repetição 409. Alternativa de reutilizar EstadoLote como comando exporia destinos inelegíveis; enum próprio expressa os dois comandos aprovados. Revisão de um lote já LIBERADO exigiria novo escopo autorizado, não um setter genérico.

### D-052 — Refresh, auditoria e preservação do histórico

FluxoOperacionalService reutiliza buscarComBloqueio do monitoramento (lock pessimista/refresh do transporte/revalidação de unidade) e executa EntityManager.refresh(lote) depois desse lock. Uma segunda findById pode retornar estado antigo e sobrescrever uma decisão concorrente. Sem lock independente do lote, mantendo ordem e serialização pelo transporte. Estado e um evento imutável LIBERADO/DESCARTADO são persistidos na mesma transação, com UUID de AcessoService.atual, Clock e justificativa informada integralmente. Autor/data ficam no evento existente; nenhuma alteração de esquema necessária. Alternativas de lock adicional ou colunas de decisão duplicariam infraestrutura/informação sem necessidade nesta entrega.

Ocorrência térmica ou perda de telemetria aberta não é veto absoluto: a conferência humana explicitamente justificada satisfaz o fluxo autorizado, sem declarar segurança clínica. Não encerrar ocorrências, liberar por normalização/tempo nem confirmar disponibilidade; manter NAO_DISPONIVEL e preservar outros lotes/histórico. Reverter/refatorar deve preservar testes de rollback, decisões concorrentes e cache JPA antigo. Segurança JWT, filtros, Advice, repositories e migrations da J4/J5-A não mudaram. Integração Mobile bilateral (D-002) permanece pendente; apenas adendo do endpoint interno.

### D-053 — Referências de OpenAPI entre etapas

AGENTS/README mantêm a associação histórica de OpenAPI à J5; o recorte atual J5-A–J5-E não autoriza sua implementação e menciona J6 como etapa fora de escopo. Registrar a distribuição J5/J6 como ponto de planejamento a reconciliar pelo responsável, sem inferir mudança de etapa, reescrever governança ou implementar Swagger agora. OpenAPI permanece pendente; Cache/HATEOAS/Actuator continuam definitivamente excluídos (D-046).


Compatibilidade dos testes históricos: a primeira regressão J5-B encontrou em ApiIntegrationTest uma asserção do marco J3 que exigia 404 para /decisao-tecnica. Com a rota autorizada implementada, o corpo vazio desse teste passou corretamente a 400. Removida somente essa asserção de inexistência; preservados o teste de 500 seguro e as rotas realmente futuras. O contrato novo tem testes próprios de 400/404. Resultados reais de ambas as regressões registrados no relatório privado; não alterar a API para manter uma expectativa histórica superada.


## Decisões da J5-C — 13/11/2026

J5-B aprovada e commitada localmente em 5a054cab87898e27e90206164a03513082687186; 214 testes aprovados são evidência histórica. Planejamento J5-C aprovado pelo responsável e implementação restrita à disponibilidade interna. J5-D/J5-E/J6/DevOps não autorizados nesta entrega; três regressões independentes e PostgreSQL continuam para J5-E.

### D-054 — Disponibilidade reversível e idempotência validada

CONFIRMADA pelo pedido: POST /api/v1/lotes/{id}/disponibilidade, record DisponibilidadeEntradaDto com @NotNull DisponibilidadeLote, reutilizando CONFIRMADA/NAO_DISPONIVEL. 204 sem corpo para alteração ou repetição válida. Lote.alterarDisponibilidade valida destino, transporte RECEBIDO e estado LIBERADO ANTES de comparar o valor atual. Retorna false no no-op, sem evento/novo horário; true só altera disponibilidade. Campo JSON extra continua 400. Não exigir justificativa humana. Alternativa 409 para repetição foi avaliada no planejamento e descartada na aprovação desta operação de estado desejado; J5-B mantém terminalidade/409 de suas decisões. Um reenvio após alteração oposta interveniente constitui nova mudança efetiva, não deduplicação global de comandos.

### D-055 — Autorização, transação e auditoria de disponibilidade

AcessoService.lote aplica perfil e unidade, preservando D-038 (funcionário alheio/inexistente 403; gestor inexistente 404). Serviço reutiliza buscarComBloqueio, que recarrega transporte sob PESSIMISTIC_WRITE e revalida unidade; refresh explícito do lote só depois do lock. Domínio e evento na mesma transação, sem lock paralelo. Somente mudanças geram DISPONIBILIDADE_CONFIRMADA/DISPONIBILIDADE_RETIRADA com entidade Lote, UUID autenticado, Clock e descrição técnica fixa. Sem alterar estado LIBERADO, ocorrências, histórico ou outros lotes. Eventos poderão fundamentar atualizadoEm em J5-D; sem coluna/data inventada agora. Tabelas/enums/constraints já comportam os valores; não há necessidade de migration, repository novo ou dependência. Testes de rollback e concorrência/refresh verificam as invariantes em H2; PostgreSQL permanece reservado à J5-E.

### D-056 — Permissão do gestor e limites

RN11/RN22 descrevem FUNCIONARIO da unidade, enquanto o endpoint da própria especificação inclui FUNCIONARIO e GESTOR; RN07 e a segurança vigente dão escopo global ao gestor no MVP. O pedido J5-C confirma explicitamente ambos: funcionário ativo da unidade responsável ou gestor ativo global. Preservar essa interpretação sem editar a especificação, criar vínculo de unidade para gestor ou confiar em claims/clientes. Retirar disponibilidade não é descarte/revisão da decisão técnica, não autoriza aplicação real nem oferece reserva ou quantidade garantida. D-002 Mobile e D-053 OpenAPI continuam pendentes em seus escopos próprios.


## Decisões da J5-D — 13/11/2026

J5-C aprovada e commitada localmente em 87f462b839ce35a92838c683a02a2915136f4a67. Seus 234 testes aprovados são históricos. Planejamento J5-D aprovado para implementação exclusiva da consulta cidadã; J5-E/J6/DevOps/J7 permanecem fora desta entrega.

### D-057 — Catálogos públicos e ausência de informação

CONFIRMADO pelo pedido: universo unidade × vacina, com ids/nomes públicos e filtros/paginação SQL, sem derivar a presença de linhas de lotes ou transportes internos. Inspeção das entidades, especificação e dados fictícios de exemplo confirmou a projeção adequada: UUID/nome de UnidadeSaude e Vacina; parâmetros térmicos e dados operacionais ficam excluídos. O modelo não possui marca de sigilo/oferta no catálogo; esses nomes são destinados à identificação pública no protótipo, não a anotações internas ou dados pessoais. A linha não afirma oferta, estoque, transporte ou recebimento. Alternativa de pares baseados em lotes revelaria associações não publicadas; apenas histórico publicado omitiria pares sem informação e não atende ao universo aprovado. Alterar esse universo exigirá revisar contrato/contagem, sem introduzir catálogo de oferta agora.

### D-058 — Situação e horário independentes

Enum não persistido SituacaoDisponibilidade: CONFIRMADA se existe LIBERADO/CONFIRMADA/RECEBIDO; CONFIRMACAO_RETIRADA somente sem confirmação válida e com retirada explícita, sem flag CONFIRMADA residual inconsistente; demais casos SEM_INFORMACAO. Nenhuma situação negativa afirma falta real de estoque. Horário usa MAX de eventos entidade Lote e ações DISPONIBILIDADE_CONFIRMADA/RETIRADA, sobre todos os lotes do par. Inclui lote já retirado e ignora auditoria genérica, autor e justificativa. Sem evento pertinente retorna null, não data sintética. Última ação do grupo não determina estado: retirar um lote mantém a confirmação de outro. Alternativa de usar somente o último evento falharia nesse caso; ordenar UUID para desempatar horários também inventaria causalidade. Dados legados incoerentes não são reparados por GET; confirmação inelegível não é positiva e não prova retirada. Sem nova coluna/migration.

### D-059 — Consulta pública autenticada, agregação e paginação

Três perfis ativos acessam a mesma visão pública global, sem unidade operacional obrigatória; AcessoService.atual também no serviço. Matcher exclusivo HttpMethod.GET na rota exata, antes da regra operacional; demais rotas permanecem protegidas. DTO específico com referência pública aninhada evita expor VacinaSaidaDto técnico ou entidades. Serviço somente leitura; repository customizado via EntityManager/Hibernate já existentes, com CTE paginando pares antes de agregar estados e auditoria separadamente. Filtros UUID vinculados e whitelist constante de sort unidadeNome/vacinaNome; desempate unit/vac UUID ascendente. Helper Paginacao antigo preservado porque seu desempate id não se aplica ao agregado. Validação HTTP de sort fica antes do proxy @Repository: sua tradução de IllegalArgumentException produziria 500; serviço garante 400, mantendo whitelist defensiva no SQL. Sem alterações globais no Advice.

Contagem deriva do mesmo catálogo/filtros, sem contar lotes/eventos. Conteúdo/horário na mesma instrução SQL; contagem separada pode acompanhar alterações concorrentes normais de catálogo. Não introduzir locks de transporte ou isolamento especial. Produto dos catálogos tem custo crescente; não há teste de carga nem plano de execução PostgreSQL comprovado. Começar com índices existentes, sem migration preventiva. Compatibilidade operacional PostgreSQL e três regressões independentes para J5-E, sem alegá-las a partir de H2. Contrato Mobile atualizado para o endpoint implementado, sem presumir validação bilateral D-002 ou criar catálogos públicos adicionais.

## Consolidação documental da J5 — 15/11/2026

### D-060 — Validação J5-E e regressão única

Validação funcional realizada em 14/11/2026 e aprovada tecnicamente no escopo solicitado, no HEAD 8fea0bebd1dadf6c9197d12eddc64fb0bf2ef130. Java 21, PostgreSQL 17.6, Hibernate validate e Flyway V0/V1/V2 já aplicadas: três migrations validadas, nenhuma nova aplicação. As 57 verificações HTTP reais foram aprovadas, incluindo o fluxo J5-A–D, consulta cidadã 200 no PostgreSQL, três situações, filtros/paginação/ordenação, auditoria e proteção de dados. São verificações operacionais, não 57 testes JUnit novos.

As menções anteriores a três regressões independentes (inclusive D-059) preservam o planejamento histórico. Posteriormente, o responsável autorizou a validação funcional simplificada seguida de **uma única regressão completa**, com Java 21 e test/H2 isolado: 256 testes, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS. Essa execução ocorreu em 14/11/2026; não foi repetida nesta consolidação documental.

Os 22 registros preexistentes, backup, inventários e V0/V1/V2 foram preservados; acrescentados somente 29 registros fictícios exclusivos, incluindo 13 eventos de auditoria. A regressão H2 não alterou o PostgreSQL. Não houve implantação, integração Flutter/Arduino ou alteração de código, segurança, dependências e migrations.

**D-002 permanece ABERTA:** contrato e integração bilateral Mobile ainda dependem de comparação e validação conjunta. **D-053 permanece ABERTA:** AGENTS/README planejam OpenAPI para J5, mas o recorte J5-A–E executado não implementa esse recurso. Cabe ao responsável reconciliar sua etapa de entrega e autorizar o escopo antes de qualquer implementação; não há transferência automática para J6 nem mudança do roadmap. A especificação funcional não menciona OpenAPI/Swagger e não define biblioteca, versão ou interface obrigatória. Não há outro requisito acadêmico específico inferido de materiais indisponíveis.

Integrantes/RMs fornecidos pelo responsável foram inseridos no README. Giovanni permanece responsável por Java Advanced/DevOps; responsabilidades dos demais, turma e nome da equipe não foram atribuídos por inferência. J5-E aguarda encerramento documental; J5 integral continua incompleta pela pendência D-053. Cache/HATEOAS/Actuator seguem excluídos por D-046.


## Encerramento técnico D-053 — OpenAPI na J5 — 15/11/2026

**D-053: ENCERRADA tecnicamente, aguardando revisão no ChatGPT.** O pedido de implementação confirmou OpenAPI como pendência da J5 e informou a exigência acadêmica de Swagger/OpenAPI e endpoints funcionando na UI nas páginas 52–56 do material GS FIAP 2026/2. Essa informação do responsável resolve a dúvida de obrigatoriedade registrada no planejamento; o arquivo acadêmico original não está nesta cópia e não foram inferidos requisitos adicionais. Não houve transferência para J6 nem reabertura da exclusão expressa D-046, mesmo diante da presença dos recursos excluídos no material acadêmico.

Adotado `springdoc-openapi-starter-webmvc-ui:3.2.0`, sem alterar Boot 4.1.2/Java 21. Configuração OpenApiConfig, descrições nos sete controllers, anotações documentais nos DTOs e ParameterObject para parâmetros de paginação. As 33 combinações método/rota correspondem aos mappings existentes. ProblemDetail é representado por schema documental, com type/title/status/detail e instance/campos opcionais; 201/Location e 204 vazio preservados. As referências são verificadas automaticamente, sem entidades JPA como contratos. A normalização do tipo de mídia no documento usa application/json para os DTOs; não altera a negociação HTTP do backend.

Política aprovada aplicada: geração/UI desligadas por padrão; somente `(dev | test) & !prod & !production` habilita e libera GET de `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs.yaml` e `/v3/api-docs/swagger-config`. Fora dessa expressão os caminhos são negados; não se alteraram as permissões de negócio. `persist-authorization=false`, sem exemplos com segredos. A alternativa de documentar publicamente em qualquer ambiente foi descartada pela restrição do pedido; autenticar a página inicial com Bearer impediria obter a UI antes de informar o token. JWT continua obrigatório nas operações protegidas; a UI só documenta e envia o token fornecido localmente.

Validações efetivas: 17 testes direcionados aprovados; regressão final com 273/0/0/0 e BUILD SUCCESS, Java 21/H2 isolado. Browser real Chrome headless carregou as 33 operações sem erro JavaScript e executou cadastro 201, login 200, GET de própria conta e disponibilidade 200 com Bearer. Testes também verificam ausência de documentação em perfis não autorizados, inclusive test+prod e gestor autenticado, preservação das restrições de negócio, DTOs, validações, nulabilidade e ausência de dados sensíveis.

A primeira rodada revelou tipo de mídia genérico na documentação e schema ProblemDetail removido antes da inserção das referências; corrigidos somente no documento gerado. Ao ampliar o teste de referências, corrigida uma falha de sintaxe no novo teste. Na revisão final, acrescentada descrição de 404 para referências do corpo dos cadastros de transporte/lote. Histórico das rodadas, incluindo as anteriores à estabilização, preservado no relatório privado. Nenhuma falha de compatibilidade exigiu alterar Spring Boot.

Domínio, serviços, repositories, migrations, contratos de negócio e testes anteriores foram preservados. PostgreSQL, volumes, backups e evidências anteriores não foram manipulados. Sem clean, commits, push, J6 ou DevOps. Para eventual reversão autorizada, remover apenas a dependência/configuração/anotações/testes documentais e os matchers documentais, restaurando o comportamento anterior, sem tocar nas migrations ou regras de negócio.
