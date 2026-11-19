package com.dev.cutly.suscripcion.service;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        planSuscripcionRepository.inicializarVersionesNulas();
        List<PlanSuscripcion> planes = planSuscripcionRepository.findAll();

        Map<String, List<PlanSuscripcion>> porNombreNormalizado = planes.stream()
                .collect(Collectors.groupingBy(plan -> PlanSuscripcion.claveNombre(plan.getNombre())));
        List<String> colisiones = porNombreNormalizado.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " => " + entry.getValue().stream()
                        .map(plan -> "id=" + plan.getPlanId() + ", nombre='" + plan.getNombre() + "'")
                        .collect(Collectors.joining("; ")))
                .toList();
        if (!colisiones.isEmpty()) {
            throw new IllegalStateException("Hay colisiones de nombres normalizados en planes: "
                    + String.join(" | ", colisiones));
        }
        for (PlanSuscripcion plan : planes) {
            String nombreNormalizado = PlanSuscripcion.normalizarNombrePresentacion(plan.getNombre());
            String clave = PlanSuscripcion.claveNombre(nombreNormalizado);
            if (nombreNormalizado.codePointCount(0, nombreNormalizado.length()) > 50
                    || clave.codePointCount(0, clave.length()) > 100) {
                throw new IllegalStateException("El plan id=" + plan.getPlanId()
                        + " excede la longitud admitida después de normalizar su nombre");
            }
        }

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

        if (!candidatosNombre.isEmpty()) {
            PlanSuscripcion planTrial = candidatosNombre.get(0);
            if (!NOMBRE_PLAN_PRUEBA.equals(planTrial.getNombre())) {
                throw new IllegalStateException(
                        "Existe una variante ambigua del nombre reservado Prueba gratuita"
                );
            }
            if (planTrial.getTipo() == TipoPlanSuscripcion.COMERCIAL) {
                throw new IllegalStateException("El plan Prueba gratuita está clasificado como COMERCIAL");
            }
            planTrial.validarPropiedadesTrial();
            planTrial.clasificarComoTrial();
            planTrial.normalizarNombrePreexistente();
        } else {
            if (!candidatosTipo.isEmpty()) {
                throw new IllegalStateException("Existe un plan TRIAL contradictorio");
            }
            PlanSuscripcion planTrial = PlanSuscripcion.crearPlanTrial();
            planSuscripcionRepository.save(planTrial);
            candidatosNombre = List.of(planTrial);
        }

        int planesClasificados = 0;
        for (PlanSuscripcion plan : planes) {
            if (plan == (candidatosNombre.isEmpty() ? null : candidatosNombre.get(0))) continue;
            if (plan.getTipo() == TipoPlanSuscripcion.TRIAL) {
                throw new IllegalStateException("Solo puede existir un plan TRIAL");
            }
            if (plan.getPrecioMensual() == null || plan.getPrecioMensual().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("El plan preexistente id=" + plan.getPlanId()
                        + " tiene un precio incompatible con el tipo COMERCIAL");
            }
            if (plan.getTipo() == null) {
                plan.clasificarComoComercial();
                planesClasificados++;
            }
            plan.normalizarNombrePreexistente();
        }

        if (planesClasificados > 0) {
            logger.info("Se clasificaron {} planes preexistentes como COMERCIAL", planesClasificados);
        }

        return candidatosNombre.get(0);
    }
}
