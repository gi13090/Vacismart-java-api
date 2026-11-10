package br.com.fiap.vacismart.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ErrosSeguranca {
    private ErrosSeguranca() { }
    public static void escrever(HttpServletResponse resposta, int status) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/problem+json");
        resposta.setCharacterEncoding("UTF-8");
        if (status == 401) resposta.setHeader("WWW-Authenticate", "Bearer");
        resposta.getWriter().write(status == 401
                ? "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Autenticação necessária ou inválida\"}"
                : "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Acesso negado\"}");
    }
}
