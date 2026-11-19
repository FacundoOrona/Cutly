package com.dev.cutly.admin.dto.negocio;

import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminNegocioDto(
        Long negocioId,
        String nombre,
        String descripcion,
        String telefono,
        String email,
        NegocioStatus status,
        String ownerNombre,
        String ownerApellido,
        String ownerEmail,
        Long suscripcionId,
        SuscripcionStatus estadoSuscripcion,
        String nombrePlan,
        TipoPlanSuscripcion tipoPlan,
        BigDecimal precioContratado,
        LocalDate fechaInicioSuscripcion,
        LocalDate fechaVencimientoSuscripcion,
        Long diasRestantes,
        Long diasAtraso,
        boolean suscripcionHabilitada
) {
}
