# Dúvidas e decisões — VaciSmart

Data de referência: 06/11/2026 (America/Sao_Paulo). ABERTA indica confirmação pendente, mesmo quando a opção provisória já foi adotada.

| ID | Data | Contexto | Dúvida ou decisão | Opção adotada | Alternativas | Impacto | Status |
|---|---|---|---|---|---|---|---|
| D-001 | 2026-11-06 | Versão do Spring Boot | Qual versão estável usar? | 4.1.2, mais recente estável indicada em spring.io em 06/11/2026, compatível com Java 21. | 3.5.16 ou 4.0.9; reverter alterando parent e adequando módulos/configuração. | Boot 4 usa Jackson 3 e módulos de autoconfiguração separados. | ABERTA |
| D-002 | 2026-11-06 | Contrato compartilhado ausente | 02_MODELO_E_CONTRATOS.md não está no repositório. | Adotar nomes de ESPECIFICACAO.md e /lotes/{id}/decisao-tecnica. | Alinhar arquivo do grupo; reverter por mudança de contrato documentada antes da implementação. | Alinhamento com Mobile e IoT pendente. | ABERTA |
| D-003 | 2026-11-06 | Banco de testes | H2 em modo PostgreSQL em vez de Testcontainers. | H2 em memória no perfil test. | Testcontainers; reverter substituindo datasource/dependência de teste. | Não comprova compatibilidade SQL com PostgreSQL. | ABERTA |
| D-004 | 2026-11-06 | Migrations e seed | Como versionar banco e dados fictícios? | Flyway com core e módulo PostgreSQL; V0 apenas comentário, sem seed na J1. | SQL manual; reverter removendo Flyway e definindo outro processo antes de J2. | Esquema e seed serão migrations futuras; não editar migrations já aplicadas. | ABERTA |
| D-005 | 2026-11-06 | Idioma | Como nomear domínio e classes técnicas? | Domínio/contrato em português e sufixos técnicos em inglês. | Tudo em português; reverter antes de publicar contrato, renomeando referências. | Contrato deve permanecer fiel à especificação. | ABERTA |
| D-006 | 2026-11-06 | Regras futuras ambíguas | Aceite de leituras e ciclo de ocorrências. | Só aceitar leitura EM_TRANSPORTE; no máximo uma ABERTA por tipo/transporte; encerrar perda de telemetria quando leituras voltarem. Nenhuma regra implementada na J1. | Rever com grupo antes de J2; alterar decisão e testes antes de implementar. | Exige confirmação antes das regras de negócio. | ABERTA |
| D-007 | 2026-11-06 | Conflito entre orientação anterior e requisitos oficiais | O pedido inicial informou dispensa de Cache, HATEOAS e Actuator; os materiais oficiais os exigem. O solicitante corrigiu a orientação nesta sessão. | Prever Spring Cache, Spring HATEOAS e Spring Actuator nas próximas etapas, sem implementação ou dependências na J1. | A dispensa anterior foi revogada; só revisar o planejamento mediante nova orientação explícita e atualizar documentos antes de implementar. | Corrigir proibição em AGENTS.md e manter J1 limitada ao bootstrap; não avançar à J2. | CONFIRMADA |
| D-008 | 2026-11-06 | Autoconfiguração Flyway no Boot 4 | core sozinho não ativa integração Spring no Boot 4. | Adicionar spring-boot-flyway, somente módulo técnico da integração já solicitada, além de flyway-core e flyway-database-postgresql. | Starter Flyway (traz o mesmo módulo); reverter trocando as declarações equivalentes. | Adaptação necessária à versão mais recente, sem recurso funcional adicional. | ABERTA |
| D-009 | 2026-11-06 | Perfil e ambiente local | Como impedir defaults locais fora de dev? | Sem perfil padrão; credenciais e defaults apenas em dev. .env é lido pelo Compose; exportar as mesmas variáveis para API. | Perfil dev padrão; reverter em application.yml e instruções. | Execução requer selecionar dev; testes selecionam test explicitamente. | ABERTA |
| D-010 | 2026-11-06 | Banco local | Qual imagem e persistência usar? | postgres:17.6, imagem oficial com versão fixa, volume nomeado e healthcheck; porta 5432. | Outra versão suportada; reverter imagem com migração/backup do volume, nunca apagar dados automaticamente. | Somente infraestrutura local; não é implantação final. | ABERTA |
| D-011 | 2026-11-06 | Fuso e Jackson 3 | Como aplicar fuso padrão sem criar classes nos pacotes reservados? | TimeZone no main; Surefire com user.timezone; Jackson timezone e datatype.datetime.write-dates-as-timestamps=false. | TZ/JVM externamente; reverter main/configuração/plugin em conjunto. | Datas com offset em ISO-8601; perfil test usa mesmo fuso. | ABERTA |

