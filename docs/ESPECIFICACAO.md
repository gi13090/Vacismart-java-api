# Especificação funcional — VaciSmart (API Java)
Fonte de verdade da API Java. Protótipo acadêmico (FIAP GS 2026/2) com dados fictícios. Não é sistema clínico nem substitui a avaliação de profissionais de saúde.

## Perfis
CIDADAO, FUNCIONARIO (vinculado a uma UnidadeSaude) e GESTOR. O dispositivo (Arduino ou gateway) não é usuário: usa credencial própria.

## Estados
- Transporte: CADASTRADO -> EM_TRANSPORTE -> RECEBIDO -> FINALIZADO (sem GPS).
- Lote: EM_MONITORAMENTO, AGUARDANDO_CONFERENCIA, BLOQUEADO_PREVENTIVAMENTE, LIBERADO, DESCARTADO.
- Ocorrência: tipo FORA_DA_FAIXA ou PERDA_DE_TELEMETRIA; status ABERTA ou ENCERRADA (encerrar não libera lote).
- Disponibilidade do lote: NAO_DISPONIVEL ou CONFIRMADA.

## Regras de negócio
- RN01 Limites por vacina (temperaturaMinC e temperaturaMaxC). Vacina Demo A: 2,0 e 8,0. Margem de atenção configurável (padrão 1,0 °C).
- RN02 Classificação da leitura: NORMAL se min+margem <= t <= max-margem (demo: 3,0 a 7,0); ATENCAO se min <= t < min+margem ou max-margem < t <= max; FORA_DA_FAIXA se t < min ou t > max. ATENCAO só registra aviso: não bloqueia nem invalida.
- RN03 FORA_DA_FAIXA abre ocorrência (se já não houver uma ABERTA desse tipo para o transporte) e muda os lotes EM_MONITORAMENTO do transporte para BLOQUEADO_PREVENTIVAMENTE; lotes já bloqueados permanecem. O sistema nunca descarta nem declara degradação clínica. Não esperar 15 minutos.
- RN04 Frequência alvo de leitura: 30 segundos (informativa).
- RN05 Transporte EM_TRANSPORTE sem leitura há mais de 3 minutos (parâmetro configurável) gera ocorrência PERDA_DE_TELEMETRIA. Ela não bloqueia o lote sozinha, mas impede liberação sem conferência.
- RN06 POST de aplicação com lote BLOQUEADO_PREVENTIVAMENTE, DESCARTADO ou não LIBERADO é rejeitado com 409 e a tentativa é auditada. O sistema jamais autoriza aplicação real.
- RN07 e RN17 FUNCIONARIO só acessa remessas, lotes, leituras e ocorrências da unidade vinculada. GESTOR acessa conforme escopo autorizado (no MVP, todas as unidades).
- RN08 e RN23 O cidadão nunca recebe dados de transporte, lote, leitura, dispositivo ou funcionário.
- RN09 Transições do transporte validam perfil e ordem; não finaliza sem recebimento.
- RN10 e RN21 Recebimento é ação explícita de FUNCIONARIO ou GESTOR da unidade de destino, com autor e horário. Transporte vai para RECEBIDO; lotes EM_MONITORAMENTO vão para AGUARDANDO_CONFERENCIA; lotes bloqueados permanecem; não libera lote nem publica disponibilidade.
- Decisão técnica: só GESTOR. Lote BLOQUEADO_PREVENTIVAMENTE ou AGUARDANDO_CONFERENCIA vai para LIBERADO ou DESCARTADO, com justificativa obrigatória. Nunca é automática (nem por tempo, nem por normalização da temperatura).
- RN11 e RN22 Disponibilidade só pode ser CONFIRMADA se o transporte estiver RECEBIDO e o lote LIBERADO, por FUNCIONARIO da unidade; caso contrário, 409.
- RN12, RN13 e RN24 Aviso informativo: sem reserva, agendamento, fila ou garantia de dose. Mensagem padrão: "Disponibilidade informada pela unidade. Consulte as condições de atendimento; não há reserva ou garantia de dose."
- RN16 Auditoria imutável: toda mudança de estado de lote, recebimento, decisão, disponibilidade e tentativa de aplicação rejeitada grava EventoAuditoria (entidade, referenciaId, acao, autor ou SISTEMA, dataHora, justificativa).
- RN18 Senhas com BCrypt; nada de segredos ou tokens em logs.
- RN19 e RN20 Sem GPS, mapa, rota, coordenadas ou previsão de chegada para ninguém.

## Convenções de contrato
Prefixo /api/v1. IDs UUID (string). Datas ISO-8601 com offset. temperaturaC decimal (ex.: 5.2). medidaEm é o instante da medição; recebidaEm é quando a API recebeu. Erros: 400 validação, 401 sem autenticação, 403 sem permissão, 404 não encontrado, 409 conflito de estado.

## Endpoints mínimos (além do CRUD exigido)
- POST /auth/login (público)
- GET /unidades (autenticado)
- GET e POST /lotes (listagem paginada com filtros; cadastro pelo GESTOR)
- GET /transportes/{id} (FUNCIONARIO e GESTOR)
- POST /transportes/{id}/leituras (dispositivo autorizado)
- POST /transportes/{id}/recebimento (FUNCIONARIO e GESTOR)
- POST /lotes/{id}/decisao-tecnica (GESTOR)
- POST /lotes/{id}/disponibilidade (FUNCIONARIO e GESTOR)
- GET /disponibilidade?unidadeId=... (CIDADAO: só vacina, unidade, situação, atualizadoEm e mensagem)
- POST /aplicacoes (FUNCIONARIO e GESTOR; serve para demonstrar o bloqueio)

Exemplo de leitura: {"transporteId":"...","dispositivoId":"ARDUINO-DEMO-01","temperaturaC":9.3,"medidaEm":"2026-11-10T14:20:30-03:00"}

## Dados de seed
Unidade Demonstrativa 01; Vacina Demo A (2,0 a 8,0 °C); Lote VAC-2026-001; Transporte TRP-001; Dispositivo ARDUINO-DEMO-01. Leituras de referência: 5,2 (normal), 7,5 (atenção), 9,3 (fora da faixa).

## Casos de aceite
T01 5,2 persiste sem bloqueio. T02 7,5 registra atenção sem descarte. T03 9,3 abre ocorrência e bloqueia o lote. T04 aplicação com lote bloqueado é rejeitada (409). T05 recebimento com lote bloqueado mantém o bloqueio. T06 token de cidadão em rotas técnicas é negado. T07 disponibilidade válida só com recebimento e lote liberado. T08 disponibilidade inválida é rejeitada (409). T09 mais de 3 minutos sem leitura cria PERDA_DE_TELEMETRIA. T10 funcionário de outra unidade é negado.
