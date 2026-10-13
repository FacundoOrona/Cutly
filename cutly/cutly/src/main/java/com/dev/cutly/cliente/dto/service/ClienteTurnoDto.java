package com.dev.cutly.cliente.dto.service;

import com.dev.cutly.turno.enums.TurnoStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteTurnoDto(
        Long turnoId,
        Long negocioId,
        String negocioNombre,
        Long clienteId,
        Long empleadoId,
        String empleadoNombre,
        String empleadoApellido,
        Long servicioId,
        String servicioNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        TurnoStatus status,
        BigDecimal precio,
        String observaciones
) {
}
