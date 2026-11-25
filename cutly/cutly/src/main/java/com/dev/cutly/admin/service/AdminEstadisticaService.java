package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.estadistica.AdminEstadisticasNegociosDto;
import com.dev.cutly.admin.dto.estadistica.AdminEstadisticasIngresosDto;
import com.dev.cutly.admin.dto.estadistica.AdminEstadisticasSuscripcionesDto;
import com.dev.cutly.admin.dto.estadistica.AdminEstadisticasTurnosDto;
import com.dev.cutly.admin.dto.estadistica.AdminEstadisticasUsuariosDto;
import com.dev.cutly.admin.dto.estadistica.AdminResumenEstadisticasDto;
import com.dev.cutly.admin.dto.estadistica.AdminIndicadoresGeneralesDto;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Service
public class AdminEstadisticaService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NegocioRepository negocioRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final TurnoRepository turnoRepository;
    private final PagoRepository pagoRepository;

    public AdminEstadisticaService(
            UsuarioRepository usuarioRepository,
            EmpleadoRepository empleadoRepository,
            NegocioRepository negocioRepository,
            SuscripcionRepository suscripcionRepository,
            TurnoRepository turnoRepository,
            PagoRepository pagoRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.negocioRepository = negocioRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.turnoRepository = turnoRepository;
        this.pagoRepository = pagoRepository;
    }

    public AdminResumenEstadisticasDto obtenerResumen() {
        return new AdminResumenEstadisticasDto(
                usuarioRepository.count(),
                empleadoRepository.count(),
                negocioRepository.count(),
                suscripcionRepository.count()
        );
    }

    public AdminEstadisticasUsuariosDto obtenerEstadisticasUsuarios() {
        return new AdminEstadisticasUsuariosDto(
                usuarioRepository.count(),
                usuarioRepository.countByStatus(UsuarioStatus.ACTIVE),
                usuarioRepository.countByStatus(UsuarioStatus.INACTIVE),
                usuarioRepository.countByStatus(UsuarioStatus.BLOCKED),
                usuarioRepository.countByRol(Rol.CLIENT),
                usuarioRepository.countByRol(Rol.OWNER),
                usuarioRepository.countByRol(Rol.EMPLOYEE),
                usuarioRepository.countByRol(Rol.SUPER_ADMIN)
        );
    }

    public AdminEstadisticasNegociosDto obtenerEstadisticasNegocios() {
        return new AdminEstadisticasNegociosDto(
                negocioRepository.count(),
                negocioRepository.countByStatus(NegocioStatus.ACTIVE),
                negocioRepository.countByStatus(NegocioStatus.INACTIVE),
                negocioRepository.countByStatus(NegocioStatus.SUSPENDED)
        );
    }

    public AdminEstadisticasTurnosDto obtenerEstadisticasTurnos() {
        return new AdminEstadisticasTurnosDto(
                turnoRepository.count(),
                turnoRepository.countByStatus(TurnoStatus.PENDING),
                turnoRepository.countByStatus(TurnoStatus.CONFIRMED),
                turnoRepository.countByStatus(TurnoStatus.IN_PROGRESS),
                turnoRepository.countByStatus(TurnoStatus.COMPLETED),
                turnoRepository.countByStatus(TurnoStatus.CANCELLED),
                turnoRepository.countByStatus(TurnoStatus.NO_SHOW),
                turnoRepository.countByStatus(TurnoStatus.REJECTED)
        );
    }

    public AdminEstadisticasIngresosDto obtenerEstadisticasIngresos() {
        BigDecimal ingresos = pagoRepository.sumarMontos();
        return new AdminEstadisticasIngresosDto(
                ingresos == null ? BigDecimal.ZERO : ingresos,
                pagoRepository.count()
        );
    }

    public AdminEstadisticasSuscripcionesDto obtenerEstadisticasSuscripciones() {
        return new AdminEstadisticasSuscripcionesDto(
                suscripcionRepository.count(),
                suscripcionRepository.countByStatus(SuscripcionStatus.PENDING),
                suscripcionRepository.countByStatus(SuscripcionStatus.TRIAL),
                suscripcionRepository.countByStatus(SuscripcionStatus.ACTIVE),
                suscripcionRepository.countByStatus(SuscripcionStatus.EXPIRED),
                suscripcionRepository.countByStatus(SuscripcionStatus.CANCELLED),
                suscripcionRepository.countByStatus(SuscripcionStatus.SUSPENDED)
        );
    }

    public AdminIndicadoresGeneralesDto obtenerIndicadoresGenerales(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Las fechas desde y hasta son obligatorias y hasta no puede ser anterior a desde");
        }
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        LocalDate hoy = com.dev.cutly.suscripcion.service.RelojSuscripcion.fechaActual();
        Map<SuscripcionStatus, Long> porEstado = new EnumMap<>(SuscripcionStatus.class);
        for (SuscripcionStatus estado : SuscripcionStatus.values()) {
            porEstado.put(estado, suscripcionRepository.countByStatus(estado));
        }
        Map<String, Long> porPlan = new LinkedHashMap<>();
        suscripcionRepository.contarPorPlan().forEach(fila -> porPlan.put((String) fila[0], (Long) fila[1]));
        Map<TurnoStatus, Long> turnosEstado = new EnumMap<>(TurnoStatus.class);
        turnoRepository.contarPorEstadoPeriodoGlobal(inicio, finExclusivo)
                .forEach(fila -> turnosEstado.put((TurnoStatus) fila[0], (Long) fila[1]));
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        turnoRepository.evolucionDiariaGlobal(inicio, finExclusivo).forEach(fila -> {
            Object fecha = fila[0];
            LocalDate dia = fecha instanceof LocalDate localDate ? localDate
                    : fecha instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate()
                    : LocalDate.parse(fecha.toString());
            porDia.put(dia, (Long) fila[1]);
        });
        long propietarios = usuarioRepository.countByRol(Rol.OWNER);
        long activos = usuarioRepository.countByRolAndStatus(Rol.OWNER, UsuarioStatus.ACTIVE);
        long inactivos = usuarioRepository.countByRolAndStatus(Rol.OWNER, UsuarioStatus.INACTIVE);
        long bloqueados = usuarioRepository.countByRolAndStatus(Rol.OWNER, UsuarioStatus.BLOCKED);
        long negociosActivos = negocioRepository.countByStatus(NegocioStatus.ACTIVE);
        long negociosInactivos = negocioRepository.countByStatus(NegocioStatus.INACTIVE);
        long negociosSuspendidos = negocioRepository.countByStatus(NegocioStatus.SUSPENDED);
        long negocios = negocioRepository.count();
        long habilitados = negocioRepository.contarConSuscripcionHabilitada(hoy);
        long totalTurnos = turnosEstado.values().stream().mapToLong(Long::longValue).sum();
        return new AdminIndicadoresGeneralesDto(desde, hasta, propietarios, activos, inactivos, bloqueados,
                negocios, negociosActivos, negociosInactivos, negociosSuspendidos, habilitados, negocios - habilitados,
                porEstado, suscripcionRepository.contarProximasAVencer(hoy, hoy.plusDays(30)),
                suscripcionRepository.contarVencidasPorFecha(hoy), porPlan, totalTurnos, turnosEstado,
                turnoRepository.contarNegociosConActividad(inicio, finExclusivo), porDia);
    }
}
