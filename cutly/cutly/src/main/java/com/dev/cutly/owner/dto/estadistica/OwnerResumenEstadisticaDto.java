package com.dev.cutly.owner.dto.estadistica;

import java.math.BigDecimal;

public record OwnerResumenEstadisticaDto(
        long totalTurnos,
        long turnosPendientes,
        long turnosConfirmados,
        long turnosEnCurso,
        long turnosCompletados,
        long turnosCancelados,
        long ausencias,
        long turnosRechazados,
        BigDecimal ingresosTotales,
        long cantidadPagos
) {
}
