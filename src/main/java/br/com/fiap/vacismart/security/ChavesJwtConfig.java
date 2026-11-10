package br.com.fiap.vacismart.security;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import java.security.KeyPair;

@Configuration
@Profile("!test")
public class ChavesJwtConfig {
    @Bean
    KeyPair chavesJwt(@Value("${vacismart.jwt.chave-publica}") Resource publica,
            @Value("${vacismart.jwt.chave-privada}") Resource privada) throws Exception {
        try (var pub = publica.getInputStream(); var pri = privada.getInputStream()) {
            var chavePublica = RsaKeyConverters.x509().convert(pub);
            var chavePrivada = RsaKeyConverters.pkcs8().convert(pri);
            if (chavePublica.getModulus().bitLength() < 2048 || !chavePublica.getModulus().equals(chavePrivada.getModulus()))
                throw new IllegalStateException("Par RSA inválido");
            return new KeyPair(chavePublica, chavePrivada);
        }
    }
}
