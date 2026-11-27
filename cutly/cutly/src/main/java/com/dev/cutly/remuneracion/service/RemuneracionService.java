package com.dev.cutly.remuneracion.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.owner.dto.turno.CorregirPrecioHistoricoRequestDto;
import com.dev.cutly.owner.dto.turno.OwnerRemuneracionTurnoDto;
import com.dev.cutly.owner.dto.turno.ResolverComisionRemuneracionRequestDto;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import com.dev.cutly.remuneracion.repository.RemuneracionEmpleadoRepository;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RemuneracionService {
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MAX_IMPORTE = new BigDecimal("99999999.99");
    private static final BigDecimal MAX_PORCENTAJE = new BigDecimal("100.00");

    private final RemuneracionEmpleadoRepository remuneracionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;

    public RemuneracionService(RemuneracionEmpleadoRepository remuneracionRepository,
                               EmpleadoRepository empleadoRepository,
                               EmpleadoServiciorRepository empleadoServicioRepository) {
        this.remuneracionRepository = remuneracionRepository;
        this.empleadoRepository = empleadoRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
    }

    public void asegurarPendiente(Turno turno) {
        if (turno.getEmpleado() == null) return;
        RemuneracionEmpleado existente = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId()).orElse(null);
        if (existente == null) {
            remuneracionRepository.save(RemuneracionEmpleado.pendiente(turno, LocalDateTime.now()));
        } else if (existente.getEstado() != EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado de remuneración del turno no permite completar la operación");
        }
    }

    public void conservarExcepcionesAlDesasignar(List<Turno> turnos, EmpleadoServicio asignacion) {
        BigDecimal porcentaje = asignacion.getPorcentajeComision();
        for (Turno turno : turnos) {
            if (turno.getEmpleado() == null) continue;
            RemuneracionEmpleado remuneracion = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId())
                    .orElseGet(() -> RemuneracionEmpleado.pendiente(turno, LocalDateTime.now()));
            if (remuneracion.getEstado() != EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Un turno completado tiene un estado de remuneración incompatible con la desasignación");
            }
            if (porcentaje != null) remuneracion.preservarExcepcion(porcentaje);
            remuneracionRepository.save(remuneracion);
        }
    }

    public void resolverDecision(Turno turno, ResultadoFinancieroTurno resultado, boolean completarTurno) {
        if (turno.getEmpleado() == null) return;
        var existente = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId());
        boolean teniaPendiente = existente.isPresent();
        RemuneracionEmpleado remuneracion = existente
                .orElseGet(() -> RemuneracionEmpleado.pendiente(turno, LocalDateTime.now()));
        if (remuneracion.getEstado() != EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno ya tiene una remuneración o una revisión pendiente incompatible");
        }

        if (resultado.getTipoResultado() == DecisionFinanciera.GRATUIDAD) {
            remuneracion.cerrarSinRemuneracion(resultado);
            remuneracionRepository.save(remuneracion);
            return;
        }

        BigDecimal porcentaje = resolverPorcentaje(turno, remuneracion, completarTurno || teniaPendiente);
        if (porcentaje == null) {
            remuneracion.pendienteRevision(resultado,
                    "No se puede determinar la comisión histórica porque el servicio ya no está asignado y no hay snapshot",
                    true);
            remuneracionRepository.save(remuneracion);
            return;
        }
        validarPorcentaje(porcentaje);
        if (turno.getPrecio() == null) {
            remuneracion.pendienteRevision(resultado,
                    "El turno no conserva un precio histórico; OWNER debe corregirlo manualmente",
                    false);
            remuneracion.preservarPorcentajePendiente(porcentaje);
            remuneracionRepository.save(remuneracion);
            return;
        }

        remuneracion.confirmar(resultado, turno.getPrecio(), porcentaje,
                calcular(turno.getPrecio(), porcentaje), LocalDateTime.now());
        remuneracionRepository.save(remuneracion);
    }

    public void marcarDecisionAnteriorParaRevision(Turno turno, ResultadoFinancieroTurno resultado) {
        if (turno.getEmpleado() == null) return;
        RemuneracionEmpleado remuneracion = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId())
                .orElse(null);
        if (remuneracion == null) {
            remuneracion = RemuneracionEmpleado.pendienteRevisionAnterior(turno, resultado, LocalDateTime.now());
            remuneracionRepository.save(remuneracion);
        } else if (remuneracion.getEstado() == EstadoRemuneracion.PENDIENTE_DEFINICION_FINANCIERA) {
            remuneracion.pendienteRevision(resultado,
                    "La decisión financiera es anterior al registro de snapshots de remuneración", true);
            remuneracionRepository.save(remuneracion);
        }
    }

    public OwnerRemuneracionTurnoDto corregirPrecioHistorico(Turno turno, ResultadoFinancieroTurno resultado,
            CorregirPrecioHistoricoRequestDto request, Usuario owner) {
        validarImporte(request.importeHistorico());
        if (resultado == null || resultado.getTipoResultado() == DecisionFinanciera.GRATUIDAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede corregir el precio de un turno regularizado con cobro o cuenta corriente");
        }
        if (turno.getStatus() != com.dev.cutly.turno.enums.TurnoStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede corregir un turno completado");
        }
        if (turno.getEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno no tiene un empleado responsable y no genera remuneración");
        }

        RemuneracionEmpleado remuneracion = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay una remuneración pendiente de revisión para el turno"));
        String motivo = request.motivo().trim();
        boolean mismaCorreccion = remuneracion.isPrecioCorregidoManualmente()
                && remuneracion.getPrecioAplicado().compareTo(request.importeHistorico()) == 0
                && motivo.equals(remuneracion.getMotivoCorreccionPrecio())
                && owner.getUsuarioId().equals(remuneracion.getOwnerCorrector().getUsuarioId());
        if (remuneracion.getEstado() == EstadoRemuneracion.CONFIRMADA) {
            if (mismaCorreccion) return dto(remuneracion);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una remuneración confirmada no admite cambios de precio");
        }
        if (remuneracion.getEstado() == EstadoRemuneracion.PENDIENTE_REVISION_MANUAL && mismaCorreccion) {
            return dto(remuneracion);
        }
        if (remuneracion.getEstado() != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL
                || remuneracion.getPrecioTurnoOriginal() != null
                || remuneracion.getPrecioAplicado() != null
                || remuneracion.isPrecioCorregidoManualmente()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno no está pendiente de corregir un precio histórico nulo");
        }

        BigDecimal porcentaje = remuneracion.getPorcentajeComisionPendiente() != null
                ? remuneracion.getPorcentajeComisionPendiente()
                : resolverPorcentaje(turno, remuneracion, !remuneracion.isRequiereRevisionComision());
        if (porcentaje != null) validarPorcentaje(porcentaje);
        if (motivo.length() < 3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El motivo de corrección debe tener al menos 3 caracteres");
        remuneracion.corregirPrecio(request.importeHistorico(), owner, motivo, LocalDateTime.now());
        if (porcentaje != null) {
            remuneracion.confirmarPrecioCorregido(porcentaje,
                    calcular(request.importeHistorico(), porcentaje), LocalDateTime.now());
        } else {
            remuneracion.marcarComisionEnRevision(
                    "El precio fue corregido, pero falta resolver manualmente la comisión histórica");
        }
        return dto(remuneracionRepository.save(remuneracion));
    }

    public OwnerRemuneracionTurnoDto resolverComisionManualmente(Turno turno, ResultadoFinancieroTurno resultado,
            ResolverComisionRemuneracionRequestDto request, Usuario owner) {
        if (resultado == null || resultado.getTipoResultado() == DecisionFinanciera.GRATUIDAD
                || turno.getStatus() != TurnoStatus.COMPLETED || turno.getEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede resolver la comisión de una remuneración regularizada y pendiente");
        }
        validarPorcentaje(request.porcentaje());
        String motivo = request.motivo().trim();
        if (motivo.length() < 3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El motivo de resolución debe tener al menos 3 caracteres");
        RemuneracionEmpleado remuneracion = remuneracionRepository.findByTurno_TurnoId(turno.getTurnoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay una remuneración pendiente de revisión para el turno"));
        if (remuneracion.getResultadoFinanciero() == null
                || !java.util.Objects.equals(remuneracion.getResultadoFinanciero().getResultadoFinancieroId(),
                        resultado.getResultadoFinancieroId())
                || remuneracion.getPorcentajeComisionPendiente() != null
                || remuneracion.getPorcentajeExcepcionPreservado() != null
                || remuneracion.getEstado() != EstadoRemuneracion.PENDIENTE_REVISION_MANUAL
                || !remuneracion.isRequiereRevisionComision()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La comisión de esta remuneración no admite resolución manual");
        }
        BigDecimal precio = remuneracion.getPrecioAplicado() != null
                ? remuneracion.getPrecioAplicado() : remuneracion.getPrecioTurnoOriginal();
        validarImporte(precio);
        remuneracion.resolverComisionManualmente(request.porcentaje(), owner, motivo, LocalDateTime.now(),
                calcular(precio, request.porcentaje()));
        return dto(remuneracionRepository.save(remuneracion));
    }

    public OwnerRemuneracionTurnoDto obtener(Long turnoId) {
        return remuneracionRepository.findByTurno_TurnoId(turnoId).map(this::dto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hay una remuneración registrada para el turno"));
    }

    private BigDecimal resolverPorcentaje(Turno turno, RemuneracionEmpleado remuneracion,
                                          boolean usarGeneralSiNoAsignado) {
        if (remuneracion.getPorcentajeExcepcionPreservado() != null) {
            return remuneracion.getPorcentajeExcepcionPreservado();
        }
        Empleado empleado = empleadoRepository.findWithLockingByEmpleadoId(turno.getEmpleado().getEmpleadoId())
                .orElse(turno.getEmpleado());
        if (turno.getNegocio() == null || empleado.getNegocio() == null || turno.getServicio() == null
                || turno.getServicio().getNegocio() == null
                || !turno.getNegocio().getNegocioId().equals(empleado.getNegocio().getNegocioId())
                || !turno.getNegocio().getNegocioId().equals(turno.getServicio().getNegocio().getNegocioId())) {
            return null;
        }
        var asignacion = empleadoServicioRepository.findWithLockingByEmpleadoAndServicio(
                empleado.getEmpleadoId(), turno.getServicio().getServicioId()).orElse(null);
        if (asignacion == null) {
            return usarGeneralSiNoAsignado ? empleado.getPorcentajeRemuneracion() : null;
        }
        if (!empleado.getNegocio().getNegocioId().equals(asignacion.getServicio().getNegocio().getNegocioId())
                || !empleado.getNegocio().getNegocioId().equals(asignacion.getEmpleado().getNegocio().getNegocioId())) {
            return null;
        }
        return asignacion.getPorcentajeComision() == null
                ? empleado.getPorcentajeRemuneracion() : asignacion.getPorcentajeComision();
    }

    private BigDecimal calcular(BigDecimal precio, BigDecimal porcentaje) {
        return precio.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private void validarImporte(BigDecimal importe) {
        if (importe == null || importe.signum() <= 0 || importe.compareTo(MAX_IMPORTE) > 0 || importe.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El importe debe ser positivo, admitir hasta dos decimales y no exceder 99999999.99");
        }
    }

    private void validarPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(MAX_PORCENTAJE) > 0
                || porcentaje.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El porcentaje debe estar entre 0.00 y 100.00 con hasta dos decimales");
        }
    }

    private OwnerRemuneracionTurnoDto dto(RemuneracionEmpleado r) {
        return new OwnerRemuneracionTurnoDto(r.getRemuneracionId(), r.getTurno().getTurnoId(),
                r.getEmpleado().getEmpleadoId(), r.getNegocio().getNegocioId(),
                r.getResultadoFinanciero() == null ? null : r.getResultadoFinanciero().getResultadoFinancieroId(),
                r.getEstado(), r.getPrecioTurnoOriginal(), r.getPrecioAplicado(), r.getPorcentajeAplicado(),
                r.getMontoCalculado(), r.isPrecioCorregidoManualmente(),
                r.getOwnerCorrector() == null ? null : r.getOwnerCorrector().getUsuarioId(),
                r.getFechaCorreccionPrecio(), r.getMotivoCorreccionPrecio(),
                r.getPorcentajeComisionResueltoManualmente(),
                r.getOwnerResolutorComision() == null ? null : r.getOwnerResolutorComision().getUsuarioId(),
                r.getFechaResolucionComision(), r.getMotivoResolucionComision(), r.getFechaGeneracion(),
                r.getFechaConfirmacion(), r.getDetalleRevision());
    }
}
