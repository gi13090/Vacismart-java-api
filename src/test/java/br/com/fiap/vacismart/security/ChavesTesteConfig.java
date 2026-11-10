package br.com.fiap.vacismart.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.security.*;

@TestConfiguration
public class ChavesTesteConfig {
    @Bean KeyPair chavesJwtTeste() throws Exception {
        var gerador = KeyPairGenerator.getInstance("RSA"); gerador.initialize(2048); return gerador.generateKeyPair();
    }
}
