# VaciSmart — Java Advanced

**FIAP Global Solution 2026/2 · CitizenTech · Turma 2TDPA**

Uma ocorrência térmica durante o transporte de vacinas precisa ser registrada e encaminhada para conferência. O VaciSmart demonstra esse fluxo: recebe leituras de um transporte simulado, identifica desvios e bloqueia preventivamente os lotes afetados. A temperatura voltar ao normal não libera o lote. O recebimento e a decisão técnica são ações humanas, com autoria e histórico.

A consulta ao cidadão apresenta a disponibilidade informada pela unidade, sem expor a logística interna nem oferecer reserva ou garantia de dose. O projeto usa somente dados fictícios: não é um sistema clínico e não autoriza aplicação real de vacina.

O beneficiário final é o cidadão; funcionário de unidade e gestor operam os fluxos internos. A proposta se relaciona ao **ODS 3 — Saúde e Bem-Estar**, com apoio dos **ODS 9 — Indústria, Inovação e Infraestrutura** e **10 — Redução das Desigualdades**: rastrear ocorrências, apoiar a conferência e oferecer informação compreensível sobre atendimento.

## Equipe e entregas

Nome da equipe: pendente de confirmação. As responsabilidades individuais dos demais integrantes não foram atribuídas; Giovanni Sacristan é o responsável informado por Java Advanced e DevOps.

| Integrante | RM |
|---|---|
| Gabriel Deotti Zanatta | 567258 |
| Murilo Massini | 567383 |
| Giovanni Sacristan | 567548 |
| Caike Roberto de Souza Hollo | 568104 |
| Gianluca Kenis de Araujo | 566765 |

