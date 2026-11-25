package com.dev.cutly.suscripcion.entity;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valores escalares de una suscripción capturados en un instante del flujo. */
public record SnapshotSuscripcion(
        Long planId,
        String nombrePlan,
        Integer maxNegocios,
        BigDecimal precioContratado,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        SuscripcionStatus estado
) {
    public static SnapshotSuscripcion capturar(Suscripcion suscripcion) {
        if (suscripcion == null) {
            throw new IllegalArgumentException("La suscripción es obligatoria para crear un snapshot");
        }
        return new SnapshotSuscripcion(
                suscripcion.getPlan() == null ? null : suscripcion.getPlan().getPlanId(),
                suscripcion.getPlan() == null ? null : suscripcion.getPlan().getNombre(),
                suscripcion.getPlan() == null ? null : suscripcion.getPlan().getMaxNegocios(),
                suscripcion.getPrecioContratado(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.getStatus()
        );
    }
}
