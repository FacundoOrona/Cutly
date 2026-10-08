package com.dev.cutly.owner.dto.estadistica;

public record OwnerServicioEstadisticaDto(
        Long servicioId,
        String nombre,
        long totalTurnos,
        long turnosCompletados,
        long turnosCancelados
) {
}
