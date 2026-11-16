package br.com.fiap.vacismart.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class OpenApiPerfisTest {
    @ParameterizedTest
    @CsvSource({"dev,true", "test,true", "prod,false", "production,false", "staging,false", "default,false", "test+prod,false", "dev+production,false"})
    void documentacaoExigeAmbienteAutorizado(String perfil, boolean habilitada) {
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active="+perfil.replace('+', ','))
                .withUserConfiguration(OpenApiConfig.class).run(context -> {
                    assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled",Boolean.class)).isEqualTo(habilitada);
                    assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled",Boolean.class)).isEqualTo(habilitada);
                    assertThat(context.containsBean("vacismartOpenApi")).isEqualTo(habilitada);
                });
    }
}
