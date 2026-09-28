package com.dev.cutly.admin.dto;

import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;

public record AdminUsuarioDto(
        Long usuarioId,
        String nombre,
        String apellido,
        String dni,
        String email,
        Rol rol,
        UsuarioStatus status
) {
}
