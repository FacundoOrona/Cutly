package com.dev.cutly.owner.dto.notificacion;

import java.time.LocalDateTime;

public record OwnerNotificacionDto(
        Long notificacionId,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion
) {
}
