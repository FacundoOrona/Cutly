package com.dev.cutly.admin.dto.auditoria;

import java.time.LocalDateTime;

public record AdminAuditoriaDto(
        Long auditoriaId,
        String accion,
        String entidad,
        Long entidadId,
        String descripcion,
        LocalDateTime fecha,
        String ipAddress,
        Long usuarioId,
        String emailUsuario
) {
}
