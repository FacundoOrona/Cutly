package com.dev.cutly.admin.dto.estadistica;

public record AdminResumenEstadisticasDto(
        long totalUsuarios,
        long totalEmpleados,
        long totalNegocios,
        long totalSuscripciones
) {
}
