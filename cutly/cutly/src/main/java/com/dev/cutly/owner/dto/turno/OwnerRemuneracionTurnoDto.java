package com.dev.cutly.owner.dto.turno;

import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerRemuneracionTurnoDto(
        Long remuneracionId,
        Long turnoId,
        Long empleadoId,
        Long negocioId,
        Long resultadoFinancieroId,
        EstadoRemuneracion estado,
        BigDecimal precioTurnoOriginal,
        BigDecimal precioAplicado,
        BigDecimal porcentajeAplicado,
        BigDecimal montoCalculado,
        boolean precioCorregidoManualmente,
        Long ownerCorrectorId,
        LocalDateTime fechaCorreccionPrecio,
        String motivoCorreccionPrecio,
        BigDecimal porcentajeComisionResueltoManualmente,
        Long ownerResolutorComisionId,
        LocalDateTime fechaResolucionComision,
        String motivoResolucionComision,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaConfirmacion,
        String detalleRevision
) { }
