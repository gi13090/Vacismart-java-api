package br.com.fiap.vacismart.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import br.com.fiap.vacismart.service.AutenticacaoService;
import br.com.fiap.vacismart.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AutenticacaoService service;
    public AuthController(AutenticacaoService service) { this.service = service; }
    @PostMapping("/cadastro")
    @Operation(summary = "Cadastrar cidadão", description = "Público. Cria somente CIDADAO; e-mail normalizado e único. Senha de 12 a 72 caracteres ASCII imprimíveis. Não emite token; Location aponta para a própria conta, cuja consulta exige login.")
    @ApiResponse(responseCode = "201", description = "Criado; Location identifica o recurso")
    public ResponseEntity<UsuarioSaidaDto> cadastrar(@Valid @RequestBody CadastroEntradaDto dto) {
        return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(service.cadastrar(dto));
    }
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Público. Conta ativa e credenciais válidas. Retorna Bearer RS256, com validade padrão de 15 minutos. Sem refresh; perfil e vínculo persistidos são reavaliados em cada requisição.")
    public LoginSaidaDto login(@Valid @RequestBody LoginEntradaDto dto) { return service.login(dto); }
    @GetMapping("/me")
    @Operation(summary = "Consultar a própria conta", description = "CIDADAO, FUNCIONARIO ou GESTOR ativo. Retorna apenas a conta autenticada, sem senha/hash.")
    public UsuarioSaidaDto propriaConta() { return service.propriaConta(); }
}
