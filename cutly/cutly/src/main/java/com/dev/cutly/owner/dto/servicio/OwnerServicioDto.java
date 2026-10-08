package com.dev.cutly.owner.dto.servicio;

import java.math.BigDecimal;

public record OwnerServicioDto(
        Long servicioId,
        Long negocioId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        boolean activo
) {
}
