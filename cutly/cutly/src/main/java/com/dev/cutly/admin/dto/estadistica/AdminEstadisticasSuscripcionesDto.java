package com.dev.cutly.admin.dto.estadistica;

public record AdminEstadisticasSuscripcionesDto(
        long totalSuscripciones,
        long enPrueba,
        long activas,
        long vencidas,
        long canceladas,
        long suspendidas
) {
}
