package com.dev.cutly.admin.dto.cliente;

import com.dev.cutly.usuario.enums.UsuarioStatus;

public record AdminClienteDto(
        Long usuarioId,
        String nombre,
        String apellido,
        String dni,
        String email,
        UsuarioStatus status
) {
}
