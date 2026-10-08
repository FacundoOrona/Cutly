package com.dev.cutly.owner.dto.estadistica;

import java.math.BigDecimal;

public record OwnerIngresosEstadisticaDto(
        BigDecimal ingresosTotales,
        long cantidadPagos
) {
}
