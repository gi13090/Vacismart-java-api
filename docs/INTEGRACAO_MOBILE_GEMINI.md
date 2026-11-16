# VaciSmart — Contrato de integração Mobile/Gemini da J4

Consolidação documental: 15/11/2026. Backend corrente: branch master, HEAD 8fea0bebd1dadf6c9197d12eddc64fb0bf2ef130, com J5-A/B/C/D implementadas. J5-D foi commitada nesse HEAD e sua consulta informativa foi validada no PostgreSQL real na J5-E em 14/11/2026. J5-E aprovada tecnicamente, aguardando encerramento documental; J5 integral incompleta, com D-053/OpenAPI aberta. O corpo original permanece como registro da J4 de 12/11/2026, aprovada pelo responsável, cuja referência era 324b87484e5f2d8f23de59a024c8165dfc93c814; os adendos datados preservam a evolução J5. Integração Flutter/Arduino, demais funcionalidades futuras e implantação DevOps continuam pendentes.

O arquivo externo `VaciSmart_Integracao_Mobile_Gemini_J4_2026-11-10.md` foi informado pelo responsável, mas não localizado no workspace. Não foi indicado outro diretório acessível para esse arquivo e não foram pesquisados diretórios privados indiscriminadamente. Esta versão foi reconstruída a partir de seis controllers, DTOs, serviços, segurança e documentação presentes no repositório. Nenhuma equivalência com o contrato original da equipe `02_MODELO_E_CONTRATOS.md` foi comprovada; esse arquivo continua ausente e **D-002 permanece aberta**. O uso real com o aplicativo ainda exige revisão e validação bilateral.

## 1. Produto e tecnologias

Protótipo acadêmico FIAP Global Solution 2026/2, CitizenTech, para monitoramento térmico de lotes de vacinas em transporte simulado, registro de ocorrências e bloqueio preventivo. Somente dados fictícios; não é sistema clínico, não confirma degradação nem autoriza aplicação real. Beneficiário final: cidadão; operação interna: funcionário e gestor.

Backend Java 21, Spring Boot 4.1.2, Spring Security Resource Server/Nimbus JWT RS256, BCrypt custo 12, JPA/Hibernate, PostgreSQL 17.6 e Flyway V0/V1/V2. Maven Wrapper 3.3.4/Maven 3.9.11. Testes usam H2 em memória com Flyway desabilitado, não PostgreSQL. OpenAPI/Swagger permanece planejado. Spring Cache, Spring HATEOAS e Spring Boot Actuator foram excluídos definitivamente pelo responsável (D-046), inclusive de próximos prompts, J5/J6/J7 e critérios internos de aceite.

Fontes internas: [README](../README.md), [decisões](../DUVIDAS.md), [especificação funcional](ESPECIFICACAO.md), `src/main/java/br/com/fiap/vacismart/web/controller`, `web/dto`, `service` e `security`. A especificação contém funcionalidades planejadas que ainda não são endpoints disponíveis; a matriz desta versão é o inventário implementado.

## 2. Base URL, ambiente e conectividade

Todas as rotas de produção começam por **`/api/v1`**. Configure a base URL do cliente por ambiente, sem fixar endereço de professor, colega ou serviço ainda inexistente. Se a base já terminar em /api/v1, acrescente apenas a rota relativa da matriz, evitando duplicar o prefixo.

| Ambiente de consumo | Base / condição |
|---|---|
| Cliente no mesmo host da API | `http://127.0.0.1:8080/api/v1`, salvo porta local configurada de outra forma |
| Emulador | Resolver o endereço do host segundo a plataforma do emulador; validar conectividade antes de autenticar. localhost/127.0.0.1 dentro de um dispositivo não identifica automaticamente o computador da API. |
| Aparelho físico na rede | Exige endereço alcançável e configuração de exposição autorizada do backend. O bind atual da API é 127.0.0.1, portanto não está acessível por outro aparelho apenas trocando a URL. |
| Ambiente hospedado | Pendente; não existe URL de produção confirmada neste contrato. Configurar somente quando houver implantação e endereço autorizados. |

A API continua vinculada a loopback. O Compose publica **o banco**, não a API, em 127.0.0.1:5432; Mobile nunca deve conectar diretamente ao PostgreSQL. Não alterar backend, firewall, TLS ou cleartext do aplicativo silenciosamente para forçar conectividade. Em ambiente público futuro, a configuração de transporte seguro faz parte da infraestrutura ainda não validada.

Antes do startup dev, o responsável pelo backend configura banco, JDK e o par RSA externo em JWT_CHAVE_PUBLICA/JWT_CHAVE_PRIVADA. A chave privada fica somente no backend; o aplicativo não recebe nem gera a chave de assinatura. Este documento não fornece credenciais. Contas de funcionário/gestor são provisionadas localmente pelo mecanismo opt-in existente, sob controle do responsável; não existe cadastro público privilegiado.

CORS importa para Flutter Web: CORS_ORIGENS recebe origens http(s) exatas, sem caminho ou wildcard; vazio por padrão. Authorization/Content-Type são aceitos e Location é exposto para origens permitidas, sem cookies. Flutter nativo continua exigindo JWT, mesmo sem depender da política CORS do navegador. Uma rejeição CORS pode ser 403 sem ProblemDetail.

## 3. Autenticação e perfis

Somente **POST /auth/cadastro e POST /auth/login** são públicos. Demais rotas/métodos exigem autenticação; os recursos operacionais exigem FUNCIONARIO/GESTOR. Enviar em chamadas protegidas:

```http
Authorization: Bearer <token-retornado-pelo-login>
Content-Type: application/json
```

Cadastro cria exclusivamente CIDADAO; não envie perfil, ativo, id, senhaHash ou unidadeSaudeId. Campos desconhecidos são rejeitados com 400. Cadastro retorna 201 com UsuarioSaidaDto e Location `/api/v1/auth/me`; para seguir esse Location é necessário fazer login, pois cadastro não emite token.

