package com.dev.cutly.admin.dto.estadistica;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.turno.enums.TurnoStatus;
import java.time.LocalDate;
import java.util.Map;

public record AdminIndicadoresGeneralesDto(
        LocalDate desde,
        LocalDate hasta,
        long totalPropietarios,
        long propietariosActivos,
        long propietariosInactivos,
        long propietariosBloqueados,
        long totalNegocios,
        long negociosActivos,
        long negociosInactivos,
        long negociosSuspendidos,
        long negociosConSuscripcionVigente,
        long negociosSinSuscripcionHabilitada,
        Map<SuscripcionStatus, Long> suscripcionesPorEstado,
        long suscripcionesProximasAVencer,
        long suscripcionesVencidas,
        Map<String, Long> suscripcionesPorPlan,
        long turnosEnPeriodo,
        Map<TurnoStatus, Long> turnosPorEstado,
        long negociosConActividad,
        Map<LocalDate, Long> turnosPorDia
) { }
