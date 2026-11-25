package com.dev.cutly.suscripcion.config;

import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlanSuscripcionInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PlanSuscripcionInitializer.class);

    private final PlanSuscripcionService planSuscripcionService;
    private final PlanSuscripcionSchemaMigrator schemaMigrator;
    private final PlanSuscripcionReadiness readiness;
    private final PlanSuscripcionSchemaVerifier schemaVerifier;

    public PlanSuscripcionInitializer(
            PlanSuscripcionService planSuscripcionService,
            PlanSuscripcionSchemaMigrator schemaMigrator,
            PlanSuscripcionReadiness readiness,
            PlanSuscripcionSchemaVerifier schemaVerifier
    ) {
        this.planSuscripcionService = planSuscripcionService;
        this.schemaMigrator = schemaMigrator;
        this.readiness = readiness;
        this.schemaVerifier = schemaVerifier;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            schemaMigrator.migrar();
            try {
                planSuscripcionService.obtenerOCrearPlanPruebaGratuita();
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                // Otra instancia pudo insertar o clasificar los mismos planes durante el arranque.
                planSuscripcionService.obtenerOCrearPlanPruebaGratuita();
            }
        } catch (RuntimeException e) {
            logger.error("No se completó la migración o inicialización de planes; las escrituras administrativas quedan bloqueadas", e);
            readiness.markBlocked(List.of(
                    e.getMessage() == null
                            ? "La migración de planes requiere revisión; consultar los logs de arranque"
                            : e.getMessage()
            ));
            return;
        }

        try {
            List<String> pendientes = schemaVerifier.verificar();
            if (pendientes.isEmpty()) {
                readiness.markReady();
            } else {
                readiness.markBlocked(pendientes);
                logger.warn("Las escrituras administrativas de planes quedan bloqueadas: {}",
                        String.join("; ", pendientes));
            }
        } catch (RuntimeException e) {
            logger.error("No se pudo verificar el esquema de planes; las escrituras administrativas quedan bloqueadas", e);
            readiness.markBlocked(List.of(
                    "No se pudo verificar el esquema de planes; revisar la configuración y los logs de arranque"
            ));
        }
    }
}
