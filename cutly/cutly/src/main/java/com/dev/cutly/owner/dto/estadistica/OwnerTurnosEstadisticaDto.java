package com.dev.cutly.owner.dto.estadistica;

public record OwnerTurnosEstadisticaDto(
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
