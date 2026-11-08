# Governança — VaciSmart

## Leitura obrigatória
Antes de qualquer tarefa, ler AGENTS.md, README.md, DUVIDAS.md e docs/ESPECIFICACAO.md.
A fonte de verdade da API é docs/ESPECIFICACAO.md. docs/dossie, se existir, é somente referência; conflitos vão para DUVIDAS.md.

## Projeto e escopo
Protótipo acadêmico FIAP GS 2026/2, CitizenTech: monitoramento térmico de lotes em transporte simulado, bloqueio preventivo e informação de disponibilidade ao cidadão. Somente dados fictícios; não é sistema clínico.
J1 criou bootstrap, infraestrutura local e documentação. J2 implementa entidades, repositories, regras térmicas e auditoria, sem controllers; segurança para J4 e OpenAPI para J5. Compose final API + banco é de DevOps.
Não expandir o escopo nem inventar requisitos. Em ambiguidade, adotar a opção mais conservadora e registrar em DUVIDAS.md, incluindo alternativas, impacto e reversão.

## Convenções
Nomes de domínio e contrato em português, exatamente como docs/ESPECIFICACAO.md: entidades, enums, campos JSON e rotas nunca devem ser traduzidos. Sufixos técnicos Java em inglês: Controller, Service, Repository, Dto. Comentários e documentos em português.
Java 21, Maven na raiz, pacote br.com.fiap.vacismart. Estrutura: config; domain/entity, domain/enums, domain/rules; repository; service; web/controller, web/dto, web/advice; security. Na J2, config, domain, repository e service contêm implementações; web e security permanecem reservados.
Perfis dev (PostgreSQL, Flyway, validate) e test (H2, sem Flyway, create-drop). Fuso America/Sao_Paulo; datas ISO-8601.

## Segurança e qualidade
Segurança é responsabilidade do backend: nunca confiar no front. Não colocar segredos, senhas reais ou tokens no Git nem nos logs. Credenciais de .env.example são exemplos locais, nunca de produção.
Toda regra de negócio exige teste automatizado. Atualizar a documentação antes de concluir tarefas.
Spring Cache, Spring HATEOAS e Spring Actuator são exigências oficiais previstas para próximas etapas; não implementar na J1. A orientação anterior de dispensa foi corrigida pelo solicitante em 06/11/2026 (D-007).
NÃO implementar no projeto: GPS, rota ou coordenadas; agendamento ou reserva; push, e-mail ou SMS; integração SUS/RNDS; QR Code; mapa de surtos; IA.

## Comandos
Com JAVA_HOME apontando para JDK 21 e seu bin no PATH:
- `./mvnw test`
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- `docker compose -f compose.dev.yml up -d` (após copiar .env.example para .env)

## Workspace e procedimento antes de cada etapa
O diretório exclusivo do projeto é `/home/gio/Documentos/Global Solution/VaciSmart`. O caminho antigo não deve ser utilizado.
Antes de cada etapa: executar `git status -sb`, `git log --oneline -5`, verificar `git diff` e `git diff --cached` e ler os quatro documentos obrigatórios. Preservar alterações preexistentes e conferir o estado real antes de qualquer operação.

## Git e definição de pronto
Nas próximas etapas, usar Conventional Commits com descrições objetivas em português, representando alterações reais: `feat: implementa cadastro de lotes`, `fix: corrige validação de temperatura`, `refactor: simplifica serviço de transportes`, `test: adiciona testes de bloqueio preventivo`, `docs: atualiza instruções de execução`, `build: configura dependências de segurança`.
Não mencionar ferramentas de assistência nos títulos dos commits; manter as declarações acadêmicas verdadeiras na documentação apropriada.
Criar commits locais pequenos e progressivos somente quando autorizados pelo escopo da tarefa. Adicionar apenas arquivos relacionados; inspecionar `git diff` e `git diff --cached` antes dos commits. Não incluir temporários, credenciais ou relatórios de revisão. Nunca um único commit gigante.
Não modificar os commits existentes. Não realizar push sem autorização explícita, push --force, reset --hard, clean com remoção de arquivos, rebase ou alterações do histórico sem autorização.
Executar os testes disponíveis. Ao finalizar: verificar Git, listar os commits realizados e arquivos pendentes e registrar resultados reais. Pronto significa compila, testes passam, documentação atualizada e commits feitos quando autorizados; validações impedidas devem ser relatadas, sem afirmar aceite completo. Restrições específicas de cada tarefa prevalecem; commits locais exigem autorização no escopo vigente e push exige autorização explícita própria.

## Protocolo obrigatório de revisão externa
Ao concluir cada etapa (J1, J2, J3 etc.), gerar ou sobrescrever na raiz um único `RELATORIO_REVISAO_CHATGPT.md`, refletindo o estado real e suficiente para revisão técnica externa antes de autorizar a próxima implementação.
Não adicionar esse relatório ao Git nem incluí-lo em commits sem autorização explícita do solicitante. Antes de qualquer add/commit, conferir que o relatório está fora do índice; evitar comandos que adicionem todos os arquivos indiscriminadamente.
Estrutura obrigatória:
- A. Identificação: etapa, objetivo, data, estado, HEAD/branch e conclusão ou execução parcial.
- B. Resumo: implementação, funcionalidades/regras efetivas, pendências e exclusões deliberadas.
- C. Estrutura: árvore sem gerados/dependências, arquivos criados/alterados/excluídos, responsabilidades e dependências.
- D. Configurações/arquitetura: versões reais, pom, perfis, persistência, migrations, relacionamentos, segurança e trechos sem segredos.
- E. Código: classes/métodos, regras, validações, erros e relação com a especificação; incluir código relevante suficiente para revisão.
- F. Endpoints, quando existirem: método, rota, perfil, request/response, códigos, validações e regras.
- G. Testes: comandos realmente executados, resultados exatos e resumos Maven, contagens, falhas/correções e testes não executados com motivos.
- H. Git: saídas de `git status -sb`, `git log --oneline -5`, diff relevante, commits da etapa e arquivos pendentes.
- I. Documentação: mudanças em README, AGENTS, DUVIDAS e ESPECIFICACAO e eventuais divergências.
- J. Decisões: confirmadas/provisórias, dúvidas abertas, riscos, problemas não resolvidos e autorizações necessárias.
- K. Critérios de aceite com classificação fundamentada: APROVADO, PENDENTE, NÃO TESTADO ou FALHOU.
- L. Próxima etapa: pronto, correções, impedimentos e recomendação, sem execução automática.
Separar evidências históricas de verificações atuais; nunca inventar resultados nem tratar o próprio relatório como evidência de teste. Não incluir credenciais, tokens, senhas reais ou conteúdo sensível; não ampliar o escopo para preencher o documento. Distinguir implementado de planejado.
Na entrega, informar o caminho completo do relatório, confirmar que está pronto para copiar e apresentar síntese curta. Aguardar revisão e aprovação explícita antes da próxima etapa; não avançar automaticamente à próxima etapa.

## Convenções do núcleo J2
Temperaturas e parâmetros usam BigDecimal, numeric(10,3), sem arredondamento na entrada. Relacionamentos ManyToOne LAZY, unidirecionais e sem cascade. Entidades não usam @Data nem setters genéricos de estado. Regras coordenadas pelo MonitoramentoService transacional, serializadas pelo lock do transporte. Datas usam OffsetDateTime e relógio Clock injetável. Toda nova mutação de estado exige evento de auditoria; normalização/encerramento nunca libera lote. Testes H2 não comprovam migrations PostgreSQL. Não avançar à J3 sem revisão explícita.
