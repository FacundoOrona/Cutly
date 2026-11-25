package com.dev.cutly.suscripcion.service;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanSuscripcionService {

    private static final String NOMBRE_PLAN_PRUEBA = "Prueba gratuita";
    private static final Logger logger = LoggerFactory.getLogger(PlanSuscripcionService.class);

    private final PlanSuscripcionRepository planSuscripcionRepository;

    public PlanSuscripcionService(PlanSuscripcionRepository planSuscripcionRepository) {
        this.planSuscripcionRepository = planSuscripcionRepository;
    }

    @Transactional
    public PlanSuscripcion obtenerOCrearPlanPruebaGratuita() {
        List<PlanSuscripcion> planes = planSuscripcionRepository.findAll();
        List<PlanSuscripcion> candidatosNombre = planes.stream()
                .filter(plan -> PlanSuscripcion.claveNombre(plan.getNombre())
                        .equals(PlanSuscripcion.claveNombre(NOMBRE_PLAN_PRUEBA)))
                .toList();
        List<PlanSuscripcion> candidatosTipo = planes.stream()
                .filter(plan -> plan.getTipo() == TipoPlanSuscripcion.TRIAL)
                .toList();

        if (candidatosNombre.size() > 1 || candidatosTipo.size() > 1) {
            throw new IllegalStateException("Se detectaron varios candidatos al plan TRIAL");
        }

        if (!candidatosTipo.isEmpty()
                && (candidatosNombre.isEmpty() || candidatosTipo.get(0) != candidatosNombre.get(0))) {
            throw new IllegalStateException(
                    "Existe un plan TRIAL cuyo nombre no coincide exactamente con Prueba gratuita"
            );
        }

        if (candidatosNombre.isEmpty()) {
            if (!candidatosTipo.isEmpty()) {
                throw new IllegalStateException("Existe un plan TRIAL contradictorio: plan_id="
                        + candidatosTipo.get(0).getPlanId());
            }
            PlanSuscripcion planTrial = PlanSuscripcion.crearPlanTrial();
            planSuscripcionRepository.save(planTrial);
            logger.info("Se creó el plan TRIAL porque no existía uno previamente");
            return planTrial;
        }

        PlanSuscripcion planTrial = candidatosNombre.get(0);
        if (!NOMBRE_PLAN_PRUEBA.equals(planTrial.getNombre())) {
            throw new IllegalStateException("El plan plan_id=" + planTrial.getPlanId()
                    + " tiene una variante ambigua del nombre reservado Prueba gratuita");
        }
        if (planTrial.getTipo() != TipoPlanSuscripcion.TRIAL) {
            throw new IllegalStateException("El plan Prueba gratuita debe estar clasificado como TRIAL; plan_id="
                    + planTrial.getPlanId());
        }
        planTrial.validarPropiedadesTrial();
        return planTrial;
    }
}
