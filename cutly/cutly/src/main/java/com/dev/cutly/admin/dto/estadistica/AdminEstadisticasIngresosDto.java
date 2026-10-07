package com.dev.cutly.admin.dto.estadistica;

import java.math.BigDecimal;

public record AdminEstadisticasIngresosDto(
        BigDecimal ingresosTotales,
        long cantidadPagos
) {
}
