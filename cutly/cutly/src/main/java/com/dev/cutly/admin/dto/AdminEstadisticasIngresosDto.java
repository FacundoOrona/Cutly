package com.dev.cutly.admin.dto;

import java.math.BigDecimal;

public record AdminEstadisticasIngresosDto(
        BigDecimal ingresosTotales,
        long cantidadPagos
) {
}
