package com.dev.cutly.admin.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminSuscripcionDto(
        Long suscripcionId,
        SuscripcionStatus status,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        BigDecimal precioContratado,
        Long planId,
        String nombrePlan,
        Long ownerId,
        String nombreOwner,
        String emailOwner,
        Integer maxNegocios,
        long cantidadNegocios,
        long capacidadDisponible,
        String version
) {
}