| D-012 | 2026-11-06 | Ferramentas Java locais | Runtime Java 21 instalado sem javac causou falha inicial: release version 21 not supported. | Com autorização explícita, usar Eclipse Temurin 21.0.13+9 somente em /tmp/vacismart-jdk21 e JAVA_HOME/PATH por comando; SHA-256 verificado na API oficial Adoptium. | JDK completo instalado pelo usuário; para reverter basta deixar de apontar para /tmp, sem mudança global a desfazer. | java e javac 21.0.13, Maven 3.9.11 conferidos antes dos testes; nenhum ajuste permanente no sistema. | CONFIRMADA |
| D-013 | 2026-11-06 | Docker e validação dev | Docker CLI presente, mas daemon indisponível; execução autorizada encontrou /var/run/docker.sock ausente. | Compose validado sintaticamente; registrar PostgreSQL, Flyway V0 e startup dev como não validados. | Disponibilizar daemon/contexto Docker e repetir up -d --wait, depois iniciar API com perfil dev; atualizar evidências para encerrar esta pendência. | Bloqueia validação real da infraestrutura local; não prejudicou teste H2. | ABERTA |
| D-014 | 2026-11-06 | Maven Wrapper | Qual versão e distribuição fixar? | Wrapper oficial 3.3.4 only-script com Maven 3.9.11; scripts Unix e Windows, sem JAR adicional. | Outro Maven compatível; reverter regenerando o wrapper com versão fixada e repetindo testes. | Primeiro uso precisa acesso ao Maven Central; testado em Linux. Script Windows não executado neste ambiente. | ABERTA |

## Fontes técnicas consultadas
- https://spring.io/projects/spring-boot/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://spring.io/blog/2025/10/28/modularizing-spring-boot/
- https://docs.spring.io/spring-boot/appendix/application-properties/index.html

## Validação
- `java -version`: Eclipse Temurin 21.0.13+9; `javac -version`: 21.0.13; `./mvnw -version`: Apache Maven 3.9.11 usando Temurin 21.0.13. Verificações anteriores à execução final de testes.
- JDK obtido de https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse ; checksum confirmado via https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse . SHA-256: `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`.
- `JAVA_HOME=/tmp/vacismart-jdk21 PATH=/tmp/vacismart-jdk21/bin:$PATH ./mvnw -B test`: BUILD SUCCESS; 1 teste, 0 falhas/erros/ignorados. Contexto test com H2 carregado. Avisos não fatais de annotation processing e Mockito/Byte Buddy registrados; nenhum teste falhou.
- Primeira tentativa falhou pela falta de javac 21 (D-012); tentativa final resolveu a dificuldade com JDK temporário, sem alteração do sistema.
- `docker compose -f compose.dev.yml config --quiet`: sucesso. `docker compose -f compose.dev.yml up -d --wait`: falha por daemon indisponível (D-013). Não foram validados PostgreSQL, aplicação da V0 ou inicialização dev. `spring-boot:run` com dev não foi executado.
- `git diff --check`: sucesso. `.env` e target ignorados. docs/ESPECIFICACAO.md comparado por igualdade com o conteúdo do pedido: idêntico.
- Sem entidades, regras ou recursos de J2 implementados. Cache, HATEOAS e Actuator apenas previstos para etapas futuras (D-007).

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

## Regressão atual da J2

| Rodada | Testes | Falhas | Erros | Ignorados | Resultado | Duração Maven | Conclusão (America/Sao_Paulo) |
|---|---|---|---|---|---|---|---|
| 1 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 14.871 s | 2026-11-08T00:56:43-03:00 |
| 2 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 11.321 s | 2026-11-08T00:59:35-03:00 |
| 3 | 45 | 0 | 0 | 0 | BUILD SUCCESS | 11.353 s | 2026-11-08T00:59:47-03:00 |

Cada rodada executou `./mvnw -B test` com JAVA_HOME/PATH do JDK 21 temporário. Execução preliminar: 43 testes aprovados. Relatórios Surefire locais; PostgreSQL/Flyway e testes de funcionalidades futuras permanecem pendentes conforme D-013 e lista acima.
