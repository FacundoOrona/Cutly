package com.dev.cutly.admin.dto.estadistica;

public record AdminEstadisticasNegociosDto(
        long totalNegocios,
        long negociosActivos,
        long negociosInactivos,
        long negociosSuspendidos
) {
}
