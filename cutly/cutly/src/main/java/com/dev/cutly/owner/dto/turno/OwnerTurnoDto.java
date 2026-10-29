package com.dev.cutly.owner.dto.turno;

import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.enums.TipoResponsableTurno;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerTurnoDto(
        Long turnoId,
        Long negocioId,
        Long clienteId,
        String clienteNombre,
        String clienteApellido,
        Long empleadoId,
        String empleadoNombre,
        String empleadoApellido,
        Long servicioId,
        String servicioNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        TurnoStatus status,
        BigDecimal precio,
        String observaciones,
        Long clienteLocalId,
        TipoClienteTurno tipoCliente,
        Long ownerResponsableId,
        TipoResponsableTurno tipoResponsable,
        String responsableNombre,
        String responsableApellido
) {
}
