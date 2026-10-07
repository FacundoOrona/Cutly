package com.dev.cutly.admin.dto.empleado;

import com.dev.cutly.usuario.enums.UsuarioStatus;

public record AdminEmpleadoDto(
        Long usuarioId,
        String nombre,
        String apellido,
        String dni,
        String email,
        UsuarioStatus status
) {
}
