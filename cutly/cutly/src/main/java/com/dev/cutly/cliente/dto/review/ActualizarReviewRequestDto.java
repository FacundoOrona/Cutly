package com.dev.cutly.cliente.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ActualizarReviewRequestDto(
        @Min(1) @Max(5) Integer puntuacion,
        String comentario
) {
}
