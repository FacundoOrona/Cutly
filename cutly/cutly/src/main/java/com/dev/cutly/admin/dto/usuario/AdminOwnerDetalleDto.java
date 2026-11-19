package com.dev.cutly.admin.dto.usuario;

import com.dev.cutly.admin.dto.negocio.AdminNegocioDto;
import com.dev.cutly.usuario.enums.UsuarioStatus;

import java.util.List;

public record AdminOwnerDetalleDto(
        Long ownerId,
        String nombre,
        String apellido,
        String dni,
        String email,
        UsuarioStatus status,
        long cantidadNegocios,
        List<AdminNegocioDto> negocios
) { }
