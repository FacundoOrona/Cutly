package com.dev.cutly.owner.dto.review;

public record OwnerReviewDto(
        Long reviewId,
        Integer puntuacion,
        String comentario,
        Long clienteId,
        String nombreCliente,
        Long negocioId,
        Long turnoId
) {
}
