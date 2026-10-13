package com.dev.cutly.cliente.dto.perfil;

import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;

public record ClientePerfilDto(Long usuarioId, String nombre, String apellido, String dni,
                               String email, Rol rol, UsuarioStatus status) {
}
