# VaciSmart — API Java
Protótipo acadêmico FIAP Global Solution 2026/2 (CitizenTech) de monitoramento térmico de lotes de vacinas em transporte simulado, bloqueio preventivo e informação de disponibilidade ao cidadão. Dados fictícios: não é sistema clínico nem autoriza aplicação real.

## Problema, beneficiário e ODS
O projeto demonstra como registrar ocorrências térmicas e apoiar a conferência humana, oferecendo ao cidadão informação de disponibilidade sem reserva ou garantia de dose. Beneficiário final: cidadão; usuários operacionais: funcionário de unidade e gestor. ODS principal 3 (Saúde e Bem-Estar); secundários 9 e 10.

## Equipe
| Nome | RM | Turma |
|---|---|---|
| A preencher | A preencher | A preencher |

## Pitch
Link: a preencher.

## Como executar
Pré-requisitos: JDK 21, Docker Engine e Docker Compose, porta 5432 disponível e acesso ao Maven Central no primeiro uso. O Maven Wrapper fixa Maven 3.9.11. Spring Boot 4.1.2 ([versão estável](https://spring.io/projects/spring-boot/), [compatibilidade Java](https://docs.spring.io/spring-boot/system-requirements.html)); decisões em DUVIDAS.md.

Configure JAVA_HOME para um **JDK completo** 21 (java e javac) e adicione `$JAVA_HOME/bin` ao PATH, somente na sessão atual. Exemplo (substitua o caminho pela sua instalação):
```bash
export JAVA_HOME=/caminho/do/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

Na raiz:
```bash
cp .env.example .env
# Revise .env: os valores fornecidos são exemplos exclusivos do desenvolvimento local.
docker compose -f compose.dev.yml up -d
docker compose -f compose.dev.yml ps
./mvnw test
# Exporte as mesmas variáveis para o Spring (Compose não as exporta para a API).
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
O arquivo .env deve conter somente atribuições de variáveis confiáveis; o comando acima o executa no shell. A API também aceita DB_URL, DB_USER e DB_PASSWORD já exportadas. Defaults de exemplo existem somente no perfil dev. Não há perfil padrão nem credenciais de produção. API em http://localhost:8080, sem endpoints de negócio nesta etapa (404 é esperado na raiz).

O perfil dev usa PostgreSQL, Flyway e `ddl-auto=validate`. `V0__baseline.sql` contém somente comentário: não há entidades nem seed. O perfil test usa H2 em memória (modo PostgreSQL), sem Flyway e `create-drop`. Fuso America/Sao_Paulo e JSON com datas ISO-8601. Nunca usar as credenciais de exemplo fora do ambiente local.

Para parar sem apagar o volume:
```bash
docker compose -f compose.dev.yml down
```

## Estrutura
`pom.xml` e Maven Wrapper na raiz; código em `src/main/java/br/com/fiap/vacismart`; pacotes config, domain (entity, enums, rules), repository, service, web (controller, dto, advice), security. Pacotes reservados contêm apenas package-info.java. Especificação em [docs/ESPECIFICACAO.md](docs/ESPECIFICACAO.md); governança em [AGENTS.md](AGENTS.md); decisões em [DUVIDAS.md](DUVIDAS.md).

## Entidades e relacionamentos
A implementar em J2. Diagrama Mermaid: a preencher após modelagem; nenhuma entidade criada na J1.

## Usuários de teste por papel
| Papel | Usuário fictício |
|---|---|
| CIDADAO | A preencher em J4 |
| FUNCIONARIO | A preencher em J4 |
| GESTOR | A preencher em J4 |
O dispositivo terá credencial própria; não é usuário. Nenhum usuário ou autenticação implementado agora.

## Endpoints
A implementar; contrato previsto em docs/ESPECIFICACAO.md com prefixo /api/v1. Nenhuma rota funcional na J1.

## Justificativas: consultas, cascade e fetch
A preencher quando houver entidades, relacionamentos e consultas. Não há escolhas JPA de cascade/fetch nem consultas implementadas nesta etapa.

## Testes
`./mvnw test`: teste de fumaça com @SpringBootTest e @ActiveProfiles("test"). H2 não substitui futura validação das migrations no PostgreSQL. Toda regra de negócio futura exige teste automatizado; T01–T10 ainda não implementados.

## Limitações e escopo
J1 entrega somente bootstrap. Sem regras, entidades, CRUD, seed, Security, JWT ou OpenAPI. J2 trata domínio; J4 segurança; J5 documentação OpenAPI. Compose final API + banco e implantação Azure ficam com DevOps.
Spring Cache, Spring HATEOAS e Spring Actuator são requisitos oficiais previstos para próximas etapas, sem implementação ou dependências nesta J1. A orientação inicial de dispensa foi corrigida pelo solicitante (D-007). Não implementar GPS, rota/coordenadas, agendamento/reserva, push/e-mail/SMS, integração SUS/RNDS, QR Code, mapa de surtos ou IA. A especificação da API prevalece sobre os materiais de referência.

## Declaração de uso de IA
Codex foi usado nesta J1 para consultar o pedido e referências, criar estrutura/configuração/documentação e executar as validações relatadas abaixo. Claude: ferramenta indicada pelo solicitante; não houve uso de Claude verificável nesta execução. A equipe deve complementar apenas com seu uso real e registrar a revisão humana realizada; não há declaração de revisão humana já concluída.

## Status por funcionalidade
| Funcionalidade | Status |
|---|---|
| Bootstrap Maven/Java 21, perfis e pacotes | Criado |
| PostgreSQL local / Flyway baseline | Configurado; validação abaixo |
| Governança e especificação | Criadas |
| Contexto no perfil test | Validação abaixo |
| Entidades, regras e seed (J2) | Pendente |
| CRUD e consultas | Pendente |
| Spring Cache, HATEOAS e Actuator | Previstos para próximas etapas, fora da J1 |
| Segurança (J4) | Pendente |
| OpenAPI (J5) | Pendente |
| Compose final / Azure | Pendente — DevOps |

## Evidências de execução
Em 06/11/2026:
- Maven Wrapper gerado pelo plugin oficial 3.3.4, tipo only-script, fixando Maven 3.9.11.
- A instalação `/usr/lib/jvm/java-21-openjdk-amd64` continha runtime 21, mas não javac; primeira tentativa de teste falhou com `release version 21 not supported` ao recorrer ao compilador 17.
- Usado Eclipse Temurin 21.0.13+9 temporariamente em `/tmp/vacismart-jdk21`, baixado pela API oficial Adoptium, com SHA-256 verificado. Nenhum Java instalado, JAVA_HOME global ou configuração de sistema foi alterado. Esse caminho é temporário e não é pré-requisito permanente.
- Antes dos testes: `java -version` = Temurin 21.0.13; `javac -version` = 21.0.13; `./mvnw -version` = Maven 3.9.11 com runtime Temurin 21.0.13.
- `JAVA_HOME=/tmp/vacismart-jdk21 PATH=/tmp/vacismart-jdk21/bin:$PATH ./mvnw -B test`: **BUILD SUCCESS**, 1 teste, 0 falhas, 0 erros e 0 ignorados; contexto no perfil test com H2. Houve avisos não fatais de processamento de anotações e de autoanexação do agente Mockito/Byte Buddy no Java 21.
- `docker compose -f compose.dev.yml config --quiet`: passou.
- `docker compose -f compose.dev.yml up -d --wait`: falhou; daemon Docker indisponível (`/var/run/docker.sock` ausente no ambiente com execução autorizada). CLI Docker 29.9.0 e Compose v5.5.1 estão presentes. PostgreSQL e migration V0 **não validados em execução**.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`: não executado por falta do PostgreSQL. Após disponibilizar o daemon, repetir os passos de execução acima.
- `git diff --check`: passou; `.env` e `target` são ignorados; especificação conferida por igualdade integral com o pedido.

Relatório Surefire local: `target/surefire-reports/`. Os logs temporários de validação não são versionados.