Login retorna LoginSaidaDto com token/tipo/expiraEm/usuario. A validade padrão é 15 minutos, configurável no backend por duração positiva de até uma hora, sem tolerância adicional na expiração. Claims: sub UUID, perfil, iss, iat e exp; assinatura RS256. O backend verifica usuário ativo e perfil persistido em cada requisição; perfil no token é informativo. Desativação/mudança de perfil ou unidade vale na próxima requisição, inclusive com token antigo. Uma operação já iniciada não é cancelada por essa garantia.

Não existe refresh, logout de servidor, recuperação de senha, revogação individual de token ou CRUD público de usuários/perfis/vínculos. No cliente, sair remove o token local, mas não revoga sua validade no servidor. Tratar 401 solicitando nova autenticação; não repetir login infinitamente nem registrar token/senha em logs. Não armazenar a senha para fazer renovação automática inventada. A interface por perfil ajuda o usuário, mas a autorização é sempre decidida pelo backend.

| Perfil | Escopo |
|---|---|
| CIDADAO | Cadastro/login e própria conta. Não acessa unidades, catálogo operacional de vacinas, lotes, transportes, leituras, dispositivo, ocorrências ou auditoria. Não há localização/rota de transporte para cidadão ou qualquer outro perfil. |
| FUNCIONARIO | Unidade vinculada obrigatória; opera transportes/lotes permitidos, leituras, ocorrências e históricos somente desse escopo. Consulta catálogo de vacinas. Não cria/edita/exclui unidades/vacinas nem cria lotes. |
| GESTOR | Acesso operacional a todas as unidades no marco J4; cadastros globais e POST /lotes exclusivos. Continua sujeito às regras de estado, vínculos, bloqueio e preservação de histórico. |

Quatro contas demonstrativas representam três perfis: cidadão, funcionário A, funcionário B e gestor. Não existe quarto perfil humano de dispositivo. A ingestão atual simula operação humana autenticada; autenticação própria Arduino/gateway continua pendente.

## 4. Matriz integral dos 29 pares método/rota

Rotas abaixo são relativas a /api/v1. F = FUNCIONARIO; G = GESTOR. "F no escopo" exige unidade própria e controles de estado. C = CIDADAO. Todos os corpos de entrada usam JSON; listagens retornam PaginaDto do tipo indicado. POSTs de criação incluem Location; DELETE e início de monitoramento bem-sucedidos não retornam corpo.

| Nº | Método | Rota | Entrada / filtros específicos | Saída / sucesso | Permissão e regra |
|---:|---|---|---|---|---|
| 1 | POST | /auth/cadastro | CadastroEntradaDto | UsuarioSaidaDto; 201 | Público; somente C |
| 2 | POST | /auth/login | LoginEntradaDto | LoginSaidaDto; 200 | Público; conta ativa/credenciais corretas |
| 3 | GET | /auth/me | Sem corpo | UsuarioSaidaDto; 200 | C/F/G, própria conta |
| 4 | POST | /unidades | UnidadeEntradaDto | UnidadeSaidaDto; 201 | G |
| 5 | GET | /unidades | page, size, sort | `PaginaDto<UnidadeSaidaDto>`; 200 | F: própria unidade; G: todas |
| 6 | GET | /unidades/{id} | UUID id | UnidadeSaidaDto; 200 | F no escopo / G |
| 7 | PUT | /unidades/{id} | UnidadeEntradaDto | UnidadeSaidaDto; 200 | G |
| 8 | DELETE | /unidades/{id} | UUID id | Sem corpo; 204 | G; sem transportes, histórico ou vínculos impeditivos |
| 9 | POST | /vacinas | VacinaEntradaDto | VacinaSaidaDto; 201 | G |
| 10 | GET | /vacinas | page, size, sort | `PaginaDto<VacinaSaidaDto>`; 200 | F/G |
| 11 | GET | /vacinas/{id} | UUID id | VacinaSaidaDto; 200 | F/G |
| 12 | PUT | /vacinas/{id} | VacinaEntradaDto | VacinaSaidaDto; 200 | G; parâmetros só mudam sem lotes; renomear com faixa igual é permitido |
| 13 | DELETE | /vacinas/{id} | UUID id | Sem corpo; 204 | G; sem lotes/histórico |
| 14 | POST | /transportes | TransporteEntradaDto | TransporteSaidaDto; 201 | F no escopo / G; inicia CADASTRADO |
| 15 | GET | /transportes | unidadeId, estado, page, size, sort | `PaginaDto<TransporteSaidaDto>`; 200 | F no escopo / G |
| 16 | GET | /transportes/{id} | UUID id | TransporteSaidaDto; 200 | F no escopo / G |
| 17 | PUT | /transportes/{id} | TransporteEntradaDto | TransporteSaidaDto; 200 | F no escopo / G; somente CADASTRADO; F não transfere unidade |
| 18 | DELETE | /transportes/{id} | UUID id | Sem corpo; 204 | F no escopo / G; CADASTRADO, sem lotes/leituras/ocorrências/histórico |
| 19 | POST | /transportes/{id}/monitoramento | Sem corpo | Sem corpo; 204 | F no escopo / G; CADASTRADO e pelo menos um lote |
| 20 | POST | /lotes | LoteEntradaDto | LoteSaidaDto; 201 | G; transporte CADASTRADO |
| 21 | GET | /lotes | transporteId, vacinaId, estado, page, size, sort | `PaginaDto<LoteSaidaDto>`; 200 | F no escopo / G |
| 22 | GET | /lotes/{id} | UUID id | LoteSaidaDto; 200 | F no escopo / G |
| 23 | PUT | /lotes/{id} | LoteAtualizacaoDto | LoteSaidaDto; 200 | F no escopo / G; só código, EM_MONITORAMENTO e transporte CADASTRADO |
| 24 | GET | /lotes/{id}/historico | page, size, sort | `PaginaDto<EventoSaidaDto>`; 200 | F no escopo / G; eventos somente desse lote |
| 25 | POST | /transportes/{id}/leituras | LeituraEntradaDto | LeituraSaidaDto; 201 | F no escopo / G; simulação, EM_TRANSPORTE; transporteId do corpo igual à rota |
| 26 | GET | /transportes/{id}/leituras | page, size, sort | `PaginaDto<LeituraSaidaDto>`; 200 | F no escopo / G |
| 27 | GET | /transportes/{id}/leituras/{leituraId} | UUIDs id/leituraId | LeituraSaidaDto; 200 | F no escopo / G; leitura pertence ao transporte |
| 28 | GET | /transportes/{id}/ocorrencias | estado, page, size, sort | `PaginaDto<OcorrenciaSaidaDto>`; 200 | F no escopo / G; JPQL com estado opcional |
| 29 | GET | /transportes/{id}/historico | page, size, sort | `PaginaDto<EventoSaidaDto>`; 200 | F no escopo / G; eventos somente da entidade Transporte |

