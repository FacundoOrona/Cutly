package com.dev.cutly.suscripcion.config;

import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class PlanSuscripcionInitializer implements ApplicationRunner {

    private final PlanSuscripcionService planSuscripcionService;

    public PlanSuscripcionInitializer(PlanSuscripcionService planSuscripcionService) {
        this.planSuscripcionService = planSuscripcionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            planSuscripcionService.obtenerOCrearPlanPruebaGratuita();
        } catch (DataIntegrityViolationException e) {
            // Una instancia concurrente pudo insertar el plan tras nuestra consulta inicial.
            planSuscripcionService.obtenerOCrearPlanPruebaGratuita();
        }
    }
}
