package com.dev.cutly.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String nombreUsuario,
        String nombre,
        String apellido,
        String email
) {
}
