-- A.3.4.3.1 — snapshots históricos de suscripción.
-- Aplicar manualmente y solo tras confirmar respaldo y esquema de destino.
-- No ejecutar desde la aplicación ni repetir el ALTER si las columnas existen.

-- PASO 1 — Verificar tabla objetivo y columnas existentes antes de modificar.
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'historial_suscripciones';

SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'historial_suscripciones'
  AND COLUMN_NAME IN (
      'plan_id_anterior', 'nombre_plan_anterior', 'plan_id_nuevo', 'nombre_plan_nuevo',
      'precio_contratado_anterior', 'precio_contratado_nuevo',
      'fecha_inicio_anterior', 'fecha_inicio_nueva'
  );

-- PASO 2 — Agregar snapshots nullable. Valores históricos preexistentes quedan intactos.
-- Ejecutar una sola vez, omitiendo columnas que ya existan con tipo compatible.
ALTER TABLE historial_suscripciones
    ADD COLUMN plan_id_anterior BIGINT NULL,
    ADD COLUMN nombre_plan_anterior VARCHAR(50) NULL,
    ADD COLUMN plan_id_nuevo BIGINT NULL,
    ADD COLUMN nombre_plan_nuevo VARCHAR(50) NULL,
    ADD COLUMN precio_contratado_anterior DECIMAL(10,2) NULL,
    ADD COLUMN precio_contratado_nuevo DECIMAL(10,2) NULL,
    ADD COLUMN fecha_inicio_anterior DATE NULL,
    ADD COLUMN fecha_inicio_nueva DATE NULL;

-- PASO 3 — Verificar existencia, tipos y nulabilidad de todos los snapshots.
SELECT COLUMN_NAME, DATA_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE,
       CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'historial_suscripciones'
  AND COLUMN_NAME IN (
      'plan_id_anterior', 'nombre_plan_anterior', 'plan_id_nuevo', 'nombre_plan_nuevo',
      'precio_contratado_anterior', 'precio_contratado_nuevo',
      'fecha_inicio_anterior', 'fecha_inicio_nueva'
  );

-- Los snapshots anteriores y nuevos son nullable por diseño: la asignación inicial
-- no tiene valores anteriores. El actor se valida en la fábrica y @PrePersist:
-- administrador_id XOR identificacion_sistema = 'SISTEMA'.
