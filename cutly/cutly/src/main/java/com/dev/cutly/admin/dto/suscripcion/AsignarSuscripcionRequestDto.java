package com.dev.cutly.admin.dto.suscripcion;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AsignarSuscripcionRequestDto(
        @NotNull Long planId,
        @Size(max = 1000) String observacion
) {
}
