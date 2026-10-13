package com.dev.cutly.empleado.dto.notificacion;

import java.time.LocalDateTime;

public record EmpleadoNotificacionDto(Long notificacionId, String titulo, String mensaje,
                                      boolean leida, LocalDateTime fechaCreacion) {
}
