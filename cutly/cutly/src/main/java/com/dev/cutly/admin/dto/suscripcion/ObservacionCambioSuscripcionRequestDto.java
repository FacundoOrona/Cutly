package com.dev.cutly.admin.dto.suscripcion;

import jakarta.validation.constraints.Size;

public record ObservacionCambioSuscripcionRequestDto(
        @Size(max = 1000) String observacion
) {
}