Nenhum DELETE de lote; nenhum endpoint de encerramento/escrita em ocorrências; nenhum CRUD público de auditoria. Rota de falha usada em src/test não integra esta matriz nem o produto. OPTIONS de preflight é tratamento CORS, não um endpoint funcional adicional.

## 5. DTOs de entrada e validações reais

Todos os campos listados como obrigatórios são exigidos; não envie campos extras. UUIDs no JSON/rota são strings válidas, não inteiros. Texto @NotBlank não pode ser nulo, vazio ou só espaços. A validação não autoriza remover silenciosamente espaços da senha.

| DTO | Campos exatos e validação |
|---|---|
| CadastroEntradaDto | nome: string obrigatória até 150; email: string obrigatória, formato e-mail, até 254; senha: obrigatória, 12–72 caracteres ASCII imprimíveis (U+0020 a U+007E) |
| LoginEntradaDto | email: obrigatório, formato e-mail, até 254; senha: obrigatória, até 72 caracteres ASCII imprimíveis; o DTO de login não impõe o mínimo 12 do cadastro |
| UnidadeEntradaDto | nome: string obrigatória até 150 |
| VacinaEntradaDto | nome: string obrigatória até 150; temperaturaMinC e temperaturaMaxC: decimais obrigatórios de -9999999.999 a 9999999.999; margemAtencaoC: decimal obrigatório de 0 a 9999999.999 |
| TransporteEntradaDto | codigo: string obrigatória até 60; unidadeSaudeId: UUID obrigatório |
| LoteEntradaDto | codigo: string obrigatória até 60; vacinaId e transporteId: UUIDs obrigatórios |
| LoteAtualizacaoDto | codigo: string obrigatória até 60; não aceita troca de vacina/transporte |
| LeituraEntradaDto | transporteId: UUID obrigatório, igual à rota; dispositivoId: string obrigatória até 100; temperaturaC: decimal obrigatório de -9999999.999 a 9999999.999; medidaEm: data/hora obrigatória com offset |

E-mail é normalizado por strip e minúsculas; duplicidade retorna 409, inclusive com caixa diferente. O cadastro não retorna senha/hash. Temperaturas/parâmetros cabem em numeric(10,3): até sete dígitos inteiros e três casas significativas após remover zeros finais; 5.2000 é aceito, 8.0001 é rejeitado. Não arredondar antes de enviar para contornar validação. min < max, margem >= 0 e 2 × margem <= max − min. A margem é obrigatória no DTO, não há default aplicado à entrada REST.

Estado, disponibilidade, classificacao, autor, recebidaEm e id são calculados/controlados pelo backend quando não pertencem ao DTO de entrada. Enviá-los como extras retorna 400. Não existe PATCH; PUT utiliza o DTO completo indicado na matriz.

## 6. DTOs de saída, UUIDs, datas e enums

Os campos abaixo são exatamente os records de saída; não há objetos entidade aninhados nem dependência de HATEOAS. UUID é string opaca e deve ser guardado como tal. Identificadores demonstrativos nas próximas seções são fictícios: substitua pelos retornados pela API.

| DTO | Campos JSON (tipo); nulabilidade relevante |
|---|---|
| UsuarioSaidaDto | id (UUID), nome (string), email (string), perfil (enum), unidadeSaudeId (UUID ou null para CIDADAO/GESTOR) |
| LoginSaidaDto | token (string), tipo (string "Bearer"), expiraEm (Instant ISO-8601), usuario (UsuarioSaidaDto) |
| UnidadeSaidaDto | id (UUID), nome (string) |
| VacinaSaidaDto | id (UUID), nome (string), temperaturaMinC (decimal), temperaturaMaxC (decimal), margemAtencaoC (decimal) |
| TransporteSaidaDto | id (UUID), codigo (string), unidadeSaudeId (UUID), estado (enum), inicioMonitoramentoEm (OffsetDateTime ou null enquanto não iniciado) |
| LoteSaidaDto | id (UUID), codigo (string), vacinaId (UUID), transporteId (UUID), estado (enum), disponibilidade (enum) |
| LeituraSaidaDto | id (UUID), transporteId (UUID), dispositivoId (string), temperaturaC (decimal), medidaEm (OffsetDateTime), recebidaEm (OffsetDateTime), classificacao (enum) |
| OcorrenciaSaidaDto | id (UUID), transporteId (UUID), tipo (enum), estado (enum), abertaEm (OffsetDateTime), encerradaEm (OffsetDateTime ou null quando aberta) |
| EventoSaidaDto | id (UUID), entidade (string), referenciaId (UUID), acao (string), autor (string), dataHora (OffsetDateTime), justificativa (string ou null) |
| PaginaDto<T> | conteudo (lista de T), pagina (inteiro), tamanho (inteiro), totalElementos (long), totalPaginas (inteiro) |

OffsetDateTime: ISO-8601 com offset explícito, por exemplo `2026-11-12T12:00:00-03:00`, ou Z para UTC. expiraEm usa Instant, normalmente com Z. O serializador pode normalizar o offset mantendo o mesmo instante; não compare o texto literalmente. O fuso de referência do projeto é America/Sao_Paulo. medidaEm é o instante da medição; recebidaEm é atribuído pelo backend. A medição deve estar entre o início do monitoramento e o instante de recebimento, inclusive; futura ou anterior ao início retorna 400.

