package com.dev.cutly.admin.dto;

public record AdminReviewDto(
        Long reviewId,
        Integer puntuacion,
        String comentario,
        Long clienteId,
        String nombreCliente,
        Long negocioId,
        String nombreNegocio,
        Long turnoId
) {
}
