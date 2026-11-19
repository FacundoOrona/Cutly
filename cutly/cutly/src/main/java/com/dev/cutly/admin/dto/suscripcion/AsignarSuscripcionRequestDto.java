package com.dev.cutly.admin.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AsignarSuscripcionRequestDto(
        @NotNull Long planId,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaVencimiento,
        @NotNull SuscripcionStatus status,
        @Size(max = 1000) String observacion
) {
}