| Enum | Valores modelados |
|---|---|
| Perfil | CIDADAO, FUNCIONARIO, GESTOR |
| EstadoTransporte | CADASTRADO, EM_TRANSPORTE, RECEBIDO, FINALIZADO |
| EstadoLote | EM_MONITORAMENTO, AGUARDANDO_CONFERENCIA, BLOQUEADO_PREVENTIVAMENTE, LIBERADO, DESCARTADO |
| DisponibilidadeLote | NAO_DISPONIVEL, CONFIRMADA |
| ClassificacaoTemperatura | NORMAL, ATENCAO, FORA_DA_FAIXA |
| TipoOcorrencia | FORA_DA_FAIXA, PERDA_DE_TELEMETRIA |
| EstadoOcorrencia | ABERTA, ENCERRADA |

Enum modelado não implica transição implementada. Na J4, transporte inicia CADASTRADO e pode ir a EM_TRANSPORTE; lote inicia EM_MONITORAMENTO/NAO_DISPONIVEL e pode ser bloqueado preventivamente. A J5-A acrescenta somente o comando de recebimento descrito no adendo abaixo. A J5-B acrescenta a decisão humana LIBERAR/DESCARTAR no adendo ao final. A J5-C acrescenta confirmação/retirada de disponibilidade interna no adendo ao final. Não existe comando REST para finalizar transporte. Tratar valor inesperado em resposta como dado desconhecido da versão, sem inventar transição no cliente.

## 7. Exemplos JSON fictícios

Nome/e-mail/senha de exemplo são marcadores locais, não contas prontas. Os corpos são compatíveis com os campos/validações; horários, IDs e token de resposta devem ser substituídos pelos resultados da execução. Nenhuma chamada deste documento foi executada na consolidação.

Cadastro (POST /auth/cadastro), 201 com Location /api/v1/auth/me:

```json
{"nome":"Cidadão fictício","email":"cidadao@demo.invalid","senha":"<senha-ficticia-local>"}
```

```json
{"id":"88888888-8888-4888-8888-888888888888","nome":"Cidadão fictício","email":"cidadao@demo.invalid","perfil":"CIDADAO","unidadeSaudeId":null}
```

Login (POST /auth/login) e resposta 200:

```json
{"email":"cidadao@demo.invalid","senha":"<senha-ficticia-local>"}
```

```json
{"token":"<token-retornado-pelo-login>","tipo":"Bearer","expiraEm":"2026-11-12T15:15:00Z","usuario":{"id":"88888888-8888-4888-8888-888888888888","nome":"Cidadão fictício","email":"cidadao@demo.invalid","perfil":"CIDADAO","unidadeSaudeId":null}}
```

GET /auth/me retorna o mesmo formato UsuarioSaidaDto, sem token ou senha. Nos exemplos operacionais seguintes, use uma conta GESTOR para cadastrar unidade, vacina e lote; uma conta CIDADAO será negada.

POST /unidades → UnidadeSaidaDto:

```json
{"nome":"Unidade Demonstrativa 01"}
```

```json
{"id":"11111111-1111-4111-8111-111111111111","nome":"Unidade Demonstrativa 01"}
```

POST /vacinas e PUT /vacinas/{id} usam o mesmo DTO:

```json
{"nome":"Vacina Demo A","temperaturaMinC":2.0,"temperaturaMaxC":8.0,"margemAtencaoC":1.0}
```

```json
{"id":"22222222-2222-4222-8222-222222222222","nome":"Vacina Demo A","temperaturaMinC":2.0,"temperaturaMaxC":8.0,"margemAtencaoC":1.0}
```

POST /transportes e PUT /transportes/{id} usam TransporteEntradaDto; a resposta de criação é:

```json
{"codigo":"TRP-DEMO-001","unidadeSaudeId":"11111111-1111-4111-8111-111111111111"}
```

```json
{"id":"33333333-3333-4333-8333-333333333333","codigo":"TRP-DEMO-001","unidadeSaudeId":"11111111-1111-4111-8111-111111111111","estado":"CADASTRADO","inicioMonitoramentoEm":null}
```

POST /lotes exige GESTOR, com transporte ainda CADASTRADO:

```json
{"codigo":"VAC-DEMO-001","vacinaId":"22222222-2222-4222-8222-222222222222","transporteId":"33333333-3333-4333-8333-333333333333"}
```

```json
{"id":"44444444-4444-4444-8444-444444444444","codigo":"VAC-DEMO-001","vacinaId":"22222222-2222-4222-8222-222222222222","transporteId":"33333333-3333-4333-8333-333333333333","estado":"EM_MONITORAMENTO","disponibilidade":"NAO_DISPONIVEL"}
```

PUT /lotes/{id}, antes do monitoramento, aceita somente:

```json
{"codigo":"VAC-DEMO-001-CORRIGIDO"}
```

POST /transportes/{id}/monitoramento é enviado sem corpo; sucesso 204. Consulte o transporte para obter inicioMonitoramentoEm. Somente então envie uma leitura; o horário abaixo é ilustrativo e deve ser trocado por um instante válido, posterior/igual ao início e não futuro:

```json
{"transporteId":"33333333-3333-4333-8333-333333333333","dispositivoId":"ARDUINO-DEMO-01","temperaturaC":9.3,"medidaEm":"2026-11-12T12:00:00-03:00"}
```

Resposta 201 de POST /transportes/{id}/leituras, Location da leitura sob o transporte:

```json
{"id":"55555555-5555-4555-8555-555555555555","transporteId":"33333333-3333-4333-8333-333333333333","dispositivoId":"ARDUINO-DEMO-01","temperaturaC":9.3,"medidaEm":"2026-11-12T12:00:00-03:00","recebidaEm":"2026-11-12T12:00:01-03:00","classificacao":"FORA_DA_FAIXA"}
```

Uma ocorrência correspondente tem formato:

```json
{"id":"66666666-6666-4666-8666-666666666666","transporteId":"33333333-3333-4333-8333-333333333333","tipo":"FORA_DA_FAIXA","estado":"ABERTA","abertaEm":"2026-11-12T12:00:01-03:00","encerradaEm":null}
```

Evento no histórico do lote, retornado dentro de `PaginaDto<EventoSaidaDto>`:

