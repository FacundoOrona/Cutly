package com.dev.cutly.suscripcion.config;

import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** MySQL real, opt-in: -Dcutly.mysql.schema-migration=true -Dtest=PlanSuscripcionSchemaMigratorMySqlTests test */
@Testcontainers
@EnabledIfSystemProperty(named = "cutly.mysql.schema-migration", matches = "true")
class PlanSuscripcionSchemaMigratorMySqlTests {

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("cutly_schema_migration_test")
            .withUsername("cutly_test_user")
            .withPassword("ephemeral_test_password");

    private JdbcTemplate jdbc;
    private PlanSuscripcionSchemaMigrator migrator;
    private DataSource dataSource;

    @BeforeEach
    void prepareDatabase() {
        dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS planes_suscripcion");
        createTable(true, "utf8mb4_bin", "YES", true, false);
        migrator = new PlanSuscripcionSchemaMigrator(dataSource);
    }

    @Test
    void esquemaCorrectoNoRealizaCambios() {
        createTable(false, "utf8mb4_bin", "NO", false, true);
        addPlan("Plan Uno", "plan uno", "COMERCIAL");

        PlanSuscripcionSchemaMigrator.MigrationResult result = migrator.migrar();

        assertFalse(result.changed());
        assertEquals("Plan Uno", jdbc.queryForObject("SELECT nombre FROM planes_suscripcion WHERE plan_id = 1", String.class));
    }

    @Test
    void corrigeNulabilidadIncorrectaSinCambiarDatosComerciales() {
        addPlan("Plan Uno", "plan uno", "COMERCIAL");
        jdbc.update("UPDATE planes_suscripcion SET precio_mensual = 1234.50, max_negocios = 7, version_plan = 9 WHERE plan_id = 1");

        assertTrue(migrator.migrar().changed());

        assertEquals("NO", nullable("nombre_normalizado"));
        assertEquals("NO", nullable("tipo"));
        assertEquals("Plan Uno", jdbc.queryForObject("SELECT nombre FROM planes_suscripcion WHERE plan_id=1", String.class));
        assertEquals("1234.50", jdbc.queryForObject("SELECT CAST(precio_mensual AS CHAR) FROM planes_suscripcion WHERE plan_id=1", String.class));
        assertEquals(7, jdbc.queryForObject("SELECT max_negocios FROM planes_suscripcion WHERE plan_id=1", Integer.class));
        assertEquals(9L, jdbc.queryForObject("SELECT version_plan FROM planes_suscripcion WHERE plan_id=1", Long.class));
    }

    @Test
    void corrigeCollationIncorrecta() {
        createTable(false, "utf8mb4_general_ci", "NO", false, true);
        addPlan("Plan Uno", "plan uno", "COMERCIAL");

        migrator.migrar();

        assertEquals("utf8mb4_bin", collation("nombre_normalizado"));
    }

    @Test
    void creaIndiceCuandoFalta() {
        createTable(false, "utf8mb4_bin", "NO", false, false);
        addPlan("Plan Uno", "plan uno", "COMERCIAL");

        migrator.migrar();

        assertTrue(indexMatches());
    }

