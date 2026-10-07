package com.dev.cutly.admin.dto;

public record AdminEstadisticasSuscripcionesDto(
        long totalSuscripciones,
        long enPrueba,
        long activas,
        long vencidas,
        long canceladas,
        long suspendidas
) {
}
