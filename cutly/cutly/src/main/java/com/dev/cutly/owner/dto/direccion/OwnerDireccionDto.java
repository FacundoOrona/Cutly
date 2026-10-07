package com.dev.cutly.owner.dto.direccion;

public record OwnerDireccionDto(
        Long direccionId,
        Long negocioId,
        String calle,
        String numero,
        String ciudad,
        String provincia,
        String codigoPostal
) {
}
