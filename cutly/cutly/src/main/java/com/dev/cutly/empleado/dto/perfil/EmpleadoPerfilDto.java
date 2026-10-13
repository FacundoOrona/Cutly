package com.dev.cutly.empleado.dto.perfil;

import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;

public record EmpleadoPerfilDto(Long empleadoId, Long usuarioId, String nombre, String apellido,
                                String dni, String email, Rol rol, UsuarioStatus status,
                                Long negocioId, boolean activo) {
}
