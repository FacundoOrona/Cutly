package com.dev.cutly.owner.dto.estadistica;

import java.math.BigDecimal;

public record OwnerIngresosEstadisticaDto(
        BigDecimal ingresosTotales,
        long cantidadPagos,
        BigDecimal ingresosCobrosDirectos,
        BigDecimal ingresosAbonos,
        long cantidadCobrosDirectos,
        long cantidadAbonos,
        long turnosCobrados,
        long turnosGratuitos,
        BigDecimal deudaPendienteTotal,
        long deudasPendientes,
        long deudasParciales,
        long deudasPagadas,
        long turnosPendientesRegularizacion
) {
}
