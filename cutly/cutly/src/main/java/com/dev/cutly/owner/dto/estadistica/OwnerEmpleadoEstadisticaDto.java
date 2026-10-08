package com.dev.cutly.owner.dto.estadistica;

public record OwnerEmpleadoEstadisticaDto(
        Long empleadoId,
        String nombre,
        String apellido,
        long totalTurnos,
        long turnosCompletados,
        long turnosCancelados
) {
}
