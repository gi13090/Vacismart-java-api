package br.com.fiap.vacismart.domain.entity;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

/** Compara nomes físicos efetivos com as migrations V1 e V2, independentemente do DDL gerado pelo H2. */
@org.springframework.context.annotation.Import(br.com.fiap.vacismart.security.ChavesTesteConfig.class)
@SpringBootTest
@ActiveProfiles("test")
class MapeamentoSchemaTest {
    @Autowired EntityManagerFactory entityManagerFactory;

    static Stream<Class<?>> entidades() {
        return Stream.of(UnidadeSaude.class, Vacina.class, Transporte.class, Lote.class,
                LeituraTemperatura.class, Ocorrencia.class, EventoAuditoria.class, Usuario.class);
    }

    @ParameterizedTest
    @MethodSource("entidades")
    void colunasFisicasCorrespondemAMigration(Class<?> entidade) throws Exception {
        var factory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        var persister = (AbstractEntityPersister) factory.getMappingMetamodel().getEntityDescriptor(entidade);
        Set<String> colunasJpa = new TreeSet<>(Arrays.asList(persister.getIdentifierColumnNames()));
        for (String propriedade : persister.getPropertyNames()) {
            colunasJpa.addAll(Arrays.asList(persister.getPropertyColumnNames(propriedade)));
        }
        String migration = entidade == Usuario.class ? "V2__modela_usuarios.sql" : "V1__modela_nucleo_termico.sql";
        String sql = new ClassPathResource("db/migration/" + migration)
                .getContentAsString(StandardCharsets.UTF_8);
        var tabela = Pattern.compile("(?ims)^CREATE TABLE " + Pattern.quote(persister.getRootTableName())
                + " [(]\\n(.*?)^[)];").matcher(sql);
        assertThat(tabela.find()).as("Tabela %s na V1", persister.getRootTableName()).isTrue();
        var coluna = Pattern.compile("(?im)^    ([a-z_]+) (?:UUID|VARCHAR|NUMERIC|TIMESTAMP|BOOLEAN)\\b")
                .matcher(tabela.group(1));
        Set<String> colunasV1 = new TreeSet<>();
        while (coluna.find()) colunasV1.add(coluna.group(1));
        assertThat(colunasV1).as("Colunas extraídas da V1").isNotEmpty();
        System.out.printf("Mapeamento %s: JPA=%s; V1=%s; physical=%s; implicit=%s%n",
                persister.getRootTableName(), colunasJpa, colunasV1,
                factory.getProperties().get("hibernate.physical_naming_strategy"),
                factory.getProperties().get("hibernate.implicit_naming_strategy"));
        assertThat(colunasJpa).as("Colunas físicas de %s", persister.getRootTableName())
                .containsExactlyInAnyOrderElementsOf(colunasV1);
    }
}
