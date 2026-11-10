package br.com.fiap.vacismart.service;

import br.com.fiap.vacismart.domain.entity.Usuario;
import br.com.fiap.vacismart.domain.enums.Perfil;
import br.com.fiap.vacismart.repository.UsuarioRepository;
import br.com.fiap.vacismart.security.AcessoService;
import br.com.fiap.vacismart.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import java.time.*;

@Service
@Transactional(readOnly = true)
public class AutenticacaoService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder encoder;
    private final Clock relogio;
    private final String emissor;
    private final Duration validade;
    private final AcessoService acesso;
    private final String hashAusente;
    public AutenticacaoService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder encoder,
            Clock relogio, @Value("${vacismart.jwt.emissor}") String emissor,
            @Value("${vacismart.jwt.validade:PT15M}") Duration validade, AcessoService acesso) {
        if (emissor.isBlank() || validade.isNegative() || validade.isZero() || validade.compareTo(Duration.ofHours(1)) > 0)
            throw new IllegalArgumentException("Emissor e validade JWT inválidos");
        this.usuarios = usuarios; this.senhas = senhas; this.encoder = encoder; this.relogio = relogio;
        this.emissor = emissor; this.validade = validade; this.acesso = acesso;
        this.hashAusente = senhas.encode(java.util.UUID.randomUUID().toString());
    }
    @Transactional
    public UsuarioSaidaDto cadastrar(CadastroEntradaDto dto) {
        String email = Usuario.normalizarEmail(dto.email());
        if (usuarios.existsByEmail(email)) throw new IllegalStateException("E-mail já cadastrado");
        return UsuarioSaidaDto.de(usuarios.saveAndFlush(new Usuario(dto.nome(), email, senhas.encode(dto.senha()), Perfil.CIDADAO, null)));
    }
    public LoginSaidaDto login(LoginEntradaDto dto) {
        var usuario = usuarios.findByEmail(Usuario.normalizarEmail(dto.email()));
        boolean correta = usuario.map(u -> u.conferirSenha(senhas, dto.senha())).orElseGet(() -> senhas.matches(dto.senha(), hashAusente));
        if (!correta || usuario.isEmpty() || !usuario.get().isAtivo()) throw new BadCredentialsException("Credenciais inválidas");
        var u = usuario.get();
        Instant agora = relogio.instant();
        var claims = JwtClaimsSet.builder().issuer(emissor).subject(u.getId().toString()).issuedAt(agora)
                .expiresAt(agora.plus(validade)).claim("perfil", u.getPerfil().name()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        return new LoginSaidaDto(token, "Bearer", agora.plus(validade), UsuarioSaidaDto.de(u));
    }
    public UsuarioSaidaDto propriaConta() { return UsuarioSaidaDto.de(acesso.atual()); }
}
