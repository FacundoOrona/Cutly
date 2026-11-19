package com.dev.cutly.owner.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnerSuscripcionDto(
        Long suscripcionId,
        SuscripcionStatus status,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        BigDecimal precioContratado,
        Long planId,
        String nombrePlan
) {
}
