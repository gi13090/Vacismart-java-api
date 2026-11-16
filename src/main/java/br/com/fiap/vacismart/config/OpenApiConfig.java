package br.com.fiap.vacismart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.headers.Header;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.math.BigDecimal;
import java.util.List;

/** Documenta contratos HTTP; não altera validação, serialização ou autorização. */
@Configuration
@Profile("(dev | test) & !prod & !production")
public class OpenApiConfig {
    private static final String BEARER = "bearerJwt";

    @Bean
    OpenAPI vacismartOpenApi() {
        return new OpenAPI().info(new Info().title("VaciSmart — API Java")
                .version("0.0.1-SNAPSHOT")
                .description("Protótipo acadêmico FIAP GS 2026/2. Dados fictícios; não é sistema clínico. "
                        + "Disponibilidade informativa, sem reserva ou garantia de dose. IDs UUID e datas ISO-8601 com offset. "
                        + "Faça login e informe somente o token em Authorize. Senhas/chaves/tokens reais não devem constar em exemplos ou capturas."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")
                        .description("JWT RS256 emitido pelo login. Privilégios vêm do usuário ativo no banco; não apenas das claims.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    private static Schema<?> problemaSchema() {
        return new ObjectSchema()
                .description("ProblemDetail. instance é opcional, em especial nos filtros 401/403; campos só aparece em validação de DTO. CORS pode rejeitar sem este corpo.")
                .addProperty("type", new StringSchema().format("uri").example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Campos inválidos"))
                .addProperty("instance", new StringSchema().format("uri"))
                .addProperty("campos", new ArraySchema().items(new ObjectSchema()
                        .addProperty("campo", new StringSchema())
                        .addProperty("mensagem", new StringSchema())))
                .required(List.of("type", "title", "status", "detail"));
    }

    @Bean
    OpenApiCustomizer contratosDocumentados() {
        return api -> {
            // O schema é acrescentado junto às referências, após a coleta dos DTOs pelo springdoc.
            api.getComponents().addSchemas("ProblemaApi", problemaSchema());
            api.getPaths().forEach((rota, item) -> item.readOperationsMap().forEach((metodo, op) -> {
                boolean publico = rota.equals("/api/v1/auth/login") || rota.equals("/api/v1/auth/cadastro");
                if (publico) op.setSecurity(List.of());
                String verbo = metodo.name();
                var respostas = op.getResponses();
                respostas.addApiResponse("400", problema("Entrada, UUID, JSON ou ordenação inválidos; campos desconhecidos são rejeitados"));
                var naoAutenticado = problema("Autenticação ausente/inválida, usuário inativo ou credenciais incorretas");
                naoAutenticado.addHeaderObject("WWW-Authenticate", new Header().schema(new StringSchema()).description("Bearer"));
                respostas.addApiResponse("401", naoAutenticado);
                if (!publico) respostas.addApiResponse("403", problema("Perfil ou unidade sem permissão. Funcionário recebe 403 também para lote/transporte inexistente, sem revelar existência"));
                if (rota.contains("{id}") || (verbo.equals("POST") && List.of("/api/v1/lotes", "/api/v1/transportes", "/api/v1/aplicacoes").contains(rota))
                        || (op.getParameters() != null && op.getParameters().stream()
                        .anyMatch(p -> "transporteId".equals(p.getName())))) {
                    respostas.addApiResponse("404", problema("Recurso ausente no escopo autorizado; gestor recebe 404 para lote/transporte inexistente"));
                }
                if (!verbo.equals("GET") && !rota.equals("/api/v1/auth/login")) {
                    respostas.addApiResponse("409", problema("Estado, duplicidade, vínculos ou concorrência impedem a operação"));
                }
                if (rota.equals("/api/v1/aplicacoes")) {
                    respostas.addApiResponse("409", problema("Lote não LIBERADO: tentativa rejeitada com auditoria persistida. Conflitos de concorrência também podem retornar 409, sem confirmar a verificação"));
                }
                if (op.getRequestBody() != null) respostas.addApiResponse("415", problema("Tipo de mídia não suportado"));
                respostas.addApiResponse("500", problema("Erro interno ao processar a requisição; sem detalhes internos"));
                if (respostas.containsKey("201")) {
                    respostas.get("201").addHeaderObject("Location", new Header().schema(new StringSchema().format("uri-reference"))
                            .description("URI relativa do recurso; no cadastro cidadão aponta para /api/v1/auth/me"));
                }
                respostas.values().forEach(resposta -> {
                    if (resposta.getContent() != null && resposta.getContent().containsKey("*/*")) {
                        resposta.getContent().addMediaType("application/json", resposta.getContent().remove("*/*"));
                    }
                });
                if (op.getParameters() != null) op.getParameters().forEach(OpenApiConfig::descreverParametro);
                if (op.getRequestBody() != null) op.getRequestBody().setDescription("JSON do DTO indicado. Não enviar campos extras; autor, perfil e horários de auditoria vêm do servidor.");
            }));
        };
    }

    private static ApiResponse problema(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemaApi"))));
    }

    private static void descreverParametro(Parameter p) {
        String descricao = switch (p.getName()) {
            case "id" -> "UUID do recurso na rota; acesso verificado por perfil/unidade.";
            case "leituraId" -> "UUID da leitura pertencente ao transporte da rota.";
            case "unidadeId" -> "Filtro opcional por UUID da unidade. Na consulta pública não impõe escopo operacional; nas demais consultas respeita o vínculo do funcionário.";
            case "transporteId" -> "Filtro opcional por UUID do transporte; exige acesso ao transporte (403/404 conforme perfil).";
            case "vacinaId" -> "Filtro opcional por UUID da vacina, cumulativo com os demais filtros.";
            case "estado" -> "Filtro opcional pelo enum do recurso, cumulativo com os demais filtros.";
            case "page" -> "Página base zero, padrão 0. Valores negativos/inválidos são normalizados pelo resolver.";
            case "size" -> "Tamanho padrão 20, máximo 100; maior é limitado a 100, inválido/não positivo usa o padrão.";
            case "sort" -> "Campo,asc ou campo,desc; repetível. Campos permitidos estão na descrição da operação. Campo não permitido retorna 400. Padrão id, exceto disponibilidade (unidadeNome/vacinaNome).";
            default -> null;
        };
        if (descricao != null) p.setDescription(descricao);
        if ("size".equals(p.getName()) && p.getSchema() != null) {
            p.getSchema().setMaximum(BigDecimal.valueOf(100));
            p.getSchema().setDefault(20);
        }
        if ("page".equals(p.getName()) && p.getSchema() != null) p.getSchema().setDefault(0);
    }
}
