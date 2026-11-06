package br.com.fiap.vacismart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VaciSmartApplicationTests {

    @Test
    void carregaContexto() {
        // O carregamento do contexto valida a configuração mínima da aplicação.
    }
}