    @Test
    void indiceExistenteCorrectoNoSeDuplica() {
        createTable(false, "utf8mb4_bin", "NO", false, true);
        addPlan("Plan Uno", "plan uno", "COMERCIAL");

        assertFalse(migrator.migrar().changed());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='planes_suscripcion' "
                + "AND INDEX_NAME='uq_planes_suscripcion_nombre_normalizado'", Integer.class));
    }

    @Test
    void duplicadosAbortanAntesDeAlterarDatosOEsquema() {
        addPlan("Plan Uno", "repetido", "COMERCIAL");
        addPlan("Plan Dos", "repetido", "COMERCIAL");

        IllegalStateException error = assertThrows(IllegalStateException.class, migrator::migrar);

        assertTrue(error.getMessage().contains("nombre_normalizado duplicado"));
        assertEquals("YES", nullable("nombre_normalizado"));
        assertFalse(indexMatches());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM planes_suscripcion", Integer.class));
    }

    @Test
    void valoresNullIncompatiblesAbortanSinInventarDatos() {
        addPlan("Plan Uno", "plan uno", null);

        IllegalStateException error = assertThrows(IllegalStateException.class, migrator::migrar);

        assertTrue(error.getMessage().contains("tipo NULL"));
        assertNull(jdbc.queryForObject("SELECT tipo FROM planes_suscripcion WHERE plan_id=1", String.class));
        assertEquals("YES", nullable("nombre_normalizado"));
        assertFalse(indexMatches());
    }

    @Test
    void fallaDeMigracionDejaReadinessBloqueada() {
        addPlan("Plan Uno", "plan uno", null);
        PlanSuscripcionReadiness readiness = new PlanSuscripcionReadiness();
        PlanSuscripcionSchemaVerifier verifier = mock(PlanSuscripcionSchemaVerifier.class);
        PlanSuscripcionInitializer initializer = new PlanSuscripcionInitializer(
                mock(PlanSuscripcionService.class), migrator, readiness, verifier);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertFalse(readiness.isReady());
        assertTrue(readiness.mensajeNoDisponible().contains("tipo NULL"));
        verifyNoInteractions(verifier);
    }

    @Test
    void segundaEjecucionNoHaceCambiosAdicionales() {
        addPlan(" Plan   Uno ", null, "COMERCIAL");

        assertTrue(migrator.migrar().changed());
        String name = jdbc.queryForObject("SELECT nombre FROM planes_suscripcion WHERE plan_id=1", String.class);
        String key = jdbc.queryForObject("SELECT nombre_normalizado FROM planes_suscripcion WHERE plan_id=1", String.class);
        assertFalse(migrator.migrar().changed());
        assertEquals(name, jdbc.queryForObject("SELECT nombre FROM planes_suscripcion WHERE plan_id=1", String.class));
        assertEquals(key, jdbc.queryForObject("SELECT nombre_normalizado FROM planes_suscripcion WHERE plan_id=1", String.class));
    }

    private void createTable(boolean normalizedNullable, String collation, String tipoNullable,
                             boolean versionNullable, boolean uniqueIndex) {
        jdbc.execute("DROP TABLE IF EXISTS planes_suscripcion");
        String normalizedNullability = normalizedNullable ? "NULL" : "NOT NULL";
        String typeNullability = "YES".equals(tipoNullable) ? "NULL" : "NOT NULL";
        String versionNullability = versionNullable ? "NULL" : "NOT NULL";
        jdbc.execute("CREATE TABLE planes_suscripcion ("
                + "plan_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "nombre VARCHAR(50) NOT NULL, "
                + "nombre_normalizado VARCHAR(100) CHARACTER SET utf8mb4 COLLATE " + collation + " " + normalizedNullability + ", "
                + "tipo VARCHAR(20) " + typeNullability + ", version_plan BIGINT " + versionNullability + ", "
                + "precio_mensual DECIMAL(10,2) NOT NULL DEFAULT 10.00, max_negocios INT NOT NULL DEFAULT 2"
                + (uniqueIndex ? ", UNIQUE INDEX uq_planes_suscripcion_nombre_normalizado (nombre_normalizado)" : "")
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void addPlan(String name, String key, String type) {
        jdbc.update("INSERT INTO planes_suscripcion (nombre, nombre_normalizado, tipo, version_plan) VALUES (?, ?, ?, 0)",
                name, key, type);
    }

    private String nullable(String column) {
        return jdbc.queryForObject("SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='planes_suscripcion' AND COLUMN_NAME=?", String.class, column);
    }

    private String collation(String column) {
        return jdbc.queryForObject("SELECT COLLATION_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='planes_suscripcion' AND COLUMN_NAME=?", String.class, column);
    }

    private boolean indexMatches() {
        List<String> columns = jdbc.query("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='planes_suscripcion' "
                        + "AND INDEX_NAME='uq_planes_suscripcion_nombre_normalizado' AND NON_UNIQUE=0 AND SUB_PART IS NULL",
                (rs, row) -> rs.getString(1));
        return columns.equals(List.of("nombre_normalizado"));
    }
}