```json
{"id":"77777777-7777-4777-8777-777777777777","entidade":"Lote","referenciaId":"44444444-4444-4444-8444-444444444444","acao":"BLOQUEADO_PREVENTIVAMENTE","autor":"SISTEMA","dataHora":"2026-11-12T12:00:01-03:00","justificativa":"Temperatura fora da faixa da vacina; leitura 55555555-5555-4555-8555-555555555555"}
```

## 8. Paginação, filtros e ordenação

Todas as listagens da matriz usam page base zero, size padrão 20 e máximo 100. O resolver Spring normaliza page negativo/inválido para 0 e size inválido/não positivo para o padrão; tamanho acima do máximo é limitado a 100. Não usar essa normalização para validar formulários do cliente silenciosamente. sort aceita `campo,asc`/`campo,desc`, repetível; campo fora da lista permitida retorna 400. Sem sort usa id; id desempata outras ordenações, não representa ordem cronológica.

| Recurso | Filtros específicos opcionais | Campos de sort permitidos |
|---|---|---|
| Unidades / vacinas | Nenhum | id, nome |
| Transportes | unidadeId (UUID), estado (EstadoTransporte) | id, codigo, estado, inicioMonitoramentoEm |
| Lotes | transporteId (UUID), vacinaId (UUID), estado (EstadoLote) | id, codigo, estado |
| Leituras | Transporte obrigatório na rota | id, medidaEm, recebidaEm, temperaturaC, classificacao |
| Ocorrências | Transporte na rota; estado (EstadoOcorrencia) opcional | id, abertaEm, encerradaEm, tipo, estado |
| Históricos | Lote/transporte na rota | id, dataHora, acao |

Filtros combinam com AND; nenhum filtro ou campo de ordenação adicional está implementado. Exemplo: `/lotes?transporteId=33333333-3333-4333-8333-333333333333&estado=EM_MONITORAMENTO&page=0&size=10&sort=codigo,asc`. UUIDs fora do escopo são negados, não uma forma de ampliar a listagem. Em particular, filtro transporteId passa por verificação de acesso ao pai e pode retornar 403/404; não garantir página vazia para todo UUID inexistente. Ausência de resultados autorizados retorna conteudo vazio.

```json
{"conteudo":[{"id":"11111111-1111-4111-8111-111111111111","nome":"Unidade Demonstrativa 01"}],"pagina":0,"tamanho":20,"totalElementos":1,"totalPaginas":1}
```

Tratar totalElementos como inteiro de contagem compatível com long; não obter contagens globais por endpoints paralelos para contornar o escopo. Unidade é aplicada no SQL antes da paginação/contagem; não é filtragem em memória no cliente.

## 9. HTTP, erros e isolamento por unidade

| Código | Significado e tratamento |
|---|---|
| 200 | Consulta, login ou atualização com corpo do DTO |
| 201 | Cadastro/criação com corpo e Location relativo; resolver Location contra a origem da API |
| 204 | Início de monitoramento ou exclusão permitida; não tentar decodificar JSON vazio |
| 400 | JSON, campo desconhecido, DTO/UUID/enum/data/decimal inválido, sort não permitido ou transporteId divergente |
| 401 | Sem Bearer, JWT inválido/expirado, conta inativa/inexistente ou credenciais incorretas; WWW-Authenticate: Bearer |
| 403 | Perfil/unidade não autorizado; não insistir trocando IDs. Pode ser rejeição CORS sem corpo ProblemDetail. |
| 404 | Recurso ausente para gestor ou filho ausente sob pai autorizado; rota inexistente |
| 409 | Estado incompatível, duplicidade, referência/histórico impeditivo ou conflito de integridade/concorrência |
| 405 / 415 | Método ou mídia não suportados no contexto em que o MVC é alcançado |
| 500 | Falha inesperada genérica; não exibir detalhes internos nem prometer que retentar escrita é seguro |

