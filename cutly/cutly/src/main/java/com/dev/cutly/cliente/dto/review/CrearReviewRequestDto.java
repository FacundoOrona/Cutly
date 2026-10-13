package com.dev.cutly.cliente.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearReviewRequestDto(
        @NotNull Long turnoId,
        @NotNull @Min(1) @Max(5) Integer puntuacion,
        String comentario
) {
}
