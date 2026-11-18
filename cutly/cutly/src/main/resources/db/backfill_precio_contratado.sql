-- Backfill técnico y reejecutable. Los valores copiados reflejan el precio
-- actual del plan; no constituyen precios históricos comprobados.
START TRANSACTION;

UPDATE suscripciones AS s
JOIN planes_suscripcion AS p ON p.plan_id = s.plan_id
SET s.precio_contratado = p.precio_mensual
WHERE s.precio_contratado IS NULL;

COMMIT;

-- Debe devolver 0 antes de endurecer la columna.
SELECT COUNT(*) AS suscripciones_sin_precio_contratado
FROM suscripciones
WHERE precio_contratado IS NULL;

-- Debe devolver 0.
SELECT COUNT(*) AS suscripciones_con_precio_negativo
FROM suscripciones
WHERE precio_contratado < 0;

-- Ejecutar en una etapa posterior, solo después de verificar ambos resultados:
-- ALTER TABLE suscripciones
--     MODIFY COLUMN precio_contratado DECIMAL(10,2) NOT NULL;
