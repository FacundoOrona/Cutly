package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.AdminEstadisticasNegociosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasIngresosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasSuscripcionesDto;
import com.dev.cutly.admin.dto.AdminEstadisticasTurnosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasUsuariosDto;
import com.dev.cutly.admin.dto.AdminResumenEstadisticasDto;
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
                suscripcionRepository.countByStatus(SuscripcionStatus.TRIAL),
                suscripcionRepository.countByStatus(SuscripcionStatus.ACTIVE),
                suscripcionRepository.countByStatus(SuscripcionStatus.EXPIRED),
                suscripcionRepository.countByStatus(SuscripcionStatus.CANCELLED),
                suscripcionRepository.countByStatus(SuscripcionStatus.SUSPENDED)
        );
    }
}
