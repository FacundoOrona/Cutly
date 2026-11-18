package com.dev.cutly.suscripcion.service;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlanSuscripcionService {

    private static final String NOMBRE_PLAN_PRUEBA = "Prueba gratuita";
    private static final BigDecimal PRECIO_PLAN_PRUEBA = BigDecimal.ZERO;

    private final PlanSuscripcionRepository planSuscripcionRepository;

    public PlanSuscripcionService(PlanSuscripcionRepository planSuscripcionRepository) {
        this.planSuscripcionRepository = planSuscripcionRepository;
    }

    @Transactional
    public PlanSuscripcion obtenerOCrearPlanPruebaGratuita() {
        List<PlanSuscripcion> candidatos = planSuscripcionRepository
                .findByNombreIgnoreCase(NOMBRE_PLAN_PRUEBA);

        if (candidatos.size() > 1) {
            throw new IllegalStateException(
                    "Hay más de un plan que coincide con el nombre de prueba gratuita"
            );
        }

        if (!candidatos.isEmpty()) {
            PlanSuscripcion plan = candidatos.get(0);
            if (!NOMBRE_PLAN_PRUEBA.equals(plan.getNombre())
                    || plan.getPrecioMensual() == null
                    || plan.getPrecioMensual().compareTo(PRECIO_PLAN_PRUEBA) != 0
                    || !plan.isActivo()) {
                throw new IllegalStateException(
                        "El plan Prueba gratuita existente no cumple el nombre, precio y estado aprobados"
                );
            }
            return plan;
        }

        PlanSuscripcion plan = new PlanSuscripcion();
        plan.setNombre(NOMBRE_PLAN_PRUEBA);
        plan.setPrecioMensual(PRECIO_PLAN_PRUEBA);
        plan.setActivo(true);
        return planSuscripcionRepository.save(plan);
    }
}
