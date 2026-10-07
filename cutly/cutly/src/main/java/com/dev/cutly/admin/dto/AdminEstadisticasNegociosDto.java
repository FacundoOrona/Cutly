package com.dev.cutly.admin.dto;

public record AdminEstadisticasNegociosDto(
        long totalNegocios,
        long negociosActivos,
        long negociosInactivos,
        long negociosSuspendidos
) {
}
