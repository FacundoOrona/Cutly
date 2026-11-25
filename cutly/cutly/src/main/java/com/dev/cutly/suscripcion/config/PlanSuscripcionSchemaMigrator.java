package com.dev.cutly.suscripcion.config;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PlanSuscripcionSchemaMigrator {

    private static final Logger logger = LoggerFactory.getLogger(PlanSuscripcionSchemaMigrator.class);
    private static final String TABLE = "planes_suscripcion";
    private static final String INDEX = PlanSuscripcionSchemaVerifier.INDICE_NOMBRE_NORMALIZADO;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PlanSuscripcionSchemaMigrator(
            DataSource dataSource
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public MigrationResult migrar() {
        ensureTableExists();
        requireTable();
        boolean normalizedColumnExists = columnExists("nombre_normalizado");
        boolean typeColumnExists = columnExists("tipo");
        requireColumn("plan_id");
        requireColumn("nombre");
        requireColumn("descripcion");
        requireColumn("precio_mensual");
        requireColumn("max_negocios");
        requireColumn("activo");
        requireColumn("version_plan");

        List<PlanRow> rows = readRows(normalizedColumnExists, typeColumnExists);
        List<String> incompatibilities = inspectRows(rows, typeColumnExists);
        if (!incompatibilities.isEmpty()) {
            throw new IllegalStateException(String.join("; ", incompatibilities));
        }

        IndexInfo existingIndex = indexInfo(INDEX);
        if (existingIndex.exists() && !existingIndex.matches()) {
            throw new IllegalStateException("El índice " + INDEX
                    + " ya existe con una definición distinta; no se eliminará ni reemplazará automáticamente.");
        }
        if (existingIndex.matches()) {
            ensureIndexBackfillDoesNotConflict(rows);
        }

        List<PlanRow> rowsToBackfill = rows.stream()
                .filter(row -> !row.normalizedNameExists() || !row.normalizedName().equals(row.expectedNormalizedName()))
                .toList();

        boolean changed = false;
        if (!normalizedColumnExists) {
            if (!columnExists("nombre_normalizado")) {
                logger.info("Agregando planes_suscripcion.nombre_normalizado como columna nullable para preparar el backfill validado");
                jdbcTemplate.execute("ALTER TABLE planes_suscripcion ADD COLUMN nombre_normalizado "
                        + "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL");
                changed = true;
            }
        }

        if (!rowsToBackfill.isEmpty()) {
            logger.info("Completando nombre_normalizado derivado del nombre en {} planes", rowsToBackfill.size());
            transactionTemplate.executeWithoutResult(status -> rowsToBackfill.forEach(row ->
                    jdbcTemplate.update("UPDATE planes_suscripcion SET nombre_normalizado = ? WHERE plan_id = ?",
                            row.expectedNormalizedName(), row.planId())));
            changed = true;
        }

        if (!columnMatches("nombre_normalizado", "varchar", 100, "NO", "utf8mb4_bin")) {
            logger.info("Ajustando tipo, longitud, nulabilidad y collation de planes_suscripcion.nombre_normalizado");
            jdbcTemplate.execute("ALTER TABLE planes_suscripcion MODIFY COLUMN nombre_normalizado "
                    + "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
            changed = true;
        }

        if (!typeColumnExists) {
            if (!rows.isEmpty()) {
                throw new IllegalStateException("Falta planes_suscripcion.tipo para " + ids(rows)
                        + "; el tipo de plan no se puede inferir sin una decisión comercial.");
            }
            if (!columnExists("tipo")) {
                logger.info("Agregando planes_suscripcion.tipo VARCHAR(20) NOT NULL a la tabla vacía");
                jdbcTemplate.execute("ALTER TABLE planes_suscripcion ADD COLUMN tipo VARCHAR(20) NOT NULL");
                changed = true;
            }
        } else if (!columnMatches("tipo", "varchar", 20, "NO", null)) {
            logger.info("Ajustando tipo, longitud y nulabilidad de planes_suscripcion.tipo");
            jdbcTemplate.execute("ALTER TABLE planes_suscripcion MODIFY COLUMN tipo VARCHAR(20) NOT NULL");
            changed = true;
        }

        if (!columnMatches("version_plan", "bigint", 0, "NO", null)) {
            logger.info("Ajustando planes_suscripcion.version_plan a BIGINT NOT NULL sin alterar sus valores");
            jdbcTemplate.execute("ALTER TABLE planes_suscripcion MODIFY COLUMN version_plan BIGINT NOT NULL");
            changed = true;
        }

        IndexInfo beforeCreate = indexInfo(INDEX);
        if (beforeCreate.exists() && !beforeCreate.matches()) {
            throw new IllegalStateException("El índice " + INDEX
                    + " cambió durante la migración y no tiene la definición requerida.");
        }
        if (!beforeCreate.matches()) {
            logger.info("Creando índice único {} sobre planes_suscripcion.nombre_normalizado", INDEX);
            try {
                jdbcTemplate.execute("CREATE UNIQUE INDEX uq_planes_suscripcion_nombre_normalizado "
                        + "ON planes_suscripcion (nombre_normalizado)");
                changed = true;
            } catch (RuntimeException e) {
                IndexInfo afterFailure = indexInfo(INDEX);
                if (!afterFailure.matches()) {
                    throw new IllegalStateException("No se pudo crear el índice único " + INDEX
                            + "; se conservaron las filas y el readiness seguirá bloqueado.", e);
                }
                logger.info("El índice {} fue creado en paralelo por otra instancia", INDEX);
            }
        }

        logger.info("Migración de planes_suscripcion finalizada; hubo cambios: {}", changed);
        return new MigrationResult(changed, rowsToBackfill.size());
    }

    private void requireTable() {
        if (!tableExists()) {
            throw new IllegalStateException("No existe la tabla planes_suscripcion después de intentar crearla.");
        }
    }

    private void ensureTableExists() {
        if (tableExists()) return;
        logger.info("La tabla planes_suscripcion no existe; creando el esquema base vacío antes de iniciar Hibernate");
        try {
            jdbcTemplate.execute("CREATE TABLE planes_suscripcion ("
                    + "plan_id BIGINT NOT NULL AUTO_INCREMENT, "
                    + "nombre VARCHAR(50) NOT NULL, "
                    + "nombre_normalizado VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL, "
                    + "descripcion VARCHAR(500) NULL, precio_mensual DECIMAL(10,2) NOT NULL, "
                    + "max_negocios INT NULL, activo BOOLEAN NOT NULL, tipo VARCHAR(20) NOT NULL, "
                    + "version_plan BIGINT NOT NULL, PRIMARY KEY (plan_id), "
                    + "UNIQUE INDEX uq_planes_suscripcion_nombre_normalizado (nombre_normalizado)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (RuntimeException e) {
            if (!tableExists()) {
                throw new IllegalStateException("No se pudo crear la tabla planes_suscripcion vacía.", e);
            }
            logger.info("La tabla planes_suscripcion fue creada en paralelo por otra instancia");
        }
    }

    private boolean tableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, TABLE);
        return count != null && count > 0;
    }

    private void requireColumn(String column) {
        if (!columnExists(column)) {
            throw new IllegalStateException("Falta planes_suscripcion." + column
                    + "; el migrador no puede inspeccionar datos con seguridad.");
        }
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, TABLE, column);
        return count != null && count > 0;
    }

    private List<PlanRow> readRows(boolean normalizedColumnExists, boolean typeColumnExists) {
        String normalized = normalizedColumnExists ? "nombre_normalizado" : "NULL AS nombre_normalizado";
        String type = typeColumnExists ? "tipo" : "NULL AS tipo";
        return jdbcTemplate.query("SELECT plan_id, nombre, " + normalized + ", " + type + ", version_plan"
                        + " FROM planes_suscripcion ORDER BY plan_id",
                (rs, row) -> new PlanRow(
                        rs.getLong("plan_id"),
                        rs.getString("nombre"),
                        rs.getString("nombre_normalizado"),
                        rs.getString("tipo"),
                        rs.getObject("nombre_normalizado") != null,
                        rs.getObject("version_plan") != null
                ));
    }

    private List<String> inspectRows(List<PlanRow> rows, boolean typeColumnExists) {
        List<String> issues = new ArrayList<>();
        List<PlanRow> invalidNames = rows.stream().filter(row -> row.name() == null || row.name().isBlank()).toList();
        if (!invalidNames.isEmpty()) {
            issues.add("Nombres vacíos o NULL en plan_id=" + idsOnly(invalidNames));
        }

        List<PlanRow> nullTypes = typeColumnExists
                ? rows.stream().filter(row -> row.type() == null).toList() : rows;
        if (!nullTypes.isEmpty()) {
            issues.add("tipo NULL o ausente en plan_id=" + idsOnly(nullTypes)
                    + "; no se inferirá ni modificará el tipo comercial");
        }
        List<PlanRow> invalidTypes = rows.stream()
                .filter(row -> row.type() != null && !Set.of("TRIAL", "COMERCIAL").contains(row.type()))
                .toList();
        if (!invalidTypes.isEmpty()) {
            issues.add("tipo no reconocido en " + invalidTypes.stream()
                    .map(row -> "plan_id=" + row.planId() + " (tipo='" + row.type() + "')")
                    .collect(Collectors.joining(", ")));
        }
        List<PlanRow> nullVersions = rows.stream().filter(row -> !row.versionExists()).toList();
        if (!nullVersions.isEmpty()) {
            issues.add("version_plan NULL en plan_id=" + idsOnly(nullVersions)
                    + "; las versiones existentes no se modificarán automáticamente");
        }

        List<PlanRow> oversized = rows.stream()
                .filter(row -> row.name() != null && !row.name().isBlank())
                .filter(row -> PlanSuscripcion.claveNombre(row.name()).codePointCount(0,
                        PlanSuscripcion.claveNombre(row.name()).length()) > 100)
                .toList();
        if (!oversized.isEmpty()) {
            issues.add("nombre_normalizado superaría 100 caracteres en plan_id=" + idsOnly(oversized));
        }

        Map<String, List<PlanRow>> storedKeys = rows.stream()
                .filter(PlanRow::normalizedNameExists)
                .filter(row -> !row.normalizedName().isBlank())
                .collect(Collectors.groupingBy(PlanRow::normalizedName));
        List<String> duplicateStored = storedKeys.values().stream().filter(group -> group.size() > 1)
                .map(this::describeRows).toList();
        if (!duplicateStored.isEmpty()) {
            issues.add("nombre_normalizado duplicado actualmente: " + String.join("; ", duplicateStored));
        }

        if (invalidNames.isEmpty() && oversized.isEmpty()) {
            Map<String, List<PlanRow>> normalizedNames = rows.stream().collect(Collectors.groupingBy(PlanRow::expectedNormalizedName));
            List<String> collisions = normalizedNames.values().stream().filter(group -> group.size() > 1)
                    .map(this::describeRows).toList();
            if (!collisions.isEmpty()) {
                issues.add("Colisiones tras normalizar nombres; no se renombraron planes: " + String.join("; ", collisions));
            }
        }
        return issues;
    }

    private void ensureIndexBackfillDoesNotConflict(List<PlanRow> rows) {
        Map<String, PlanRow> storedOwners = new HashMap<>();
        rows.stream().filter(PlanRow::normalizedNameExists).forEach(row -> storedOwners.put(row.normalizedName(), row));
        List<PlanRow> conflicts = rows.stream().filter(PlanRow::normalizedNameExists)
                .filter(row -> !row.normalizedName().equals(row.expectedNormalizedName()))
                .filter(row -> storedOwners.containsKey(row.expectedNormalizedName())
                        && !storedOwners.get(row.expectedNormalizedName()).planId().equals(row.planId()))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("El índice único existente impide corregir claves derivadas sin una colisión temporal; "
                    + "revisar " + describeRows(conflicts));
        }
    }

    private boolean columnMatches(String column, String expectedType, long expectedLength, String nullable, String collation) {
        List<ColumnInfo> result = jdbcTemplate.query(
                "SELECT DATA_TYPE, IS_NULLABLE, CHARACTER_MAXIMUM_LENGTH, COLLATION_NAME "
                        + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                (rs, row) -> new ColumnInfo(rs.getString("DATA_TYPE"), rs.getString("IS_NULLABLE"),
                        rs.getLong("CHARACTER_MAXIMUM_LENGTH"), rs.getString("COLLATION_NAME")), TABLE, column);
        if (result.isEmpty()) return false;
        ColumnInfo info = result.get(0);
        return expectedType.equalsIgnoreCase(info.type()) && info.length() == expectedLength
                && nullable.equalsIgnoreCase(info.nullable())
                && (collation == null || collation.equalsIgnoreCase(info.collation()));
    }

    private IndexInfo indexInfo(String indexName) {
        List<IndexColumn> columns = jdbcTemplate.query(
                "SELECT NON_UNIQUE, COLUMN_NAME, SUB_PART FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? ORDER BY SEQ_IN_INDEX",
                (rs, row) -> new IndexColumn(rs.getInt("NON_UNIQUE"), rs.getString("COLUMN_NAME"), rs.getObject("SUB_PART")),
                TABLE, indexName);
        boolean matches = columns.size() == 1 && columns.get(0).nonUnique() == 0
                && "nombre_normalizado".equalsIgnoreCase(columns.get(0).name())
                && columns.get(0).prefix() == null;
        return new IndexInfo(!columns.isEmpty(), matches);
    }

    private String ids(List<PlanRow> rows) {
        return describeRows(rows);
    }

    private String idsOnly(List<PlanRow> rows) {
        return rows.stream().map(row -> String.valueOf(row.planId())).collect(Collectors.joining(", "));
    }

    private String describeRows(List<PlanRow> rows) {
        return rows.stream().map(row -> "plan_id=" + row.planId() + " nombre='" + row.name() + "'")
                .collect(Collectors.joining(", "));
    }

    private record PlanRow(Long planId, String name, String normalizedName, String type,
                           boolean normalizedNameExists, boolean versionExists) {
        private String expectedNormalizedName() {
            return PlanSuscripcion.claveNombre(name);
        }
    }

    private record ColumnInfo(String type, String nullable, long length, String collation) {
    }

    private record IndexColumn(int nonUnique, String name, Object prefix) {
    }

    private record IndexInfo(boolean exists, boolean matches) {
    }

    public record MigrationResult(boolean changed, int normalizedNamesBackfilled) {
    }
}
