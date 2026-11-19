package com.dev.cutly.owner.dto.suscripcion;

import jakarta.validation.constraints.Size;

public record RenovarSuscripcionRequestDto(
        @Size(max = 1000) String observacion
) {
}
