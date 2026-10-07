package com.dev.cutly.admin.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;

import java.time.LocalDate;

public record AdminSuscripcionDto(
        Long suscripcionId,
        SuscripcionStatus status,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        Long planId,
        String nombrePlan,
        Long negocioId,
        String nombreNegocio
) {
}
