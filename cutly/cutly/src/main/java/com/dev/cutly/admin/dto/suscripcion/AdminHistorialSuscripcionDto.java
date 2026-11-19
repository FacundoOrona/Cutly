package com.dev.cutly.admin.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminHistorialSuscripcionDto(
        Long historialSuscripcionId,
        TipoEventoSuscripcion tipoEvento,
        SuscripcionStatus estadoAnterior,
        SuscripcionStatus estadoNuevo,
        Long planIdAnterior,
        String nombrePlanAnterior,
        Long planIdNuevo,
        String nombrePlanNuevo,
        BigDecimal precioContratadoAnterior,
        BigDecimal precioContratadoNuevo,
        LocalDate fechaInicioPeriodo,
        LocalDate fechaInicioAnterior,
        LocalDate fechaInicioNueva,
        LocalDate fechaVencimientoAnterior,
        LocalDate fechaVencimientoNueva,
        Long actorId,
        String actorNombre,
        String actorIdentificacion,
        LocalDateTime fechaEvento,
        String observacion
) {}
