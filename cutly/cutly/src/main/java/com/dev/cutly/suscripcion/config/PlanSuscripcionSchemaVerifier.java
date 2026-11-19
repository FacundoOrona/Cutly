package com.dev.cutly.suscripcion.config;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class PlanSuscripcionSchemaVerifier {

    public static final String INDICE_NOMBRE_NORMALIZADO = "uq_planes_suscripcion_nombre_normalizado";

    private final JdbcTemplate jdbcTemplate;
    private final PlanSuscripcionRepository planRepository;

    public PlanSuscripcionSchemaVerifier(
            JdbcTemplate jdbcTemplate,
            PlanSuscripcionRepository planRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<String> verificar() {
        List<String> pendientes = new ArrayList<>();
        ColumnInfo nombreNormalizado = columna("planes_suscripcion", "nombre_normalizado");
        ColumnInfo version = columna("planes_suscripcion", "version_plan");
        ColumnInfo tipo = columna("planes_suscripcion", "tipo");
        ColumnInfo descripcionAuditoria = columna("audit_logs", "descripcion");

        validarNombreNormalizado(nombreNormalizado, pendientes);
        validarVersion(version, pendientes);
        validarTipo(tipo, pendientes);
        validarDescripcionAuditoria(descripcionAuditoria, pendientes);

        if (nombreNormalizado != null && version != null && tipo != null) {
            validarDatosPlanes(pendientes);
        }
        if (nombreNormalizado != null && !indiceUnicoDeUnaColumna(
                "planes_suscripcion", INDICE_NOMBRE_NORMALIZADO, "nombre_normalizado")) {
            pendientes.add("Falta el índice único " + INDICE_NOMBRE_NORMALIZADO + ".");
        }
        if (columnaUnicaSola("planes_suscripcion", "nombre")) {
            pendientes.add("Debe retirarse el índice único histórico de nombre.");
        }
        return List.copyOf(pendientes);
    }

    private void validarNombreNormalizado(ColumnInfo column, List<String> pendientes) {
        if (column == null) {
            pendientes.add("Falta la columna planes_suscripcion.nombre_normalizado.");
            return;
        }
        if (!"varchar".equalsIgnoreCase(column.dataType())
                || column.characterLength() == null
                || column.characterLength() < 100
                || !"NO".equalsIgnoreCase(column.nullable())
                || !"utf8mb4_bin".equalsIgnoreCase(column.collationName())) {
            pendientes.add("planes_suscripcion.nombre_normalizado debe ser VARCHAR(100) NOT NULL con collation utf8mb4_bin.");
        }
    }

    private void validarVersion(ColumnInfo column, List<String> pendientes) {
        if (column == null) {
            pendientes.add("Falta la columna planes_suscripcion.version_plan.");
            return;
        }
        if (!"bigint".equalsIgnoreCase(column.dataType())
                || !"NO".equalsIgnoreCase(column.nullable())) {
            pendientes.add("planes_suscripcion.version_plan debe ser BIGINT NOT NULL.");
        }
    }

    private void validarTipo(ColumnInfo column, List<String> pendientes) {
        if (column == null) {
            pendientes.add("Falta la columna planes_suscripcion.tipo.");
            return;
        }
        if (!"varchar".equalsIgnoreCase(column.dataType())
                || column.characterLength() == null
                || column.characterLength() < 20
                || !"NO".equalsIgnoreCase(column.nullable())) {
            pendientes.add("planes_suscripcion.tipo debe ser VARCHAR(20) NOT NULL.");
        }
    }

    private void validarDescripcionAuditoria(ColumnInfo column, List<String> pendientes) {
        if (column == null) {
            pendientes.add("Falta la columna audit_logs.descripcion.");
            return;
        }
        String tipo = column.dataType().toLowerCase(Locale.ROOT);
        if (!List.of("text", "mediumtext", "longtext").contains(tipo)) {
            pendientes.add("audit_logs.descripcion debe ser TEXT o un tipo compatible de mayor capacidad.");
        }
    }

    private void validarDatosPlanes(List<String> pendientes) {
        if (contar("SELECT COUNT(*) FROM planes_suscripcion WHERE nombre_normalizado IS NULL") > 0) {
            pendientes.add("Hay planes sin nombre_normalizado.");
        }
        if (contar("SELECT COUNT(*) FROM planes_suscripcion WHERE version_plan IS NULL") > 0) {
            pendientes.add("Hay planes sin version_plan.");
        }
        if (contar("SELECT COUNT(*) FROM planes_suscripcion WHERE tipo IS NULL OR tipo NOT IN ('TRIAL', 'COMERCIAL')") > 0) {
            pendientes.add("Hay planes sin clasificación válida.");
        }
        if (contar("SELECT COUNT(*) FROM (SELECT nombre_normalizado FROM planes_suscripcion "
                + "GROUP BY nombre_normalizado HAVING COUNT(*) > 1) AS colisiones") > 0) {
            pendientes.add("Hay nombres normalizados duplicados.");
        }
        if (contar("SELECT COUNT(*) FROM planes_suscripcion WHERE tipo = 'TRIAL' "
                + "AND (BINARY nombre <> BINARY 'Prueba gratuita' OR precio_mensual <> 0 OR activo <> 1)") > 0
                || contar("SELECT COUNT(*) FROM planes_suscripcion WHERE tipo = 'TRIAL'") > 1) {
            pendientes.add("Las propiedades o la unicidad del plan TRIAL son inconsistentes.");
        }

        for (PlanSuscripcion plan : planRepository.findAll()) {
            String claveEsperada = PlanSuscripcion.claveNombre(plan.getNombre());
            if (!Objects.equals(claveEsperada, plan.getNombreNormalizado())) {
                pendientes.add("Hay claves normalizadas que no corresponden al nombre del plan.");
                break;
            }
            if (plan.getTipo() == TipoPlanSuscripcion.COMERCIAL
                    && (plan.getPrecioMensual() == null
                    || plan.getPrecioMensual().compareTo(BigDecimal.ZERO) <= 0)) {
                pendientes.add("Hay planes COMERCIAL con precio inválido.");
                break;
            }
        }
    }

    private ColumnInfo columna(String tabla, String columna) {
        List<ColumnInfo> resultados = jdbcTemplate.query(
                "SELECT DATA_TYPE, IS_NULLABLE, CHARACTER_MAXIMUM_LENGTH, COLLATION_NAME "
                        + "FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                (resultSet, rowNum) -> {
                    long length = resultSet.getLong("CHARACTER_MAXIMUM_LENGTH");
                    Long characterLength = resultSet.wasNull() ? null : length;
                    return new ColumnInfo(
                            resultSet.getString("DATA_TYPE"),
                            resultSet.getString("IS_NULLABLE"),
                            characterLength,
                            resultSet.getString("COLLATION_NAME")
                    );
                },
                tabla,
                columna
        );
        return resultados.isEmpty() ? null : resultados.get(0);
    }

    private boolean indiceUnicoDeUnaColumna(String tabla, String indice, String columna) {
        Integer columnas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? "
                        + "AND NON_UNIQUE = 0 AND SUB_PART IS NULL",
                Integer.class,
                tabla,
                indice
        );
        Integer objetivo = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? "
                        + "AND NON_UNIQUE = 0 AND COLUMN_NAME = ? AND SUB_PART IS NULL",
                Integer.class,
                tabla,
                indice,
                columna
        );
        return columnas != null && columnas == 1 && objetivo != null && objetivo == 1;
    }

    private boolean columnaUnicaSola(String tabla, String columna) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? AND NON_UNIQUE = 0 "
                        + "AND INDEX_NAME IN (SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? GROUP BY INDEX_NAME HAVING COUNT(*) = 1)",
                Integer.class,
                tabla,
                columna,
                tabla
        );
        return count != null && count > 0;
    }

    private long contar(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    private record ColumnInfo(String dataType, String nullable, Long characterLength, String collationName) {
    }
}
