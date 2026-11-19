package com.dev.cutly.notificacion.dto;

import java.time.LocalDateTime;

public record NotificacionDto(Long notificacionId, String tipoEvento, String titulo, String mensaje,
                              Long negocioId, String recursoTipo, Long recursoId,
                              boolean leida, LocalDateTime fechaCreacion, LocalDateTime fechaLectura) { }
