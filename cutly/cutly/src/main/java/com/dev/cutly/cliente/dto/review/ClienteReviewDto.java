package com.dev.cutly.cliente.dto.review;

public record ClienteReviewDto(
        Long reviewId,
        Integer puntuacion,
        String comentario,
        Long turnoId,
        Long negocioId,
        String negocioNombre
) {
}
