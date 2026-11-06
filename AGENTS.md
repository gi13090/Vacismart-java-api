# Governança — VaciSmart

## Leitura obrigatória
Antes de qualquer tarefa, ler AGENTS.md, README.md, DUVIDAS.md e docs/ESPECIFICACAO.md.
A fonte de verdade da API é docs/ESPECIFICACAO.md. docs/dossie, se existir, é somente referência; conflitos vão para DUVIDAS.md.

## Projeto e escopo
Protótipo acadêmico FIAP GS 2026/2, CitizenTech: monitoramento térmico de lotes em transporte simulado, bloqueio preventivo e informação de disponibilidade ao cidadão. Somente dados fictícios; não é sistema clínico.
J1 cria apenas bootstrap, infraestrutura local e documentação. Entidades e regras ficam para J2; segurança para J4 e OpenAPI para J5. Compose final API + banco é de DevOps.
Não expandir o escopo nem inventar requisitos. Em ambiguidade, adotar a opção mais conservadora e registrar em DUVIDAS.md, incluindo alternativas, impacto e reversão.

## Convenções
Nomes de domínio e contrato em português, exatamente como docs/ESPECIFICACAO.md: entidades, enums, campos JSON e rotas nunca devem ser traduzidos. Sufixos técnicos Java em inglês: Controller, Service, Repository, Dto. Comentários e documentos em português.
Java 21, Maven na raiz, pacote br.com.fiap.vacismart. Estrutura: config; domain/entity, domain/enums, domain/rules; repository; service; web/controller, web/dto, web/advice; security. Na J1, esses pacotes contêm somente package-info.java.
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

## Git e definição de pronto
Commits pequenos, progressivos, em português e no imperativo. Nunca um único commit gigante.
Pronto significa: compila, testes passam, documentação atualizada e commits feitos. Validações impedidas devem ser relatadas com resultado real em DUVIDAS.md e README.md.
