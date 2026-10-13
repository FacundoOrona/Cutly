package com.dev.cutly.cliente.dto.disponibilidad;

import java.time.LocalDateTime;

public record ClienteDisponibilidadDto(
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        Long servicioId,
        Long empleadoId
) {
}
