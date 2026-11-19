-- A.3.4.2.3 — migración escalonada para habilitar escrituras CRUD.
-- No ejecutar durante el desarrollo ni contra una base no respaldada.
-- Hibernate ddl-auto=update agrega nombre_normalizado y version_plan como
-- columnas nullable; el inicializador Java calcula las claves NFC exactas,
-- clasifica planes y completa versiones antes de que readiness permita escribir.
-- La entidad NO declara unique=true: el índice se crea aquí después del preflight.

-- PASO 1 — INSPECCIÓN: revisar nombres y claves calculadas por la aplicación.
-- Si el inicializador detecta colisiones, falla su transacción y sus logs
-- identifican los IDs/nombres. Resolver manualmente antes de reiniciar.
SELECT plan_id, nombre, nombre_normalizado, tipo
FROM planes_suscripcion
ORDER BY nombre;

SELECT nombre_normalizado, COUNT(*) AS cantidad
FROM planes_suscripcion
WHERE nombre_normalizado IS NOT NULL
GROUP BY nombre_normalizado
HAVING COUNT(*) > 1;

-- PASO 2 — MANUAL: si hay colisiones, acordar nombres únicos y corregirlos
-- por ID. No elegir ni renombrar registros automáticamente.
-- UPDATE planes_suscripcion SET nombre = 'Nombre aprobado' WHERE plan_id = <id>;
-- Reiniciar la aplicación para que el inicializador recalcule las claves Java.

-- PASO 3 — MODIFICACIÓN IDEMPOTENTE: completar versiones que sigan nulas.
UPDATE planes_suscripcion
SET version_plan = 0
WHERE version_plan IS NULL;

-- PASO 4 — INSPECCIÓN: todos los conteos deben dar 0 antes de continuar.
SELECT COUNT(*) AS planes_sin_nombre_normalizado
FROM planes_suscripcion
WHERE nombre_normalizado IS NULL;

SELECT COUNT(*) AS planes_sin_version
FROM planes_suscripcion
WHERE version_plan IS NULL;

SELECT COUNT(*) AS planes_sin_tipo_valido
FROM planes_suscripcion
WHERE tipo IS NULL OR tipo NOT IN ('TRIAL', 'COMERCIAL');

SELECT COUNT(*) AS planes_trial_inconsistentes
FROM planes_suscripcion
WHERE tipo = 'TRIAL'
  AND (BINARY nombre <> BINARY 'Prueba gratuita' OR precio_mensual <> 0 OR activo <> 1);

SELECT COUNT(*) AS cantidad_planes_trial
FROM planes_suscripcion
WHERE tipo = 'TRIAL';

-- PASO 5 — INSPECCIÓN: confirmar si ya existe el índice único requerido.
SELECT INDEX_NAME, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'planes_suscripcion'
  AND COLUMN_NAME = 'nombre_normalizado';

-- PASO 5 — MODIFICACIÓN: ejecutar solo si no existe ya un índice UNIQUE de
-- una sola columna llamado uq_planes_suscripcion_nombre_normalizado.
-- Los valores duplicados deben estar resueltos antes de ejecutar.
-- CREATE UNIQUE INDEX uq_planes_suscripcion_nombre_normalizado
--     ON planes_suscripcion (nombre_normalizado);

-- PASO 6 — MODIFICACIÓN: ejecutar solo tras conteos limpios e índice único activo.
-- ALTER TABLE planes_suscripcion
--     MODIFY COLUMN nombre_normalizado VARCHAR(100)
--         CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
--     MODIFY COLUMN version_plan BIGINT NOT NULL,
--     MODIFY COLUMN tipo VARCHAR(20) NOT NULL;

-- PASO 7 — MODIFICACIÓN: garantizar espacio para snapshots completos de auditoría.
-- ALTER TABLE audit_logs MODIFY COLUMN descripcion TEXT NULL;

-- PASO 8 — INSPECCIÓN: identificar el nombre real del índice histórico.
SELECT INDEX_NAME, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'planes_suscripcion'
  AND COLUMN_NAME = 'nombre';

-- PASO 8 — MODIFICACIÓN: solo después de confirmar que el índice único
-- normalizado está activo. Reemplazar por el nombre real obtenido arriba.
-- ALTER TABLE planes_suscripcion DROP INDEX nombre_indice_confirmado;

-- Después de aplicar los cambios manuales, reiniciar la aplicación. Readiness
-- verifica columnas, nulabilidad, collation, índices, clasificación y datos;
-- las escrituras administrativas permanecen en 503 hasta que todo esté listo.
