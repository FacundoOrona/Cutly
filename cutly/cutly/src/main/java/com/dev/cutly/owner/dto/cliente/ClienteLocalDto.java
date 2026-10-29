package com.dev.cutly.owner.dto.cliente;

public record ClienteLocalDto(
        Long clienteLocalId,
        String nombre,
        String apellido,
        String dniPresentado,
        String telefono
) {
}