Erro usual: Content-Type application/problem+json, com type/title/status/detail. MVC normalmente acrescenta instance e validação de DTO acrescenta campos. Não exigir instance em todos os erros: os filtros 401/403 retornam o conjunto básico. Não há formato de sucesso envolvido em propriedade data.

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Campos inválidos","instance":"/api/v1/unidades","campos":[{"campo":"nome","mensagem":"não deve estar em branco"}]}
```

```json
{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Autenticação necessária ou inválida"}
```

Mensagens de validação podem variar com o idioma do Validator; o cliente deve decidir pelo status e nome do campo, não por igualdade textual da mensagem. Mostrar mensagens adequadas sem registrar corpo de login, token ou senha.

FUNCIONARIO só consulta sua unidade. Em transporte/lote alheio **ou inexistente**, a resposta é 403 uniforme para não revelar existência; GESTOR recebe 404 para ausente. Filtro explícito de outra unidade/transporte é negado. Leitura inexistente ou que pertença a outro transporte sob pai autorizado retorna 404. Mutação recarrega o transporte sob lock e revalida a unidade, impedindo uso de unidade antiga após transferência concorrente. O cliente não manda perfil/unidade para decidir autorização; unidadeSaudeId em TransporteEntradaDto é vínculo a ser autorizado, não prova de permissão.

## 10. Monitoramento térmico, ocorrências e histórico

Sequência operacional: G cria unidade → vacina → transporte CADASTRADO → lote; F autorizado/G inicia monitoramento → envia leituras → consulta lote, ocorrências e históricos. Não cadastrar lote após o início. Não editar transporte após o início nem código de lote bloqueado. Não alterar faixa da vacina quando já há lotes; parâmetros numericamente iguais com renomeação são permitidos.

Faixas são configuradas por vacina. Demo A (min=2, max=8, margem=1): NORMAL de 3 a 7 inclusive; ATENCAO de 2 até menos de 3 e acima de 7 até 8; FORA_DA_FAIXA abaixo de 2 ou acima de 8. Atenção registra a classificação sem bloquear. Desvio abre no máximo uma ocorrência térmica ABERTA por transporte/tipo e bloqueia somente lotes EM_MONITORAMENTO afetados pela faixa da própria vacina, mantendo NAO_DISPONIVEL. Transporte misto registra na leitura a pior classificação; ela não autoriza tratar todos os lotes como afetados.

Normalização não encerra ocorrência térmica, não libera nem descarta lotes e não confirma disponibilidade. Auditoria de monitoramento usa autor SISTEMA. GETs são consultas, não comandos de mudança de estado. Histórico do transporte mostra eventos da entidade Transporte; eventos dos lotes são consultados em cada lote, não numa timeline agregada inexistente.

Perda de telemetria: método interno verifica mais de três minutos desde o último recebimento/início sem leitura (limite configurável); exatamente três minutos não abre. Ela não bloqueia sozinha; retomada encerra apenas a ocorrência de telemetria e audita. Não há endpoint nem agendador de verificação, nem endpoint de encerramento. Não prometer ao Mobile que um relógio de tela dispara regras do servidor ou libera lote.

## 11. Orientações para Flutter e uso direto pelo Gemini

Centralize base URL por ambiente e a montagem de cabeçalhos numa camada HTTP do cliente. Gere modelos Dart com os nomes JSON portugueses exatos; use String para UUID, num/decimal com cuidado para temperaturas e DateTime.parse para ISO-8601, preservando o instante ao exibir no fuso escolhido. Não arredondar medidas para passar na validação. Campos null dos DTOs devem ser tolerados. Não enviar propriedade futura de tela no payload REST.

Separe autenticação, conta e operação por perfil. Para CIDADAO, cadastro/login/conta são reais na J4 e a consulta GET de disponibilidade passa a existir na J5-D conforme adendo ao final. Telas futuras de caderneta podem usar mocks **explicitamente identificados**, em fonte de dados distinta, sem chamar endpoints inventados e sem misturar dados operacionais restritos. Não criar telas de localização de transporte, lote ou telemetria para cidadão. A aplicação não conecta ao banco e não manipula chave privada JWT.

Trate a resposta conforme status antes de decodificar: 204 não tem JSON; 400 pode trazer campos; 401 exige autenticar novamente; 403 é permissão/CORS; 409 exige revisar o estado. Trate página vazia e campos nulos. Não presumir idempotência de POST de leitura: cada envio aceito persiste uma nova leitura. Não reenvie automaticamente mutações após erro de rede sem consultar estado e decidir com o usuário/contrato. Logout é local; não inventar /auth/refresh ou /auth/logout.

Texto de orientação para o colega usar com o Gemini:

> Use esta versão do contrato, os DTOs exatos da J4 e os adendos J5 implementados como limites do backend existente. Implemente somente a camada Flutter solicitada pelo responsável Mobile. Preserve nomes JSON, /api/v1, UUIDs, datas com offset, paginação e a matriz de permissões. Configure a base URL por ambiente, envie Bearer e trate 204/ProblemDetail/CORS. Não crie endpoints ou campos novos nem altere o backend para adaptar uma tela. Cidadão tem cadastro/login/própria conta e GET /api/v1/disponibilidade conforme adendo J5-D; caderneta continua futura e só pode usar mocks identificados. Nunca exponha recursos operacionais ao cidadão. Não use credenciais reais ou tokens completos em exemplos/logs. Cache, HATEOAS e Actuator estão definitivamente excluídos do backend. Registre incompatibilidades e leve-as à validação conjunta, sem presumir que D-002 foi encerrada.

Roteiro de integração conjunta ainda a executar: conferir versão/base URL e conectividade; cadastro cidadão → login → me; confirmar negações operacionais; usar contas operacionais fornecidas privadamente pelo responsável para listar apenas a unidade; validar DTO/página/erros e o fluxo térmico permitido; comparar este documento com o contrato original da equipe e registrar diferenças antes de qualquer alteração. Não executar esse roteiro com dados reais ou transformar esses passos propostos em evidência de teste já concluído.

## 12. Limites, funcionalidades futuras e validações anteriores

Ainda não implementados: finalização posterior, aplicações de doses, caderneta digital, autenticação própria de dispositivos, integração efetiva Arduino/Flutter, Swagger/OpenAPI e implantação DevOps/Azure. Não há refresh ou CRUD público de usuários. Não inventar /aplicacoes como rota existente por aparecer na especificação futura; a decisão técnica J5-B e o POST interno de disponibilidade J5-C estão nos adendos abaixo; GET /api/v1/disponibilidade foi implementado na J5-D, conforme adendo ao final.

Fora do MVP: GPS/rastreamento, localização/rotas de transporte, agendamento/reserva, SUS/RNDS, notificações externas, QR Code, IA e dados clínicos reais. Os três recursos Spring excluídos na seção 1 não são requisitos futuros.

Evidências **históricas**, anteriores à elaboração deste documento, em 12/11/2026:

| Verificação | Resultado / limite |
|---|---|
| Suíte J4 às 00:03:56 | 180 testes, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS, 57.854 s; H2 |
| Auditoria preventiva posterior | 180/0/0/0, BUILD SUCCESS, 56.235 s; código idêntico em cópia temporária, H2 |
| Validação operacional final PostgreSQL 17.6 | 109 verificações aprovadas, 0 falhas no escopo; HTTP/claims/assinatura/estado/persistência/integridade, **não 109 testes JUnit** |
| Banco / startup | V0/V1/V2 já aplicadas/bem-sucedidas; duas instâncias temporárias validaram as três migrations sem nova aplicação; Hibernate validate |
| Autenticação / escopo | Cadastro cidadão validado antes do bloco final; login JWT/me nas quatro contas, BCrypt, isolamento A/B e proteção operacional |
| Domínio / consultas | Fluxo normal → atenção → fora → normal, bloqueio, auditoria/histórico, JPQL sem/com estado e paginação/contagem; sem liberação automática |
| Preservação | Três registros preexistentes preservados; bootstrap desabilitado no reinício; instâncias temporárias encerradas |

Esses resultados fundamentam o fechamento aprovado da J4. Não são testes executados pelo Mobile/Gemini nem por exemplos deste documento. H2 não substitui PostgreSQL e demonstração local não comprova produção, carga/concorrência PostgreSQL, aplicação inédita de V2, todos os checks/locks adversos, hardware ou integração real do aplicativo. A consolidação atual registra suas próprias verificações no relatório local A–L; não reabre a J4 nem encerra automaticamente o alinhamento bilateral.


## Adendo J5-A — recebimento interno — 13/11/2026

O inventário J4 acima mantém suas 29 rotas; esta entrega acrescenta o 30º par: `POST /api/v1/transportes/{id}/recebimento`. Sem corpo JSON e sem DTO novo. Exige Bearer de FUNCIONARIO da unidade de destino ou GESTOR (todas as unidades no MVP). CIDADAO não tem acesso nem recebe dados operacionais.

Sucesso 204 sem corpo: EM_TRANSPORTE → RECEBIDO; lotes EM_MONITORAMENTO → AGUARDANDO_CONFERENCIA; lotes bloqueados permanecem. Não libera/descarta, confirma disponibilidade, finaliza transporte nem encerra ocorrências. Autor e horário vêm exclusivamente do backend. Antes da transição, o backend verifica o limite configurado de silêncio e pode abrir PERDA_DE_TELEMETRIA, sem bloquear lotes.

Erros seguem J4: 401 sem autenticação/JWT inválido, 403 cidadão/outra unidade (também UUID inexistente para funcionário), 404 transporte inexistente para gestor, 409 estado diferente de EM_TRANSPORTE, inclusive repetição. Enviar somente o UUID na rota e Authorization; nenhum campo de autor, perfil, unidade ou horário é necessário. As respostas dos GETs existentes permitem consultar o estado posterior e os históricos autorizados. Integração conjunta Mobile permanece pendente (D-002); J5 inteira não está concluída.


## Adendo J5-B — decisão técnica interna — 13/11/2026

Novo par método/rota: `POST /api/v1/lotes/{id}/decisao-tecnica` (31º, contando J4 e recebimento J5-A). Exige JWT Bearer de GESTOR ativo; funcionário e cidadão não podem decidir. Corpo JSON exclusivamente com `decisao` (`LIBERAR` ou `DESCARTAR`) e `justificativa` obrigatória, não branca, até 1000 caracteres. Exemplo fictício: `{"decisao":"LIBERAR","justificativa":"Conferência humana realizada pelo gestor."}`. Autor, perfil, unidade, horário, disponibilidade ou estado adicionais são rejeitados; autoria/instante vêm do backend. Sucesso 204 sem corpo; consultar lote/histórico pelos GETs autorizados existentes.

Transporte precisa estar RECEBIDO. Apenas AGUARDANDO_CONFERENCIA ou BLOQUEADO_PREVENTIVAMENTE podem ir a LIBERADO/DESCARTADO; esses destinos são terminais nesta entrega. Disponibilidade continua NAO_DISPONIVEL. Não altera outros lotes, temperaturas, recebimento ou ocorrências; ocorrência aberta não impede sozinha decisão humana justificada e não é encerrada automaticamente. Registro operacional acadêmico, sem declaração de segurança clínica.

Erros preservados: 400 entrada inválida/campo desconhecido; 401 JWT ausente/inválido/usuário inativo; 403 funcionário/cidadão com comando válido; 404 lote inexistente para gestor; 409 lote/transporte incompatível ou decisão repetida. Cliente deve tratar ProblemDetail e não repetir decisões como se fossem idempotentes. Nenhum endpoint de disponibilidade, consulta cidadã ou finalização foi acrescentado. Demais DTOs/rotas J4/J5-A preservados; D-002 segue aberta e J5 inteira não está concluída.


## Adendo J5-C — disponibilidade interna — 13/11/2026

Novo par método/rota (32º): `POST /api/v1/lotes/{id}/disponibilidade`, Bearer JWT obrigatório. Corpo exclusivamente `{"disponibilidade":"CONFIRMADA"}` para confirmar ou `{"disponibilidade":"NAO_DISPONIVEL"}` para retirar. Campo obrigatório, enum existente; autor, perfil, unidade, horário, estado técnico e outros campos extras são rejeitados. Sem justificativa humana adicional.

FUNCIONARIO ativo só opera na unidade responsável pelo lote; GESTOR ativo mantém o escopo global do MVP, conforme endpoint/RN07 da especificação e aprovação explícita J5-C, sem contrariar o vínculo do funcionário previsto em RN11/RN22. CIDADAO negado. Transporte RECEBIDO e lote LIBERADO são obrigatórios nos dois sentidos e nas repetições, antes da idempotência. Estado técnico, outros lotes, ocorrências e histórico são preservados. Retirada não é descarte nem revogação da decisão técnica.

Sucesso 204 sem corpo. Repetição válida do valor atual também 204, sem evento ou novo horário. Mudança efetiva gera auditoria DISPONIBILIDADE_CONFIRMADA ou DISPONIBILIDADE_RETIRADA com UUID e Clock do backend. Comandos concorrentes são serializados; idempotência não é deduplicação global depois de uma mudança oposta interveniente.

Erros: 400 entrada/UUID/enum inválido, nulo/ausente ou campo extra; 401 autenticação ausente/inválida/inativa; 403 cidadão ou unidade alheia (também recurso inexistente para funcionário); 404 lote inexistente para gestor; 409 transporte/estado técnico incompatível. Preservados ProblemDetail e GETs operacionais existentes para consultar o resultado. Não foi criado GET cidadão /disponibilidade, quantidade garantida, reserva, agendamento ou finalização. Sem coluna atualizadoEm: seu uso futuro em J5-D depende dos eventos e da definição do caso sem evento, sem horário inventado. Demais contratos anteriores preservados; D-002 segue aberta e J5 inteira não está concluída.


## Adendo J5-D — consulta informativa ao cidadão — 13/11/2026

Novo par método/rota (33º): `GET /api/v1/disponibilidade`. Requer Bearer JWT válido e usuário ativo persistido. CIDADAO, FUNCIONARIO e GESTOR recebem a mesma visão pública global, sem exigir vínculo com a unidade consultada. Esta é a única autorização acrescentada ao cidadão; GETs de unidades/vacinas operacionais, lotes, transportes, leituras, ocorrências e históricos continuam restritos conforme J4. Os adendos J5-A/B/C são registros históricos e seus comandos internos continuam preservados.

| Parâmetro opcional | Regra |
|---|---|
| unidadeId | UUID público; filtro exato de unidade |
| vacinaId | UUID público; filtro exato de vacina, cumulativo com unidade |
| page | Página iniciada em 0; padrão 0 |
| size | Padrão 20; máximo 100 |
| sort | unidadeNome ou vacinaNome, asc/desc; pode repetir sort para compor critérios |

Sem sort, ordenar unidadeNome asc e vacinaNome asc. Sempre há desempate por UUID de unidade e vacina ascendentes; não enviar `sort=id`, `situacao` ou `atualizadoEm`. Exemplos: `/api/v1/disponibilidade?unidadeId=<UUID>&size=20&sort=vacinaNome,asc` e `/api/v1/disponibilidade?sort=vacinaNome,desc&sort=unidadeNome,asc`.

200 com `PaginaDto<DisponibilidadeSaidaDto>`. Exemplo fictício para catálogo com uma unidade e uma vacina:

```json
{"conteudo":[{"vacina":{"id":"11111111-1111-4111-8111-111111111111","nome":"Vacina Demo A"},"unidade":{"id":"22222222-2222-4222-8222-222222222222","nome":"Unidade Demonstrativa 01"},"situacao":"SEM_INFORMACAO","atualizadoEm":null,"mensagem":"Disponibilidade informada pela unidade. Consulte as condições de atendimento; não há reserva ou garantia de dose."}],"pagina":0,"tamanho":20,"totalElementos":1,"totalPaginas":1}
```

Cada item tem SOMENTE vacina/unidade (id/nome públicos), situacao, atualizadoEm e mensagem fixa. Não há lote/transporte/dispositivo/funcionário, temperaturas, ocorrências, histórico, justificativa ou quantidade. Não buscar dados operacionais para enriquecer a tela cidadã.

Universo: todas as combinações dos catálogos públicos unidade × vacina, com filtros e paginação no banco. Uma linha não significa que a unidade oferece a vacina, tem estoque, recebeu uma remessa ou teve transporte. Par sem lote e par com logística não publicada são SEM_INFORMACAO; não transformar isso em “sem doses”. TotalElementos conta pares do catálogo, nunca lotes ou doses.

Situações:

- CONFIRMADA: ao menos um lote LIBERADO/CONFIRMADA e transporte RECEBIDO. Informação da unidade, sem garantia ou reserva.
- CONFIRMACAO_RETIRADA: sem confirmação válida atual, com evento explícito de retirada e sem flag confirmada residual inconsistente. A confirmação foi retirada; não significa falta real de estoque.
- SEM_INFORMACAO: evidência insuficiente, inclusive sem lotes/eventos, dados não publicados ou flags inelegíveis inconsistentes. Não equivale a indisponibilidade real.

AtualizadoEm é nullable, ISO-8601 com offset quando presente: maior horário dos eventos de confirmação/retirada de todos os lotes do par, incluindo os já retirados. Ignora decisão técnica e demais eventos. Sem evento pertinente é null, inclusive para flag elegível legada sem auditoria. Não preencher com relógio do dispositivo/consulta. A última retirada pode atualizar horário mantendo CONFIRMADA se outro lote continuar confirmado. Só retirar a última confirmação muda para CONFIRMACAO_RETIRADA; reconfirmar volta a CONFIRMADA. Repetição idempotente não renova horário. Sempre exibir a mensagem fixa, mesmo sem informação.

Erros: 400 UUID/sort inválido, no padrão ProblemDetail existente; 401 autenticação ausente/inválida/expirada ou usuário inativo. UUID válido inexistente e página além do total retornam 200 com conteúdo vazio e total coerente com filtros. Nenhuma mutação, reserva, oferta quantitativa, agendamento ou finalização integra esta consulta.

Validação funcional J5-E executada em 14/11/2026, consolidada documentalmente em 15/11: GET /api/v1/disponibilidade retornou 200 com JWT de cidadão no PostgreSQL 17.6 real. Confirmados filtros por unidade/vacina isolados e combinados, paginação/ordenação, UUIDs, datas com offset/null, mensagem fixa e ausência de dados operacionais no DTO público. Foram observadas SEM_INFORMACAO, CONFIRMADA e CONFIRMACAO_RETIRADA; retirar um lote manteve a confirmação do outro e retirar o último mudou a situação, com atualizadoEm baseado na auditoria.

O cenário completo somou 57 verificações HTTP aprovadas, incluindo casos representativos 401/403/409; uma única regressão Java 21/test/H2 aprovou 256 testes sem falhas/erros/ignorados. Flyway validou V0/V1/V2 sem nova aplicação e Hibernate validou o schema. Registros preexistentes e backups preservados. São resultados anteriores; nenhum teste foi repetido nesta consolidação.

Rotas, DTOs, JWT, ProblemDetail, regras de disponibilidade e privacidade permanecem os mesmos. Integração efetiva Flutter/Arduino não executada; **D-002 continua aberta**. J5-E aprovada tecnicamente e aguardando encerramento documental; J5 inteira incompleta, com **D-053/OpenAPI aberta**, sem implementação ou mudança de etapa autorizada nesta atividade.


## Adendo de acesso Swagger/OpenAPI — 15/11/2026

D-053 atendida na J5 e encerrada tecnicamente, aguardando revisão. Com o backend dev já iniciado, Swagger UI em `http://127.0.0.1:8080/swagger-ui.html`, JSON em `/v3/api-docs` e YAML em `/v3/api-docs.yaml` (ajustar a porta usada). Documentação habilitada somente em dev/test, com prod/production desabilitando mesmo em combinação; não há nova URL hospedada. Faça login pela UI e informe somente o token em Authorize; as operações protegidas continuam exigindo Bearer e as mesmas permissões por perfil/unidade.

As 33 combinações método/rota, DTOs, paginação, erros e adendos funcionais anteriores permanecem intactos. Este acesso serve para consultar/experimentar a documentação local; não altera base URL do aplicativo nem comprova integração bilateral. D-002 permanece aberta. Não foram implementadas funcionalidades J6, dispositivos ou mudanças Mobile nesta atividade.
