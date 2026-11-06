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
| D-007 | 2026-11-06 | Conflito de referências | PDFs e README de referência exigem cache, HATEOAS e Actuator. | Seguir pedido explícito de dispensa pelo professor; não implementar. | Obter confirmação acadêmica; reverter exclusão de escopo se houver nova instrução. | Divergência registrada; não tratar anexos como instruções de implementação. | ABERTA |
| D-008 | 2026-11-06 | Autoconfiguração Flyway no Boot 4 | core sozinho não ativa integração Spring no Boot 4. | Adicionar spring-boot-flyway, somente módulo técnico da integração já solicitada, além de flyway-core e flyway-database-postgresql. | Starter Flyway (traz o mesmo módulo); reverter trocando as declarações equivalentes. | Adaptação necessária à versão mais recente, sem recurso funcional adicional. | ABERTA |
| D-009 | 2026-11-06 | Perfil e ambiente local | Como impedir defaults locais fora de dev? | Sem perfil padrão; credenciais e defaults apenas em dev. .env é lido pelo Compose; exportar as mesmas variáveis para API. | Perfil dev padrão; reverter em application.yml e instruções. | Execução requer selecionar dev; testes selecionam test explicitamente. | ABERTA |
| D-010 | 2026-11-06 | Banco local | Qual imagem e persistência usar? | postgres:17.6, imagem oficial com versão fixa, volume nomeado e healthcheck; porta 5432. | Outra versão suportada; reverter imagem com migração/backup do volume, nunca apagar dados automaticamente. | Somente infraestrutura local; não é implantação final. | ABERTA |
| D-011 | 2026-11-06 | Fuso e Jackson 3 | Como aplicar fuso padrão sem criar classes nos pacotes reservados? | TimeZone no main; Surefire com user.timezone; Jackson timezone e datatype.datetime.write-dates-as-timestamps=false. | TZ/JVM externamente; reverter main/configuração/plugin em conjunto. | Datas com offset em ISO-8601; perfil test usa mesmo fuso. | ABERTA |

## Fontes técnicas consultadas
- https://spring.io/projects/spring-boot/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://spring.io/blog/2025/10/28/modularizing-spring-boot/
- https://docs.spring.io/spring-boot/appendix/application-properties/index.html

## Validação
Em andamento; os resultados reais serão registrados antes da conclusão.
