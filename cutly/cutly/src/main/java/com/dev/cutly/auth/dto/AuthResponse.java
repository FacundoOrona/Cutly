package com.dev.cutly.auth.dto;

import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;

public record AuthResponse(
        String token,
        Long userId,
        String dni,
        String nombre,
        String apellido,
        String email,
        UsuarioStatus status,
        Rol rol
) {
}
