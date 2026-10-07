package com.dev.cutly.owner.dto.empleado;

public record OwnerEmpleadoDto(
        Long empleadoId,
        Long usuarioId,
        Long negocioId,
        String nombre,
        String apellido,
        String email,
        String dni,
        boolean activo
) {
}
