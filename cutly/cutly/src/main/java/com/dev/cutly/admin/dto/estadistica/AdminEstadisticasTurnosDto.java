package com.dev.cutly.admin.dto.estadistica;

public record AdminEstadisticasTurnosDto(
        long totalTurnos,
        long pendientes,
        long confirmados,
        long enCurso,
        long completados,
        long cancelados,
        long ausencias,
        long rechazados
) {
}