- [Vídeo pitch do VaciSmart — Global Solution 2026/2](https://youtu.be/ttOAN3gsTNY).
- [Repositório da equipe](https://github.com/gi13090/Vacismart-java-api).

O link do pitch foi fornecido pelo responsável; duração e acesso ao vídeo não foram validados nesta revisão. Para Java Advanced, o material oficial orienta a submissão no Portal FIAP de um arquivo TXT contendo somente o link do repositório público; este README não confirma que a submissão foi realizada.

## Como usar este README

| Para consultar | Seção |
|---|---|
| Entender os fluxos do projeto | [Funcionalidades e regras](#funcionalidades-e-regras) |
| Explicar a estrutura do backend | [Tecnologias e arquitetura Java](#tecnologias-e-arquitetura-java) e [Modelagem JPA](#modelagem-jpa) |
| Consultar contratos e exemplos | [Endpoints e operações REST](#endpoints-e-operações-rest) |
| Estudar as escolhas de persistência | [Consultas Spring Data JPA](#consultas-spring-data-jpa) |
| Explicar erros e permissões | [Validação e tratamento de erros](#validação-e-tratamento-de-erros) e [Segurança](#segurança) |
| Rodar a API no desenvolvimento | [Como executar a API](#como-executar-a-api) e [Swagger/OpenAPI](#swaggeropenapi) |
| Preparar a avaliação | [Rubrica](#cobertura-da-rubrica-java-advanced), [roteiro](#roteiro-para-apresentação-presencial) e [estudo](#estudo--perguntas-e-respostas-técnicas) |

### Documentação complementar

A documentação de infraestrutura, com acesso à VM existente, Docker e arquitetura Azure, está no [README_DEVOPS.md](README_DEVOPS.md). O [contrato Mobile](docs/INTEGRACAO_MOBILE_GEMINI.md) é uma referência do backend para o aplicativo; não comprova integração ponta a ponta.

## Funcionalidades e regras

### Cadastros e preparação do transporte

| Módulo | Funcionamento implementado | Regra que protege o fluxo |
|---|---|---|
| Unidades de saúde | Cadastro, consulta, atualização e exclusão | Escritas de unidade são exclusivas do gestor; exclusão respeita dependências e integridade referencial |
| Vacinas | Catálogo operacional com faixa térmica e margem de atenção por vacina | Mínimo menor que máximo, margem não negativa e duas vezes a margem menor ou igual à amplitude; parâmetros ficam congelados quando existem lotes |
| Transportes | Cadastro com código e unidade responsável, consulta e edição antes do monitoramento | Começam `CADASTRADO`; funcionário atua na própria unidade e não transfere um transporte para outra |
| Lotes | Cadastro associado a vacina e transporte; consulta e alteração do código | Cadastro só em transporte `CADASTRADO`; inicia `EM_MONITORAMENTO`/`NAO_DISPONIVEL`; não há DELETE de lote |
| Início do monitoramento | Ação explícita muda `CADASTRADO` para `EM_TRANSPORTE` | Exige pelo menos um lote, registra o horário de início e gera auditoria |

Os estados não são `campos` livres dos DTOs de cadastro. O backend executa as transições por operações próprias e rejeita alterações incompatíveis, em vez de confiar no estado enviado pelo cliente.

### Leituras, ocorrências e bloqueio preventivo

A ingestão atual é uma **simulação humana autenticada**. O campo `dispositivoId` identifica textualmente a origem, mas não funciona como credencial de hardware. O transporte deve estar `EM_TRANSPORTE`; `transporteId` do JSON deve coincidir com o da rota. `medidaEm` deve ter offset e estar entre o início do monitoramento e o instante de recebimento pelo servidor.

`ClassificadorTemperatura` usa `BigDecimal` e os parâmetros da vacina. Para o exemplo com mínimo 2 °C, máximo 8 °C e margem 1 °C:

| Classificação | Intervalo do exemplo | Efeito |
|---|---|---|
| `NORMAL` | De 3 a 7 °C, inclusive | Persiste a leitura |
| `ATENCAO` | De 2 a menos de 3 °C, ou acima de 7 até 8 °C | Registra atenção; não bloqueia nem descarta |
| `FORA_DA_FAIXA` | Abaixo de 2 ou acima de 8 °C | Abre ocorrência e bloqueia os lotes afetados ainda `EM_MONITORAMENTO` |

A precisão é numeric(10,3), com até sete dígitos inteiros e três casas decimais significativas. 5.2000 é numericamente aceito; 8.0001 é rejeitado para evitar arredondamento que mude a classificação.

Em um transporte com vacinas diferentes, a mesma leitura ambiental é comparada com cada faixa. A leitura guarda a pior classificação encontrada, mas apenas os lotes cuja vacina estiver fora da faixa podem ser bloqueados. Não se presume a temperatura individual de cada embalagem.

Há no máximo uma ocorrência `ABERTA` de cada tipo por transporte. A normalização térmica não encerra automaticamente a ocorrência de temperatura e não libera o lote. `MonitoramentoService` também verifica perda de telemetria quando chamado explicitamente: mais de três minutos sem recebimento, por padrão, abre `PERDA_DE_TELEMETRIA`. Exatamente três minutos não abre; o limite é configurável pelo parâmetro vacismart.telemetria.limite, por exemplo PT3M. Não existe agendador periódico. O recebimento avalia essa condição, e a retomada de leituras encerra somente a ocorrência de telemetria. A perda de telemetria sozinha não bloqueia lotes.

### Recebimento, decisão e disponibilidade

| Etapa | Quem pode executar | Resultado e restrições |
|---|---|---|
| Receber transporte | `FUNCIONARIO` da unidade de destino ou `GESTOR` | `EM_TRANSPORTE` passa a `RECEBIDO`; lotes `EM_MONITORAMENTO` passam a `AGUARDANDO_CONFERENCIA`; bloqueados permanecem bloqueados |
| Decidir tecnicamente | Somente `GESTOR` | `Transporte` `RECEBIDO` e lote aguardando conferência ou bloqueado; LIBERAR ou DESCARTAR exige `justificativa` de até 1000 caracteres |
| Confirmar disponibilidade | `FUNCIONARIO` da unidade ou `GESTOR` | Exige transporte `RECEBIDO` e lote `LIBERADO`; muda para `CONFIRMADA` e audita a mudança |
| Retirar disponibilidade | `FUNCIONARIO` da unidade ou `GESTOR` | Mantém as mesmas pré-condições; muda para `NAO_DISPONIVEL` sem descartar o lote |

A decisão técnica leva o lote a um destino terminal e não publica disponibilidade nem encerra ocorrências. Repetir uma decisão retorna 409. Já o comando de disponibilidade representa o estado desejado: repetir um valor válido retorna 204 sem novo evento, após revalidar as pré-condições.

### Consulta cidadã e verificação simulada de aplicação

`ConsultaDisponibilidadeService` retorna a mesma visão pública aos três perfis ativos. A página parte dos pares unidade × vacina e apresenta `CONFIRMADA`, `CONFIRMACAO_RETIRADA` ou `SEM_INFORMACAO`. A ausência de informação não significa falta real de estoque, e a presença de um par não afirma oferta pela unidade.

O retorno contém referências públicas de vacina/unidade, situação, `atualizadoEm` e mensagem. `atualizadoEm` vem do maior horário de eventos pertinentes de confirmação/retirada; sem evento, é null. Não é a data da consulta. Dados de lotes, transportes, leituras, dispositivos, funcionários e quantidades não são expostos.

A verificação de aplicação recebe somente `loteId`. Se o lote estiver `LIBERADO`, retorna 200 com resultado `PERMITIDA` e aviso de simulação; não exige disponibilidade `CONFIRMADA`, não baixa estoque e não registra administração clínica. Qualquer outro estado gera auditoria `APLICACAO_REJEITADA` e resposta 409. Negação por perfil/unidade não produz esse evento de negócio.

### Histórico, autoria e limites

`EventoAuditoria` registra entidade, `referenciaId`, acao, autor, `dataHora` e `justificativa`. As ações automáticas de monitoramento usam SISTEMA; os fluxos humanos usam o UUID autenticado. Consultar histórico não altera estados. O histórico do transporte contém eventos da entidade `Transporte` e não agrega automaticamente os eventos dos lotes.

São limites do MVP: não há GPS, rotas, coordenadas, reserva, agendamento, push/e-mail/SMS, integração SUS/RNDS, QR Code, mapa de surtos, IA no produto ou caderneta clínica. A autenticação própria de dispositivo e a finalização do transporte não estão implementadas. A integração Mobile ponta a ponta permanece sem comprovação.

## Tecnologias e arquitetura Java

As versões abaixo são as declaradas nos arquivos do projeto, não uma indicação da versão mais recente disponível.

| Tecnologia | Uso no VaciSmart |
|---|---|
| Java 21 e Maven Wrapper 3.3.4 / Maven 3.9.11 | Compilação, records de DTO e execução reproduzível pelo Wrapper |
| Spring Boot 4.1.2 / Spring Web | Inicialização, configuração e API REST |
| Spring Data JPA / Hibernate | Entidades, transações, consultas derivadas, JPQL e `Specification` |
| PostgreSQL 17.6 / Flyway | Banco de desenvolvimento e migrations V0/V1/V2; Hibernate em validate |
| Bean Validation | Validação de DTOs de entrada, complementada por regras de domínio |
| Spring Security / OAuth2 Resource Server / Nimbus | BCrypt, emissão e validação de JWT RS256, autorização e CORS |
| springdoc-openapi 3.2.0 | OpenAPI e Swagger UI |
| Spring Boot Test / JUnit / H2 | Testes unitários e de integração no perfil test |
| Lombok | Getters e construtores de suporte nas entidades, sem setters genéricos de estado |

### Separação de responsabilidades

~~~mermaid
flowchart LR
    Cliente[Cliente HTTP] --> Seguranca[Spring Security]
    Seguranca --> Controller[Controller e DTO de entrada]
    Controller --> Service[Service transacional]
    Service --> Repository[Repository]
    Repository --> Database[(PostgreSQL)]
    Service --> Dominio[Entidades e regras]
    Controller --> Saida[DTO de saída]
    Advice[ErroApiAdvice] -.-> Controller
~~~

| Camada | Exemplos reais | Responsabilidade |
|---|---|---|
| Controllers | `VacinaController`, `TransporteController`, `OperacaoTransporteController`, `LoteController`, `AplicacaoController` | Receber a requisição, validar o DTO e definir a resposta HTTP |
| Services de cadastro e consulta | `CadastroService`, `ConsultaOperacionalService`, `ConsultaDisponibilidadeService` | Aplicar escopo, consultar/persistir e mapear os resultados dentro da transação |
| Services de negócio | `MonitoramentoService`, `FluxoOperacionalService` | Coordenar regras, locks, mudanças de estado e eventos de auditoria |
| Domínio | `Lote`, `Transporte`, `Vacina`, `ClassificadorTemperatura`, `Validacoes` | Manter invariantes e impedir transições inválidas |
| Repositories | `LoteRepository`, `TransporteRepository`, `OcorrenciaRepository`, `DisponibilidadeConsultaRepository` | Executar consultas e acesso ao banco |
| DTOs | `VacinaEntradaDto`, `LoteSaidaDto`, `PaginaDto`, `DisponibilidadeSaidaDto`, `MapeadorDto` | Definir contratos sem serializar entidades ou carregar relações livremente |
| Segurança | `SegurancaConfig`, `ChavesJwtConfig`, `AcessoService`, `AutenticacaoService` | Validar identidade, emitir tokens e verificar papel/unidade |
| Configurações | `RelogioConfig`, `UsuariosDemoConfig`, `OpenApiConfig` | Relógio testável, provisionamento fictício explícito e documentação |
| Advice | `ErroApiAdvice` | Converter falhas de validação/negócio em respostas padronizadas |

As classes estão em [src/main/java/br/com/fiap/vacismart](src/main/java/br/com/fiap/vacismart). Os controllers não devem decidir se uma temperatura bloqueia um lote. Essa regra precisa ser coordenada nos Services, junto da transação e da autorização, inclusive quando a chamada não vem diretamente de um endpoint. As entidades também protegem suas próprias invariantes: o Service coordena o caso de uso, sem substituir as regras de `Lote` ou `Transporte`.

## Modelagem JPA

`Entidade` é uma `@MappedSuperclass` que centraliza id com `@GeneratedValue`(strategy = GenerationType.UUID). As oito entidades concretas usam UUID; enums são persistidos como STRING e nomes de tabelas/colunas seguem snake_case.

O Mermaid abaixo preserva o diagrama do núcleo térmico. A tabela seguinte inclui também `Usuario` e seu vínculo condicional com a unidade.

~~~mermaid
erDiagram
    UNIDADE_SAUDE ||--o{ TRANSPORTE : recebe
    VACINA ||--o{ LOTE : caracteriza
    TRANSPORTE ||--o{ LOTE : contem
    TRANSPORTE ||--o{ LEITURA_TEMPERATURA : registra
    TRANSPORTE ||--o{ OCORRENCIA : apresenta
    EVENTO_AUDITORIA {
        UUID id PK
        string entidade
        UUID referenciaId
        string acao
        string autor
        datetime dataHora
        string justificativa
    }
~~~

| `Entidade` | Relacionamento e cardinalidade |
|---|---|
| `UnidadeSaude` | Uma unidade pode receber zero ou vários transportes |
| `Vacina` | Uma vacina pode caracterizar zero ou vários lotes |
| `Transporte` | Cada transporte tem uma unidade obrigatória; pode conter vários lotes, leituras e ocorrências |
| `Lote` | Cada lote tem exatamente uma vacina e um transporte |
| `LeituraTemperatura` | Cada leitura pertence a um transporte e guarda `dispositivoId` textual, temperatura/datas e classificação |
| `Ocorrencia` | Cada ocorrência pertence a um transporte; possui tipo e estado |
| `Usuario` | `FUNCIONARIO` tem exatamente uma unidade; `CIDADAO` e `GESTOR` não têm vínculo de unidade. Uma unidade pode ter vários funcionários |
| `EventoAuditoria` | Identifica o objeto por entidade + `referenciaId`; não existe FK polimórfica |

### LAZY, cascade e EntityGraph

Os relacionamentos `@ManyToOne` são unidirecionais e LAZY. Isso evita carregar uma cadeia inteira de objetos para uma consulta que só precisa de IDs. O vínculo de `Usuario` é opcional no mapeamento, mas obrigatório para funcionário pelas regras e constraints.

Não usamos cascade: salvar um lote não deve cadastrar uma vacina implicitamente, e excluir um transporte não deve apagar seu histórico ou seus lotes. `CadastroService` resolve as referências explicitamente e verifica dependências antes de excluir; as FKs do PostgreSQL continuam sendo a proteção final, inclusive contra escritas fora da API.

O método `LoteRepository`.`findByTransporteId` usa `@EntityGraph`(attributePaths = "vacina"). Durante a classificação térmica, o serviço precisa dos parâmetros das vacinas de todos os lotes. Esse carregamento direcionado evita consultas adicionais por lote nesse fluxo, sem tornar todos os relacionamentos EAGER.

### Integridade, transações e auditoria

Flyway mantém [V0/V1/V2](src/main/resources/db/migration): V0 contém somente um comentário; V1 cria sete tabelas do núcleo com checks, FKs e índices; V2 acrescenta usuario e seus vínculos. São oito tabelas de negócio, seis FKs e, contando flyway_schema_history, nove tabelas no schema. Não existe seed automático de dados de negócio nas migrations.

`MonitoramentoService` e `FluxoOperacionalService` usam `@Transactional`. Mudança de estado e auditoria participam da mesma transação: uma falha que causa rollback não deve deixar metade da operação gravada. `CadastroService` e os serviços de consulta usam readOnly nas leituras e transações de escrita nas mutações.

`TransporteRepository`.`buscarParaAtualizacao` usa `PESSIMISTIC_WRITE`. As operações relacionadas ao transporte são serializadas por esse lock; após adquiri-lo, o serviço recarrega a entidade e revalida a unidade. O refresh do lote evita decidir usando um estado carregado antes de uma operação concorrente. Um índice único parcial no PostgreSQL reforça a unicidade da ocorrência `ABERTA` por tipo/transporte.

`EventoAuditoria` é `@Immutable`, tem colunas não atualizáveis e não oferece endpoint de edição/exclusão. Isso protege o uso pela aplicação, sem prometer impedir SQL direto de um administrador do banco.

**Por que a auditoria de aplicação rejeitada não sofre rollback junto com o 409?** `FluxoOperacionalService`.`verificarAplicacao` grava o evento e retorna false normalmente. A transação pode concluir; só depois `AplicacaoController` lança a exceção convertida em 409. Se a exceção fosse lançada dentro da mesma transação, o rollback poderia desfazer o evento. A garantia depende dessa fronteira entre Service transacional e Controller sem transação.

## Endpoints e operações REST

Todas as rotas de negócio começam com **/api/v1**. As tabelas mostram o caminho completo. Os perfis são obtidos do usuário ativo no banco; funcionário fica restrito à unidade nos recursos operacionais.

### Identidade e catálogos

| Método | Rota | Finalidade / autorização | Sucesso |
|---|---|---|---|
| POST | /api/v1/auth/cadastro | Público; cadastra somente `CIDADAO` | 201 + Location |
| POST | /api/v1/auth/login | Público; autentica conta ativa | 200 |
| GET | /api/v1/auth/me | Própria conta; três perfis autenticados | 200 |
| POST | /api/v1/unidades | Cria unidade; `GESTOR` | 201 + Location |
| GET | /api/v1/unidades | Lista unidades; `FUNCIONARIO` vê a própria, `GESTOR` vê todas | 200 |
| GET | /api/v1/unidades/{id} | Consulta unidade no escopo | 200 |
| PUT | /api/v1/unidades/{id} | Atualiza nome; `GESTOR` | 200 |
| DELETE | /api/v1/unidades/{id} | Exclui sem vínculos impeditivos; `GESTOR` | 204 |
| POST | /api/v1/vacinas | Cria vacina; `GESTOR` | 201 + Location |
| GET | /api/v1/vacinas | Lista catálogo operacional; `FUNCIONARIO`/`GESTOR` | 200 |
| GET | /api/v1/vacinas/{id} | Consulta vacina; `FUNCIONARIO`/`GESTOR` | 200 |
| PUT | /api/v1/vacinas/{id} | Atualiza vacina respeitando congelamento das faixas; `GESTOR` | 200 |
| DELETE | /api/v1/vacinas/{id} | Exclui sem lotes/histórico; `GESTOR` | 204 |

### Transportes, lotes e fluxo operacional

| Método | Rota | Finalidade / autorização | Sucesso |
|---|---|---|---|
| POST | /api/v1/transportes | Cria transporte na unidade autorizada; `FUNCIONARIO`/`GESTOR` | 201 + Location |
| GET | /api/v1/transportes | Lista com filtros e escopo de unidade | 200 |
| GET | /api/v1/transportes/{id} | Consulta transporte no escopo | 200 |
| PUT | /api/v1/transportes/{id} | Atualiza somente `CADASTRADO`; `FUNCIONARIO`/`GESTOR` | 200 |
| DELETE | /api/v1/transportes/{id} | Exclui `CADASTRADO` sem vínculos/histórico; `FUNCIONARIO`/`GESTOR` | 204 |
| POST | /api/v1/transportes/{id}/monitoramento | Inicia monitoramento; `FUNCIONARIO`/`GESTOR`, sem corpo | 204 |
| POST | /api/v1/transportes/{id}/recebimento | Recebe na unidade; `FUNCIONARIO`/`GESTOR`, sem corpo | 204 |
| POST | /api/v1/lotes | Cria lote antes do monitoramento; `GESTOR` | 201 + Location |
| GET | /api/v1/lotes | Lista com filtros e escopo de unidade | 200 |
| GET | /api/v1/lotes/{id} | Consulta lote no escopo | 200 |
| PUT | /api/v1/lotes/{id} | Atualiza somente código antes do monitoramento; `FUNCIONARIO`/`GESTOR` | 200 |
| POST | /api/v1/lotes/{id}/decisao-tecnica | LIBERAR ou DESCARTAR com `justificativa`; `GESTOR` | 204 |
| POST | /api/v1/lotes/{id}/disponibilidade | `CONFIRMADA` ou `NAO_DISPONIVEL`; `FUNCIONARIO`/`GESTOR` no escopo | 204 |
| POST | /api/v1/aplicacoes | Verificação simulada por lote; `FUNCIONARIO`/`GESTOR` no escopo | 200 |

### Leituras, histórico e consulta cidadã

| Método | Rota | Finalidade / autorização | Sucesso |
|---|---|---|---|
| POST | /api/v1/transportes/{id}/leituras | Registra leitura simulada; `FUNCIONARIO`/`GESTOR` no escopo | 201 + Location |
| GET | /api/v1/transportes/{id}/leituras/{leituraId} | Consulta leitura pertencente ao transporte | 200 |
| GET | /api/v1/transportes/{id}/leituras | Lista leituras do transporte autorizado | 200 |
| GET | /api/v1/transportes/{id}/ocorrencias | Lista ocorrências; filtro opcional estado | 200 |
| GET | /api/v1/transportes/{id}/historico | Lista eventos do próprio transporte | 200 |
| GET | /api/v1/lotes/{id}/historico | Lista eventos do lote autorizado | 200 |
| GET | /api/v1/disponibilidade | Consulta informativa global; `CIDADAO`, `FUNCIONARIO` ou `GESTOR` | 200 |

Nas cinco consultas operacionais de leituras/histórico/ocorrências, aplicam-se `FUNCIONARIO` da unidade ou `GESTOR`. Os erros relevantes são 400 para entrada inválida, 401 para autenticação ausente/inválida, 403 para acesso negado, 404 para recurso ausente no escopo e 409 para conflito de estado ou dependência; o [tratamento de erros](#validação-e-tratamento-de-erros) explica as diferenças. POST de aplicação não LIBERADA retorna 409 após auditoria; não é uma criação com 201.

Não existem endpoints de exclusão de lote, CRUD de ocorrências/auditoria, encerramento de ocorrência, finalização de transporte ou credencial própria de dispositivo.

### Exemplo de CRUD de vacina

Execute no Swagger como `GESTOR`. Os corpos abaixo são exemplos, não registros previamente existentes. Use um nome exclusivo e guarde o UUID retornado. Para o DELETE, escolha uma vacina criada para descarte na apresentação, sem lotes nem histórico.

1. POST /api/v1/vacinas, com resultado esperado 201 e Location:

~~~json
{
  "nome": "Vacina demonstrativa Java",
  "temperaturaMinC": 2,
  "temperaturaMaxC": 8,
  "margemAtencaoC": 1
}
~~~

2. GET /api/v1/vacinas/{id}: substitua {id} pelo UUID retornado; esperado 200 com os dados da vacina.
3. PUT /api/v1/vacinas/{id}, esperado 200. Para esse registro sem lotes, mantenha as faixas e altere o nome:

~~~json
{
  "nome": "Vacina demonstrativa Java atualizada",
  "temperaturaMinC": 2,
  "temperaturaMaxC": 8,
  "margemAtencaoC": 1
}
~~~

4. DELETE /api/v1/vacinas/{id}: esperado 204 sem corpo, se não houver dependências. Novo GET no mesmo UUID deve retornar 404. Não excluir a vacina usada no fluxo térmico.

### Exemplos de operações relacionadas

Os textos UUID_REAL_* são marcadores a substituir por UUIDs efetivamente retornados pela API. Crie primeiro uma unidade e uma vacina, depois o transporte e o lote; a ordem corresponde às FKs.

| Operação | JSON de entrada |
|---|---|
| POST /api/v1/unidades | {"nome":"Unidade demonstrativa Java"} |
| POST /api/v1/transportes | {"codigo":"TRP-JAVA-EXCLUSIVO","unidadeSaudeId":"UUID_REAL_UNIDADE"} |
| POST /api/v1/lotes | {"codigo":"LOTE-JAVA-EXCLUSIVO","vacinaId":"UUID_REAL_VACINA","transporteId":"UUID_REAL_TRANSPORTE"} |
| PUT /api/v1/lotes/{id} | {"codigo":"LOTE-JAVA-ATUALIZADO"} |
| POST /api/v1/lotes/{id}/decisao-tecnica | {"decisao":"LIBERAR","justificativa":"Conferência humana demonstrativa realizada."} |
| POST /api/v1/lotes/{id}/disponibilidade | {"disponibilidade":"CONFIRMADA"} |
| POST /api/v1/aplicacoes | {"loteId":"UUID_REAL_LOTE"} |

Inicie o monitoramento antes de registrar leituras. Exemplo de POST /api/v1/transportes/{id}/leituras:

~~~json
{
  "transporteId": "UUID_REAL_TRANSPORTE",
  "dispositivoId": "ARDUINO-DEMO-01",
  "temperaturaC": 9.3,
  "medidaEm": "2026-11-19T10:00:00-03:00"
}
~~~

Troque a data do exemplo por um instante válido da demonstração, entre `inicioMonitoramentoEm` e o recebimento atual. Usar uma data antiga ou futura pode retornar 400. Não tente liberar o lote antes do recebimento; não trate a liberação técnica como confirmação automática de disponibilidade.

## Consultas Spring Data JPA

### Query Methods: consultas derivadas para necessidades diretas

Em [LeituraTemperaturaRepository](src/main/java/br/com/fiap/vacismart/repository/LeituraTemperaturaRepository.java), a assinatura existente é:

~~~java
Page<LeituraTemperatura> findByTransporteId(UUID transporteId, Pageable pageable);
~~~

O Spring Data interpreta o nome e filtra leituras pelo UUID do transporte, com paginação no banco. É adequado porque a condição é fixa e simples; não há necessidade de escrever JPQL para essa consulta. `ConsultaOperacionalService` verifica o acesso ao transporte antes da chamada.

Outros exemplos reais são `findFirstByTransporteIdOrderByRecebidaEmDesc`, usado na telemetria, e `existsByVacinaId`, que verifica dependências sem carregar todos os lotes.

### JPQL com @Query: ocorrências por transporte e estado

[OcorrenciaRepository](src/main/java/br/com/fiap/vacismart/repository/OcorrenciaRepository.java) declara:

~~~java
@Query("select o from Ocorrencia o where o.transporte.id = :transporteId "
        + "and (:estado is null or o.estado = :estado)")
Page<Ocorrencia> consultarPorTransporte(
        @Param("transporteId") UUID transporteId,
        @Param("estado") EstadoOcorrencia estado,
        Pageable pageable);
~~~

JPQL usa entidades e atributos Java, não nomes de tabelas SQL. `transporteId` restringe o pai; estado nulo significa todos os estados, e um valor como `ABERTA` restringe o resultado. Os valores são parâmetros vinculados, sem concatenação da entrada do usuário. A mesma consulta atende a inspeção geral e a busca de ocorrências abertas, mantendo `Page` e contagem no banco.

### Specification: combinar filtros sem multiplicar métodos

[Filtros](src/main/java/br/com/fiap/vacismart/repository/Filtros.java) monta os predicados de lotes por `transporteId`, `vacinaId` e estado, e de transportes por `unidadeId` e estado. Trecho do método real:

~~~java
if (transporteId != null) filtros.add(cb.equal(root.get("transporte").get("id"), transporteId));
if (vacinaId != null) filtros.add(cb.equal(root.get("vacina").get("id"), vacinaId));
if (estado != null) filtros.add(cb.equal(root.get("estado"), estado));
return cb.and(filtros.toArray(Predicate[]::new));
~~~

Cada parâmetro opcional acrescenta uma condição com AND. O mesmo método atende consultas com nenhum, um ou vários filtros, sem criar uma assinatura derivada para cada combinação. `LoteRepository` e `TransporteRepository` implementam `JpaSpecificationExecutor`. `CadastroService` acrescenta o escopo do funcionário na consulta antes da paginação e da contagem; não filtra uma página já carregada em memória.

Exemplos de consultas:

~~~http
GET /api/v1/lotes?estado=BLOQUEADO_PREVENTIVAMENTE&page=0&size=10&sort=codigo,asc
GET /api/v1/transportes?estado=RECEBIDO&page=0&size=10&sort=codigo,desc
~~~

Os filtros por UUID podem ser acrescentados com os IDs reais: `transporteId`/`vacinaId` em lotes e `unidadeId` em transportes.

### Paginação e ordenação

`Pageable` transporta página, `tamanho` e ordenação até o Repository. `Page` contém os resultados e a contagem; `PaginaDto` apresenta `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`, sem expor a representação interna do Spring Data.

A página começa em zero; `tamanho` padrão 20 e máximo 100. O resolver normaliza parâmetros page/size inválidos conforme sua configuração. Paginacao.validar limita o `tamanho` e rejeita propriedades de sort fora da lista permitida com 400; acrescenta id como desempate. Não existe ordenação livre por `campos` internos ou senha.

| Recurso | Filtros específicos | Campos de sort permitidos |
|---|---|---|
| Unidades | Escopo de unidade do usuário | id, nome |
| Vacinas | Sem filtro específico de negócio | id, nome |
| Transportes | `unidadeId`, estado | id, codigo, estado, `inicioMonitoramentoEm` |
| Lotes | `transporteId`, `vacinaId`, estado | id, codigo, estado |
| Leituras | `Transporte` da rota | id, `medidaEm`, `recebidaEm`, `temperaturaC`, classificacao |
| Ocorrências | `Transporte` da rota; estado opcional | id, abertaEm, encerradaEm, tipo, estado |
| Histórico | `Entidade`/referência da rota | id, `dataHora`, acao |
| Disponibilidade | `unidadeId`, `vacinaId` | unidadeNome, vacinaNome |

A ordenação operacional padrão resulta em id. Disponibilidade usa unidadeNome e vacinaNome ascendentes quando não há sort, com UUIDs de unidade/vacina como desempate.

### Consulta agregada de disponibilidade

`DisponibilidadeConsultaRepository` usa `EntityManager` e SQL nativo com CTE para paginar os pares unidade × vacina antes de agregar os estados e os eventos. Agregações separadas evitam multiplicar linhas de lotes por eventos. A contagem usa o mesmo universo e filtros; não conta cada lote como uma linha pública. Os filtros são vinculados, e os fragmentos de ordenação vêm de uma lista fixa.

Essa consulta atende uma projeção pública específica e não retorna entidades operacionais. Sua validação funcional no PostgreSQL é histórica; não há medição de desempenho em carga que permita prometer ganho quantitativo.

## Validação e tratamento de erros

Os records de entrada recebem `@Valid` nos Controllers. `VacinaEntradaDto`, por exemplo, exige nome não branco de até 150 caracteres e valores térmicos não nulos; `LoteEntradaDto` exige código de até 60 e UUIDs de vacina/transporte. O cadastro cidadão valida e-mail e senha de 12 a 72 caracteres ASCII imprimíveis. As saídas têm DTOs próprios e não incluem senha/hash.

Bean Validation valida a forma dos `campos`. `Validacoes` e as entidades verificam invariantes como faixa térmica coerente, precisão decimal e ordem das transições. Os Services verificam relações, escopo e condições que dependem do banco. Jackson rejeita `campos` JSON desconhecidos, UUIDs inválidos e enums não reconhecidos.

[ErroApiAdvice](src/main/java/br/com/fiap/vacismart/web/advice/ErroApiAdvice.java) é um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler`. Retorna ProblemDetail; a validação de DTO acrescenta `campos` com nome e mensagem. Exemplo ilustrativo para um nome vazio:

~~~json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Campos inválidos",
  "campos": [
    {"campo": "nome", "mensagem": "não deve estar em branco"}
  ]
}
~~~

A mensagem de cada campo depende do idioma do Validator. `instance` pode aparecer, mas não é obrigatório em todas as respostas. Os filtros de segurança usam `ErrosSeguranca` para 401/403, pois nem toda negação chega ao Controller ou ao Advice. Rejeições de CORS podem ocorrer sem corpo ProblemDetail.

| Código | Situação no VaciSmart |
|---|---|
| 400 | DTO, JSON, precisão, UUID, enum, data ou sort inválido; `transporteId` do corpo diferente da rota |
| 401 | JWT ausente/inválido/expirado, conta inativa ou credenciais incorretas; resposta informa WWW-Authenticate: Bearer |
| 403 | Perfil sem permissão ou recurso fora da unidade autorizada |
| 404 | Recurso ausente no escopo permitido |
| 409 | Estado incompatível, e-mail/código duplicado, vínculos, integridade ou concorrência; inclui aplicação simulada rejeitada |
| 500 | Falha inesperada, com mensagem genérica e sem stack trace na resposta |

Para lote/transporte inexistente ou alheio, o funcionário recebe 403 uniforme, evitando revelar a existência de um recurso de outra unidade; o gestor recebe 404 para inexistente. O tratamento de 500 existe no Advice e é exercitado por suporte exclusivo de teste, sem um endpoint de falha no código de produção.

## Segurança

### Cadastro, senha e emissão do JWT

`AuthController` recebe cadastro e login; `AutenticacaoService` normaliza o e-mail, verifica unicidade e senha, exige conta ativa e emite JWT. Cadastro público cria somente `CIDADAO` e retorna 201 sem `token`; login retorna 200 com `token`, tipo Bearer, `expiraEm` e usuário. O banco armazena **hash BCrypt com custo 12**, não senha reversivelmente criptografada.

`SegurancaConfig` usa OAuth2 Resource Server e Nimbus. A chave privada RSA assina com RS256 e a pública verifica a assinatura; `ChavesJwtConfig` exige par correspondente com no mínimo 2048 bits, privada PEM PKCS#8 e pública PEM/X.509. Chaves externas são obrigatórias no desenvolvimento. O perfil test gera seu próprio par efêmero.

O `token` contém sub (UUID), perfil, iss, iat e exp. O decoder valida RS256, emissor, validade e claims obrigatórias; rejeita emissão futura, UUID inválido e expiração sem tolerância adicional de relógio. A validade padrão é PT15M, configurável por `JWT_VALIDADE`, positiva e no máximo uma hora. Não há refresh `token`, revogação individual nem rotação com múltiplas chaves/JWKS.

### Da requisição à autorização

~~~mermaid
sequenceDiagram
    participant C as Cliente
    participant A as AuthController / AutenticacaoService
    participant S as Spring Security
    participant B as UsuarioRepository
    participant R as Controller / Service
    C->>A: POST /api/v1/auth/login
    A->>B: Buscar usuário ativo e conferir BCrypt
    A-->>C: JWT RS256 e expiração
    C->>S: Requisição com Authorization Bearer
    S->>S: Validar assinatura, emissor e tempo
    S->>B: Consultar usuário ativo e perfil atual
    S->>R: Autorizar rota e identificar usuário
    R->>R: AcessoService valida unidade/recurso
    R-->>C: DTO ou erro padronizado
~~~

A API é stateless, sem sessão HTTP para autenticar, com CSRF desabilitado no modelo de Bearer em header. O perfil da claim é informativo: a cada requisição, os privilégios vêm do usuário ativo no banco. Mudanças de papel, vínculo ou ativação são reavaliadas; um JWT ainda não expirado não preserva privilégios antigos.

`AcessoService` aplica a mesma proteção nos Services. O Controller não aceita autor, perfil ou unidade de autorização enviados livremente pelo cliente.

| Operação | `CIDADAO` | `FUNCIONARIO` | `GESTOR` |
|---|---|---|---|
| Cadastro/login | Públicos | Públicos | Públicos |
| Consultar própria conta | Própria | Própria | Própria |
| Consultar disponibilidade informativa | Global | Global, mesma visão pública | Global, mesma visão pública |
| Consultar unidades | Não | Própria unidade | Todas |
| Criar/editar/excluir unidade | Não | Não | Sim, respeitando dependências |
| Consultar vacinas | Não | Sim | Sim |
| Criar/editar/excluir vacina | Não | Não | Sim, respeitando vínculos/faixas |
| Transportes e leituras | Não | Somente unidade vinculada | Escopo global |
| Criar lote | Não | Não | Sim |
| Consultar lote, editar código e histórico | Não | Somente unidade vinculada | Escopo global |
| Recebimento e disponibilidade | Não | Somente unidade vinculada | Escopo global |
| Decisão técnica | Não | Não | Sim |
| Verificação simulada de aplicação | Não | Somente unidade vinculada | Escopo global |

Escopo global não elimina as regras térmicas ou de estado. O gestor também recebe 409 se tentar uma transição inválida.

### CORS e cuidados de demonstração

`CORS_ORIGENS` recebe origens explícitas separadas por vírgula, por exemplo http://localhost:5173, sem caminho, curinga, usuário ou query. A lista vazia não libera automaticamente o app. O código permite GET/POST/PUT/DELETE/OPTIONS, headers Authorization e Content-Type, expõe Location e não habilita allowCredentials. CORS é uma política do navegador, não uma autorização para acessar a API; Postman não comprova CORS do Mobile.

Não exiba chaves, senhas, hashes ou tokens em capturas. No Swagger, faça login e preencha Authorize fora da gravação, depois recolha a resposta de login. Este protótipo não implementa proteção adicional contra abuso de login, recuperação de senha ou administração pública de perfis.

## Swagger/OpenAPI

`OpenApiConfig` e as anotações dos oito controllers descrevem **34 operações HTTP**: 3 de autenticação, 5 de unidades, 5 de vacinas, 7 de transportes, 7 de lotes, 5 de leituras/ocorrências/histórico do transporte, 1 de disponibilidade e 1 de aplicação. A contagem foi conferida nos mappings atuais e na expectativa de OpenApiIntegrationTest, sem executar a API nesta revisão.

| Caminho local | Uso |
|---|---|
| http://127.0.0.1:8080/swagger-ui.html | Entrada da interface |
| http://127.0.0.1:8080/swagger-ui/index.html | Interface Swagger |
| http://127.0.0.1:8080/v3/api-docs | Documento OpenAPI JSON |
| http://127.0.0.1:8080/v3/api-docs.yaml | Documento YAML |
| http://127.0.0.1:8080/v3/api-docs/swagger-config | Configuração usada pela UI |

A documentação fica desabilitada por padrão e só é habilitada na expressão de perfis (dev ou test), sem prod nem production ativos. Os GETs documentais são públicos nesses perfis; isso não libera operações protegidas. Fora deles, as rotas documentais são negadas.

No Swagger, execute /api/v1/auth/login com suas credenciais fictícias existentes. Copie somente o valor de `token` para **Authorize**; a UI adiciona o prefixo Bearer ao header. A autorização não fica persistida pela interface. DTOs, permissões, filtros, respostas 201/Location, 204 sem corpo e erros estão documentados. A interface não cria usuários operacionais automaticamente.

## Como executar a API

Este procedimento é para **Java no computador de desenvolvimento e PostgreSQL em Docker local**, usando compose.dev.yml. Não é o Compose de dois serviços da Azure. Para o ambiente já implantado e o acesso por túnel, siga o [README_DEVOPS.md](README_DEVOPS.md#acessar-e-verificar-o-vacismart-já-implantado).

Todos os comandos abaixo são instruções para um terminal Bash. Não foram executados nesta revisão documental. Confirme sucesso de cada passo antes de continuar; preserve banco, ambiente e chaves existentes.

### 1. Conferir código, JDK e Maven Wrapper

Tenha Git, JDK completo 21 (java e javac), Docker Engine/Compose para o banco, OpenSSL para as chaves e acesso aos registries/Maven Central no primeiro uso. Em uma instalação nova, clone o repositório e entre na raiz; em cópia existente, apenas use a raiz correta.

~~~bash
# Somente para uma cópia nova:
git clone https://github.com/gi13090/Vacismart-java-api.git
cd Vacismart-java-api

# Substitua pelo diretório do JDK 21 instalado.
export JAVA_HOME=/caminho/do/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
./mvnw -version
~~~

O Wrapper fixa Maven 3.9.11 e confere o SHA-256 da distribuição configurada em .mvn/wrapper/maven-wrapper.properties. Não é necessário instalar Maven globalmente. Se o checkout perder a permissão do Wrapper, use chmod +x mvnw; não troque a versão do JDK para contornar uma cópia incorreta.

### 2. Preparar o ambiente local do banco

~~~bash
# Na raiz; não sobrescreve um .env existente.
if [ ! -e .env ]; then
  (umask 077; cp .env.example .env)
fi
chmod 600 .env
~~~

Edite o arquivo local com atribuições de variáveis confiáveis, usando aspas de shell quando necessário. [.env.example](.env.example) contém somente exemplos de desenvolvimento; não use sua senha de exemplo em produção. Configure os quatro valores em conjunto:

| Variável | Valor esperado no desenvolvimento |
|---|---|
| `DB_NAME` | Nome do banco |
| `DB_USER` | Usuário SQL |
| `DB_PASSWORD` | Senha privada escolhida para esse banco |
| `DB_URL` | jdbc:postgresql://localhost:5432/ seguido do mesmo `DB_NAME` |

Exporte os valores para o Java; o Compose ler .env não exporta variáveis para um processo Maven no host:

~~~bash
set -a
. ./.env
set +a
~~~

O comando acima executa o arquivo no shell: use somente um .env que você controla. Não exiba seu conteúdo em prints. Alterar `POSTGRES_`* ou `DB_`* não reconfigura automaticamente um volume PostgreSQL já inicializado; nesse caso, reutilize as credenciais existentes e não apague o volume para “corrigir” acesso.

### 3. Iniciar e conferir o PostgreSQL local

A porta local 5432 deve estar livre. Se usar um PostgreSQL existente, configure `DB_URL`/credenciais para ele e não inicie um segundo serviço na mesma porta.

~~~bash
docker compose -f compose.dev.yml config --quiet
docker compose -f compose.dev.yml up -d --wait
docker compose -f compose.dev.yml ps
docker compose -f compose.dev.yml exec postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
~~~

O arquivo usa postgres:17.6, publicação somente em 127.0.0.1:5432, healthcheck e volume nomeado postgres_dev_data. Não use compose config sem --quiet em capturas, pois a saída resolvida contém credenciais. Não execute down -v nem prune.

### 4. Configurar as chaves JWT externas

Use um par RSA dedicado, fora do repositório. Se já existe um par válido, informe seus caminhos e preserve-o. O exemplo abaixo gera arquivos apenas quando nenhum dos dois existe e usa um subshell para interromper a geração ao primeiro erro.

~~~bash
KEY_DIR="$HOME/.config/vacismart-java/jwt"
install -d -m 700 "$KEY_DIR"
(
  set -eu
  if [ -e "$KEY_DIR/privada.pem" ] || [ -e "$KEY_DIR/publica.pem" ]; then
    test -f "$KEY_DIR/privada.pem" && test -f "$KEY_DIR/publica.pem"
  else
    umask 077
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEY_DIR/privada.pem"
    openssl pkey -in "$KEY_DIR/privada.pem" -pubout -out "$KEY_DIR/publica.pem"
  fi
  chmod 600 "$KEY_DIR/privada.pem" "$KEY_DIR/publica.pem"
)
# Só continuar se a preparação terminou sem erro.
export JWT_CHAVE_PUBLICA="file:$KEY_DIR/publica.pem"
export JWT_CHAVE_PRIVADA="file:$KEY_DIR/privada.pem"
~~~

O processo Java roda como seu usuário local e deve ler ambos os arquivos. São URIs file: absolutas; caminhos do container não servem para o Java no host. O startup valida o par. Nunca use a chave privada SSH como chave JWT, nem gere outro par a cada reinício se quiser preservar tokens até a expiração.

`JWT_EMISSOR` usa vacismart por padrão; `JWT_VALIDADE` usa PT15M. Se houver frontend de navegador, configure `CORS_ORIGENS` com a origem real permitida, sem curinga. Não ative perfis prod/production para tentar usar este guia de desenvolvimento.

### 5. Escolher os usuários demonstrativos

O cadastro público cria cidadão. Para demonstrar operações internas, use os usuários operacionais já provisionados ou prepare um conjunto fictício novo pelo bootstrap abaixo. Não há senha padrão nem promoção de perfil pelo corpo de uma requisição.

| Papel demonstrativo | Perfil persistido | Uso |
|---|---|---|
| Cidadão | `CIDADAO`, sem unidade | Consulta pública autenticada; negação nas rotas técnicas |
| Funcionário A | `FUNCIONARIO`, unidade fictícia A | Fluxo operacional da própria unidade |
| Funcionário B | `FUNCIONARIO`, unidade fictícia B | Conferir isolamento entre unidades |
| Gestor | `GESTOR`, sem vínculo obrigatório | Cadastros restritos e decisão técnica global |

**Somente no primeiro provisionamento de um conjunto novo:** antes de iniciar a API, defina quatro e-mails fictícios distintos e senhas privadas de 12–72 caracteres ASCII imprimíveis. Estes e-mails são exemplos, não contas que o código cria por padrão.

~~~bash
export DEMO_CIDADAO_EMAIL=cidadao@demo.invalid
export DEMO_FUNCIONARIO_A_EMAIL=funcionario.a@demo.invalid
export DEMO_FUNCIONARIO_B_EMAIL=funcionario.b@demo.invalid
export DEMO_GESTOR_EMAIL=gestor@demo.invalid
for papel in CIDADAO FUNCIONARIO_A FUNCIONARIO_B GESTOR; do
  read -r -s -p "Senha fictícia para $papel: " "DEMO_${papel}_SENHA"
  printf '\n'
  export "DEMO_${papel}_SENHA"
done
export VACISMART_DEMO_HABILITADO=true
export VACISMART_DEMO_CONFIRMACAO=DADOS_FICTICIOS_LOCAIS
~~~

`UsuariosDemoConfig` exige somente o perfil dev, a flag e a confirmação explícitas; cria duas unidades e quatro usuários em uma transação. Se uma conta já existe, falha sem sobrescrevê-la. O perfil test ou qualquer perfil adicional impede esse provisionamento.

### 6. Iniciar a API e usar o Swagger

Na mesma sessão com banco e chaves exportados:

~~~bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
~~~

O startup deve validar/aplicar V0/V1/V2 pelo Flyway, conferir o mapeamento com Hibernate validate e iniciar a API em 127.0.0.1:8080. A raiz / não é um endpoint de negócio; uma rota protegida sem JWT retornar 401 não significa falha de inicialização.

Se ativou o bootstrap no passo anterior, após sua primeira execução bem-sucedida encerre somente esse processo Java com Ctrl+C, desative as variáveis abaixo e inicie a API novamente com o mesmo comando:

~~~bash
unset VACISMART_DEMO_HABILITADO VACISMART_DEMO_CONFIRMACAO
for papel in CIDADAO FUNCIONARIO_A FUNCIONARIO_B GESTOR; do
  unset "DEMO_${papel}_EMAIL" "DEMO_${papel}_SENHA"
done
~~~

Acesse [Swagger local](http://127.0.0.1:8080/swagger-ui/index.html). Faça login com a conta fictícia preparada, use Authorize e siga os exemplos de CRUD/fluxo deste README. Para uma leitura, confira o horário atual e o início do monitoramento antes de preencher `medidaEm`.

### 7. Executar os testes e encerrar o ambiente

A suíte usa perfil test/H2 e chaves efêmeras; o teste não depende de rodar o bootstrap ou a API de desenvolvimento.

~~~bash
./mvnw -B test
~~~

Esse comando é uma instrução de uso, não uma execução feita nesta revisão. Para encerrar, use Ctrl+C no processo Java e, quando não precisar mais do banco local:

~~~bash
docker compose -f compose.dev.yml stop
~~~

A parada preserva o volume. Backups precisam de procedimento próprio; um volume não é backup. O guia de infraestrutura documenta as opções de retomada.

## Testes e resultados

Os testes combinam casos unitários de regras e integrações `SpringBootTest`. Os testes HTTP iniciam servidor real em `RANDOM_PORT`, usam `HttpClient` do JDK e percorrem Controller → Service → Repository. O relógio `Clock` é controlado nos cenários que dependem de tempo.

| Área | Classes de teste e verificação |
|---|---|
| Classificação e precisão | ClassificadorTemperaturaTest, ClassificacaoTemperaturaTest |
| Domínio, persistência, locks e rollback | MonitoramentoServiceIntegrationTest, MapeamentoSchemaTest |
| Contratos HTTP e fluxos operacionais | ApiIntegrationTest |
| JWT, papéis, conta ativa e isolamento de unidades | SegurancaIntegrationTest, UsuariosDemoConfigTest |
| Aplicação simulada e auditoria após 409 | AplicacaoIntegrationTest |
| OpenAPI, UI e perfis de documentação | OpenApiIntegrationTest, DocumentacaoDesabilitadaIntegrationTest, OpenApiPerfisTest |

As classes ficam em [src/test/java/br/com/fiap/vacismart](src/test/java/br/com/fiap/vacismart). O perfil test usa H2 em memória, modo PostgreSQL, create-drop e Flyway desabilitado. Ele verifica regras, transações e contratos, mas não comprova migrations, índice parcial, comportamento integral do PostgreSQL ou desempenho em carga.

### Evidências já registradas

| Ambiente / data | Resultado histórico comprovado | Limite da evidência |
|---|---|---|
| Java 21 + test/H2, 16/11/2026 | Regressão final de 304 testes, zero falhas, erros ou ignorados; BUILD SUCCESS | Resultado registrado da entrega J6; não é uma execução nova nem prova de todos os cenários no PostgreSQL |
| PostgreSQL 17.6, 12/11/2026 | 109 verificações operacionais aprovadas no recorte de segurança/fluxo térmico; provisionamento e login dos quatro usuários demonstrativos | Verificações operacionais, não 109 testes JUnit |
| PostgreSQL 17.6, 14/11/2026 | 57 verificações HTTP aprovadas, incluindo recebimento, decisão, disponibilidade, paginação e privacidade | Não comprova carga nem concorrência integral PostgreSQL |
| Azure, confirmação do responsável | Infraestrutura, CRUD (201/200/200/204/404), SELECTs e um JOIN confirmados; banco saudável, V0/V1/V2 e usuários provisionados | Não é nova execução nesta revisão nem prova de todos os fluxos Java na nuvem |

Os registros de resultados existentes foram conferidos no README anterior e em DUVIDAS.md; não houve build, teste ou comando de infraestrutura nesta reestruturação. A persistência após stop/start na Azure **não foi comprovada** e não será demonstrada nesta etapa. A configuração de volume, sozinha, não encerra essa limitação.

## Cobertura da rubrica Java Advanced

Referência: seção **Java Advanced**, páginas 52–56 do material “2TDS Agosto — Global Solution 2026-2”, conferida na cópia oficial disponibilizada ao grupo. Os oito critérios e pesos abaixo descrevem a rubrica, não uma nota obtida. A entrega no Portal FIAP ainda não foi confirmada.

| Critério oficial | Peso | Funcionalidade / classes relevantes | Evidência disponível | Situação real |
|---|---|---|---|---|
| API REST e modelagem | 20 | Oito controllers, DTOs, entidades e `CadastroService` | Mappings, migrations, testes HTTP e validações PostgreSQL históricas | IMPLEMENTADO no MVP, com restrições de estado/vínculo documentadas |
| Consultas JPA | 15 | `LeituraTemperaturaRepository`, `OcorrenciaRepository`, Filtros, Paginacao | Query Method, JPQL, `Specification` e testes de consulta/paginação | IMPLEMENTADO |
| Validação e erros | 10 | DTOs, `Validacoes`, `ErroApiAdvice`, `ErrosSeguranca` | Testes de entrada inválida e respostas HTTP padronizadas | IMPLEMENTADO; CORS possui tratamento próprio |
| Cache | 10 | Sem `@Cacheable`/`@CacheEvict` ou configuração Spring Cache | Ausência do recurso no código e decisão D-046 | NÃO IMPLEMENTADO |
| HATEOAS | 10 | DTOs sem RepresentationModel/links de ações | Ausência de Spring HATEOAS | NÃO IMPLEMENTADO; Location de 201 não substitui HATEOAS |
| Actuator | 5 | Sem endpoints Actuator health/info/metrics | Ausência da dependência e dos endpoints | NÃO IMPLEMENTADO; OpenAPI e pg_isready não substituem Actuator |
| Swagger e README | 10 | `OpenApiConfig`, anotações nos controllers/DTOs, este README | 34 mappings, teste de equivalência e validações históricas da UI | IMPLEMENTADO; documentação/execução sujeitas à avaliação do professor |
| JWT, papéis e CORS | 20 | `SegurancaConfig`, `AutenticacaoService`, `AcessoService`, `ChavesJwtConfig` | Testes de segurança; validações históricas de login e isolamento | IMPLEMENTADO no backend; integração real do app não comprovada |

Spring Cache, HATEOAS e Actuator foram excluídos por decisão expressa de escopo, registrada em [D-046](DUVIDAS.md#d-046--decisão-definitiva-de-escopo--12112026). Essa escolha não equivale a dispensa concedida pelo professor e não atende aos três critérios oficiais. Não há consultas cacheadas nem invalidação de cache de aplicação. As consultas JPA e o EntityGraph são escolhas de acesso ao banco, não substitutos de `@Cacheable`. O contexto de persistência do Hibernate também não deve ser apresentado como implementação de Spring Cache.

## Roteiro para apresentação presencial

Prepare o ambiente local, os usuários fictícios e registros com códigos exclusivos antes da aula. As falas abaixo são sugestões para explicar o que estiver na tela; não representam resultados já obtidos na apresentação.

| Momento | Mostrar e fazer | Explicação sugerida / cuidado |
|---|---|---|
| 1. Problema e equipe | Objetivo CitizenTech, integrantes, turma e pitch | “Um desvio térmico precisa de rastreabilidade e conferência; usamos dados fictícios e não tomamos decisão clínica.” |
| 2. Modelo de domínio | Mermaid e entidades `Lote`, `Transporte`, `Usuario` | Relacionar vacina, lote, transporte e unidade; explicar que cidadão não acessa a logística interna |
| 3. Arquitetura | `VacinaController` → `CadastroService` → `VacinaRepository` | “O Controller recebe o contrato; o Service verifica as condições e coordena a transação.” |
| 4. Consultas JPA | Query Method de leitura, `@Query` de ocorrência e Filtros | Mostrar por que cada forma atende uma necessidade; paginar e ordenar com `campos` permitidos |
| 5. Erros e validação | Enviar nome vazio ou sort não permitido | Esperar 400 e ProblemDetail; não usar falhas artificiais de produção para mostrar 500 |
| 6. Identidade e JWT | Login, Authorize e /auth/me | Não capturar senha/`token`; explicar assinatura, expiração e reconsulta do usuário |
| 7. CRUD | Criar/consultar/atualizar/excluir vacina descartável | Conferir 201/200/200/204 e 404 posterior; não excluir a vacina ligada ao lote |
| 8. Monitoramento térmico | Criar transporte/lote, iniciar e enviar 5.2, 7.5 e 9.3 em condições válidas | Explicar `NORMAL`, `ATENCAO` e bloqueio por faixa; data da leitura deve ser atual e válida |
| 9. Conferência humana | Receber transporte e decidir com gestor | O bloqueio persiste no recebimento; a decisão exige `justificativa` e não publica disponibilidade |
| 10. Cidadão e simulação | Confirmar/retirar disponibilidade, consultar com cidadão e verificar aplicação | Visão pública sem logística; tentativa não LIBERADA retorna 409 e mantém auditoria |
| 11. Isolamento | Funcionário B tenta acessar recurso da unidade A | Esperar 403; não oferecer bypass ou promover conta por SQL |
| 12. Integração e limites | Contrato Mobile, matriz da rubrica e limitações | “O contrato do backend está documentado; não temos comprovação de integração Mobile ponta a ponta. Cache, HATEOAS e Actuator não foram implementados.” |

Para mostrar aplicação rejeitada, use um lote não `LIBERADO` e consulte seu histórico com usuário autorizado. Para o caso permitido, use outro lote liberado após recebimento e decisão. Normalização e recebimento não são atalhos de liberação.

A infraestrutura e o vídeo DevOps têm documentação própria. Não é necessário reprovisionar Azure, reconstruir containers ou reiniciar o banco para explicar a camada Java.

## Estudo — perguntas e respostas técnicas

| Pergunta | Resposta ligada ao VaciSmart |
|---|---|
| O que é Spring Boot? | Organiza a inicialização e a configuração da aplicação Spring. No projeto, os starters integram HTTP, JPA, validação e segurança, e os perfis escolhem banco e documentação. |
| Por que usamos JPA? | Para mapear o domínio Java para tabelas e trabalhar com relações, transações e consultas. Isso não elimina SQL: FKs, constraints e a consulta agregada de disponibilidade continuam importantes. |
| Qual é a diferença entre Entity e DTO? | `Lote` representa estado persistido e suas regras; `LoteEntradaDto` descreve só o que a API aceita no cadastro. Separar os dois impede que o cliente envie estado, autor ou relações internas livremente. |
| O que fica no Controller e no Service? | O Controller traduz HTTP e DTOs. O Service coordena autorização, transação, consultas e regras de negócio; as entidades mantêm invariantes. Classificar leitura no Controller deixaria o fluxo preso ao HTTP. |
| Query Method, JPQL ou `Specification`? | Query Method resolve a leitura por transporte com condição fixa. JPQL expressa a consulta de ocorrências com estado opcional. `Specification` combina filtros de lote/transporte sem criar um método para cada combinação. |
| Por que usar `Page` e `Pageable`? | Para limitar e ordenar a consulta no banco, junto da contagem. `PaginaDto` dá ao cliente um contrato estável; não carregamos tudo para depois recortar uma página em memória. |
| LAZY é sempre mais rápido? | Não automaticamente. Evita carregar relações desnecessárias, mas pode gerar consultas repetidas quando o fluxo precisa delas. O EntityGraph de lotes carrega vacina para a classificação; não há benchmark geral comprovado. |
| Por que não usar cascade? | `Vacina`, transporte e lote têm ciclos próprios. Excluir ou salvar um filho não deve propagar operações aos pais nem apagar o histórico. Serviços e FKs controlam esses vínculos. |
| Como funcionam transação e rollback? | Estado e auditoria do fluxo normalmente são gravados juntos. Se houver falha transacional, o rollback impede alterações parciais. Na aplicação rejeitada, o Service retorna antes de o Controller produzir o 409, preservando o evento. |
| Por que usar lock pessimista e refresh? | Duas operações sobre o mesmo transporte não devem decidir com estados incompatíveis. O lock serializa o fluxo, e o refresh evita reutilizar uma entidade carregada antes de uma mudança concorrente; a unidade é revalidada. |
| Bean Validation substitui regra de negócio? | Não. `@NotBlank` e `@NotNull` validam `campos`, mas só o domínio/Service sabe se o transporte foi recebido ou se a vacina já tem lotes. Essas condições podem exigir consulta ao banco. |
| O que faz `@RestControllerAdvice`? | Centraliza a tradução de exceções em respostas. `ErroApiAdvice` retorna ProblemDetail, mas falhas no filtro de segurança também precisam de `ErrosSeguranca`, pois podem ocorrer antes do Controller. |
| Qual é a diferença entre 401 e 403? | 401 indica autenticação ausente ou inválida; 403 indica acesso negado para perfil ou unidade. O funcionário também recebe 403 para lote/transporte inexistente, evitando revelar recursos de outra unidade. |
| JWT é uma senha criptografada? | Não. É um conjunto de claims assinado. A chave pública verifica se o `token` foi assinado pela privada correta; assinatura não esconde o conteúdo. Por isso não colocamos senhas ou segredos nas claims. |
| Por que consultar o banco se o JWT tem perfil? | O usuário pode ter sido desativado ou mudado de papel/unidade depois do login. `SegurancaConfig` e `AcessoService` usam os dados atuais; a claim de perfil não é a fonte final de privilégio. |
| O que BCrypt protege? | A senha é armazenada como hash com salt, usando custo 12. No login, PasswordEncoder compara a senha informada ao hash; a aplicação não precisa recuperar a senha original. |
| CORS substitui autenticação? | Não. Ele controla quais origens de navegador podem ler respostas. O JWT e o escopo de unidade continuam obrigatórios; uma chamada por Postman não demonstra o funcionamento do navegador Mobile. |
| Qual é o papel do Swagger? | Apresenta o contrato, os DTOs, permissões e respostas e permite testar operações com Bearer. Abrir a UI não autentica o usuário nem libera as rotas técnicas. |
| Como acontece o bloqueio preventivo? | `MonitoramentoService` compara a leitura com cada vacina e bloqueia os lotes afetados ainda em monitoramento. A pior classificação fica na leitura; normalização posterior não libera o lote. |
| Por que a decisão depende do gestor? | O sistema registra um indício térmico e exige conferência humana. `AcessoService`.gestor restringe LIBERAR/DESCARTAR; o fluxo exige recebimento e `justificativa` e audita a autoria, sem conclusão clínica automática. |
| O que significa `SEM_INFORMACAO`? | Não há confirmação pública válida nem retirada explícita que determine o outro estado. Não significa que a unidade está sem estoque, e não permite inferir se existe transporte ou lote interno. |
| Por que o teste H2 não encerra a validação PostgreSQL? | O perfil test cria o schema pelo Hibernate e não executa Flyway. Migrations, índice parcial e comportamento específico do PostgreSQL precisam de conferência nesse banco. |
| O que não foi implementado? | Spring Cache, HATEOAS e Actuator, além de credencial de dispositivo, finalização de transporte e recursos fora do MVP. Mobile ponta a ponta e persistência após reinício Azure não têm comprovação nesta entrega. |

## Declaração de uso de IA

Codex e ChatGPT foram utilizados como apoio ao desenvolvimento, à revisão técnica e à documentação. Codex auxiliou na estrutura do projeto, implementação de domínio/API/segurança/OpenAPI, testes, infraestrutura e validações registradas; ChatGPT apoiou a revisão do material e a organização das entregas. Os resultados de testes citados são os efetivamente registrados e não foram repetidos nesta reestruturação documental.
