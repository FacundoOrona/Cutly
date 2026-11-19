package com.dev.cutly.admin.dto.plan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CambiarEstadoPlanSuscripcionRequestDto(
        @NotNull(message = "La versión esperada es obligatoria")
        @PositiveOrZero(message = "La versión no puede ser negativa")
        Long versionEsperada
) {
}
