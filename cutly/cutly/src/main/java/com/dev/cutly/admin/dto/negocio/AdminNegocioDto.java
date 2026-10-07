package com.dev.cutly.admin.dto.negocio;

import com.dev.cutly.negocio.enums.NegocioStatus;

public record AdminNegocioDto(
        Long negocioId,
        String nombre,
        String descripcion,
        String telefono,
        String email,
        NegocioStatus status
) {
}
