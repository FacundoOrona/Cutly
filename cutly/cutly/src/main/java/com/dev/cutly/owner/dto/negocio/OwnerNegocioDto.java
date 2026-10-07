package com.dev.cutly.owner.dto.negocio;

import com.dev.cutly.negocio.enums.NegocioStatus;

public record OwnerNegocioDto(
        Long negocioId,
        String nombre,
        String descripcion,
        String telefono,
        String email,
        NegocioStatus status
) {
}
