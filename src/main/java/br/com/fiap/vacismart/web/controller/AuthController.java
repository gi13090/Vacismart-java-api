package br.com.fiap.vacismart.web.controller;

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
    public ResponseEntity<UsuarioSaidaDto> cadastrar(@Valid @RequestBody CadastroEntradaDto dto) {
        return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(service.cadastrar(dto));
    }
    @PostMapping("/login")
    public LoginSaidaDto login(@Valid @RequestBody LoginEntradaDto dto) { return service.login(dto); }
    @GetMapping("/me")
    public UsuarioSaidaDto propriaConta() { return service.propriaConta(); }
}
