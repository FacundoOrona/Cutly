package com.dev.cutly.admin.dto.plan;

import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;

import java.math.BigDecimal;

public record AdminPlanSuscripcionDto(
        Long planId,
        String nombre,
        String descripcion,
        BigDecimal precioMensual,
        TipoPlanSuscripcion tipo,
        boolean activo,
        Long version
) {
}
