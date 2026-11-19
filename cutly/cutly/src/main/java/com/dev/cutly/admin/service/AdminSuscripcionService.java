package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.suscripcion.AdminSuscripcionDto;
import com.dev.cutly.admin.dto.suscripcion.AsignarSuscripcionRequestDto;
import com.dev.cutly.admin.dto.suscripcion.RegularizarSuscripcionRequestDto;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.entity.SnapshotSuscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import com.dev.cutly.suscripcion.service.HistorialSuscripcionService;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AdminSuscripcionService {

    // GESTION MANUAL DE DIAS QUE DURA LA SUSCRIPCION

    private static final int DIAS_PROXIMOS_A_VENCER = 30;
    private static final Set<SuscripcionStatus> ESTADOS_ACTIVOS = Set.of(
            SuscripcionStatus.ACTIVE,
            SuscripcionStatus.TRIAL
    );

    private final SuscripcionRepository suscripcionRepository;
    private final HistorialSuscripcionService historialService;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final PlanSuscripcionRepository planRepository;

    public AdminSuscripcionService(
            SuscripcionRepository suscripcionRepository,
            HistorialSuscripcionService historialService,
            UsuarioRepository usuarioRepository,
            NegocioRepository negocioRepository,
            PlanSuscripcionRepository planRepository
    ) {
        this.suscripcionRepository = suscripcionRepository;
        this.historialService = historialService;
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarSuscripciones() {
        return suscripcionRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminSuscripcionDto obtenerSuscripcion(Long id) {
        return suscripcionRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarVencidas() {
        return suscripcionRepository.findByStatusOrFechaVencimientoBefore(
                        SuscripcionStatus.EXPIRED, RelojSuscripcion.fechaActual()
                ).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSuscripcionDto> listarProximasAVencer() {
        LocalDate hoy = RelojSuscripcion.fechaActual();
        return suscripcionRepository.findByFechaVencimientoBetweenAndStatusIn(
                        hoy, hoy.plusDays(DIAS_PROXIMOS_A_VENCER), ESTADOS_ACTIVOS
                ).stream().map(this::aDto).toList();
    }

    @Transactional
    public AdminSuscripcionDto actualizarEstado(
            Long id,
            SuscripcionStatus status,
            String emailAdministrador,
            String ipAddress,
            String observacion
    ) {
        Suscripcion suscripcion = suscripcionRepository.findWithLockingBySuscripcionId(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
        validarTransicion(suscripcion, status, suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(), RelojSuscripcion.fechaActual());
        if (suscripcion.getStatus() == status) {
            return aDto(suscripcion);
        }
        Usuario administrador = obtenerSuperAdmin(emailAdministrador);
        SnapshotSuscripcion anterior = SnapshotSuscripcion.capturar(suscripcion);
        suscripcion.setStatus(status);
        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        SnapshotSuscripcion nuevo = SnapshotSuscripcion.capturar(guardada);
        TipoEventoSuscripcion evento = eventoCambioEstado(anterior.estado(), status);
        String observacionFinal = observacionFinal(observacion, status);
        historialService.registrar(
                guardada, evento, null, anterior, nuevo, administrador, null, observacionFinal,
                "Cambio administrativo de estado a " + status, ipAddress
        );
        return aDto(guardada);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdminSuscripcionDto asignar(
            Long negocioId,
            AsignarSuscripcionRequestDto request,
            String emailAdministrador,
            String ipAddress
    ) {
        Usuario administrador = obtenerSuperAdmin(emailAdministrador);
        Negocio negocio = negocioRepository.findWithLockingByNegocioId(negocioId)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + negocioId));
        if (suscripcionRepository.existsByNegocio_NegocioId(negocioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El negocio ya tiene una suscripción; no se sobrescribirá");
        }

        PlanSuscripcion plan = planRepository.findWithLockingByPlanId(request.planId())
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el plan con id " + request.planId()));
        validarPlanComercialActivo(plan);
        LocalDate hoy = RelojSuscripcion.fechaActual();
        validarPeriodo(request.fechaInicio(), request.fechaVencimiento(), true, hoy);
        validarEstadoAsignacion(request.status(), request.fechaVencimiento(), hoy);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setNegocio(negocio);
        suscripcion.setPlan(plan);
        suscripcion.setFechaInicio(request.fechaInicio());
        suscripcion.setFechaVencimiento(request.fechaVencimiento());
        suscripcion.setStatus(request.status());
        suscripcion.asignarPrecioContratado(plan.getPrecioMensual());
        Suscripcion guardada = suscripcionRepository.save(suscripcion);

        boolean habilitado = request.status() == SuscripcionStatus.ACTIVE
                && !request.fechaInicio().isAfter(hoy)
                && !request.fechaVencimiento().isBefore(hoy)
                && negocio.getStatus() == NegocioStatus.INACTIVE;
        if (habilitado) {
            negocio.setStatus(NegocioStatus.ACTIVE);
            negocioRepository.save(negocio);
        }

        historialService.registrar(
                guardada,
                TipoEventoSuscripcion.SUBSCRIPTION_ASSIGNED,
                request.fechaInicio(),
                null,
                SnapshotSuscripcion.capturar(guardada),
                administrador,
                null,
                request.observacion(),
                habilitado
                        ? "Asignación administrativa de suscripción; negocio " + negocioId + " habilitado"
                        : "Asignación administrativa de suscripción al negocio " + negocioId,
                ipAddress
        );
        return aDto(guardada);
    }

    @Transactional
    public AdminSuscripcionDto regularizar(
            Long id,
            RegularizarSuscripcionRequestDto request,
            String emailAdministrador,
            String ipAddress
    ) {
        Usuario administrador = obtenerSuperAdmin(emailAdministrador);
        Suscripcion suscripcion = suscripcionRepository.findWithLockingBySuscripcionId(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
        if (!versionDe(suscripcion).equals(request.versionEsperada())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La suscripción cambió desde la última consulta; vuelva a cargarla antes de regularizar");
        }
        SnapshotSuscripcion anterior = SnapshotSuscripcion.capturar(suscripcion);

        boolean modificaCampo = request.planId() != null || request.fechaInicio() != null
                || request.fechaVencimiento() != null || request.status() != null
                || request.precioContratado() != null;
        if (!modificaCampo) {
            throw new IllegalArgumentException("Debe indicarse al menos un dato para regularizar");
        }

        PlanSuscripcion planActual = suscripcion.getPlan();
        PlanSuscripcion planNuevo = planActual;
        if (request.planId() != null) {
            if (!request.planId().equals(planActual.getPlanId())
                    && planActual.getTipo() == TipoPlanSuscripcion.TRIAL) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La conversión de una suscripción TRIAL queda fuera de esta operación");
            }
            planNuevo = planRepository.findWithLockingByPlanId(request.planId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "No se encontró el plan con id " + request.planId()));
            validarPlanComercialActivo(planNuevo);
        }

        LocalDate fechaInicioNueva = request.fechaInicio() == null
                ? suscripcion.getFechaInicio() : request.fechaInicio();
        LocalDate fechaVencimientoNueva = request.fechaVencimiento() == null
                ? suscripcion.getFechaVencimiento() : request.fechaVencimiento();
        LocalDate hoy = RelojSuscripcion.fechaActual();
        validarPeriodo(fechaInicioNueva, fechaVencimientoNueva, false, hoy);
        if (fechaVencimientoNueva.isAfter(suscripcion.getFechaVencimiento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La regularización no puede extender el vencimiento; utilice el flujo de renovación");
        }

        SuscripcionStatus estadoNuevo = request.status() == null
                ? suscripcion.getStatus() : request.status();
        if (estadoNuevo == SuscripcionStatus.TRIAL
                && suscripcion.getStatus() != SuscripcionStatus.TRIAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una suscripción comercial no puede convertirse en TRIAL");
        }
        if (estadoNuevo == SuscripcionStatus.TRIAL && planNuevo.getTipo() != TipoPlanSuscripcion.TRIAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una suscripción TRIAL debe conservar el plan Prueba gratuita");
        }
        if (estadoNuevo == SuscripcionStatus.CANCELLED && estadoNuevo != suscripcion.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cancelación debe realizarse mediante la operación administrativa de cancelación");
        }
        if (estadoNuevo != suscripcion.getStatus()) {
            validarTransicion(suscripcion, estadoNuevo, fechaInicioNueva, fechaVencimientoNueva, hoy);
        }

        suscripcion.setPlan(planNuevo);
        suscripcion.setFechaInicio(fechaInicioNueva);
        suscripcion.setFechaVencimiento(fechaVencimientoNueva);
        suscripcion.setStatus(estadoNuevo);
        if (request.precioContratado() != null) {
            if (planNuevo.getTipo() == TipoPlanSuscripcion.COMERCIAL
                    && request.precioContratado().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio de una suscripción COMERCIAL debe ser mayor que cero");
            }
            if (planNuevo.getTipo() == TipoPlanSuscripcion.TRIAL
                    && request.precioContratado().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("El precio de una suscripción TRIAL debe ser cero");
            }
            if (suscripcion.getPrecioContratado() == null
                    || suscripcion.getPrecioContratado().compareTo(request.precioContratado()) != 0) {
                suscripcion.corregirPrecioContratadoAdministrativamente(request.precioContratado());
            }
        }

        if (anterior.equals(SnapshotSuscripcion.capturar(suscripcion))) {
            return aDto(suscripcion);
        }

        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        historialService.registrar(
                guardada,
                eventoRegularizacion(anterior.estado(), guardada.getStatus()),
                null,
                anterior,
                SnapshotSuscripcion.capturar(guardada),
                administrador,
                null,
                request.observacion(),
                "Regularización administrativa de suscripción",
                ipAddress
        );
        return aDto(guardada);
    }

    private Usuario obtenerSuperAdmin(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No se pudo identificar al SUPER_ADMIN autenticado");
        }
        return usuarioRepository.findByEmail(email)
                .filter(usuario -> usuario.getRol() == Rol.SUPER_ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No se pudo validar al SUPER_ADMIN responsable"));
    }

    private void validarPlanComercialActivo(PlanSuscripcion plan) {
        if (plan.getTipo() != TipoPlanSuscripcion.COMERCIAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden asignar planes COMERCIAL");
        }
        if (!plan.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede asignar un plan inactivo");
        }
        if (plan.getPrecioMensual() == null
                || plan.getPrecioMensual().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El precio del plan COMERCIAL debe ser mayor que cero");
        }
        if (plan.getPrecioMensual().precision() - plan.getPrecioMensual().scale() > 8
                || plan.getPrecioMensual().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El precio del plan excede la precisión admitida");
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate vencimiento, boolean asignacion, LocalDate hoy) {
        if (inicio == null || vencimiento == null) {
            throw new IllegalArgumentException("Las fechas de inicio y vencimiento son obligatorias");
        }
        if (vencimiento.isBefore(inicio)) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior al inicio");
        }
        boolean periodoHistorico = vencimiento.isBefore(hoy);
        if (asignacion && !periodoHistorico && !vencimiento.equals(inicio.plusDays(29))) {
            throw new IllegalArgumentException("Una asignación vigente o futura debe durar 30 días inclusivos");
        }
    }

    private void validarEstadoAsignacion(SuscripcionStatus status, LocalDate vencimiento, LocalDate hoy) {
        if (status == null || status == SuscripcionStatus.TRIAL || status == SuscripcionStatus.CANCELLED) {
            throw new IllegalArgumentException("La asignación inicial admite ACTIVE, SUSPENDED o EXPIRED");
        }
        boolean vencido = vencimiento.isBefore(hoy);
        if ((status == SuscripcionStatus.EXPIRED && !vencido)
                || (status == SuscripcionStatus.ACTIVE && vencido)) {
            throw new IllegalArgumentException("El estado no coincide con las fechas de la asignación");
        }
    }

    private boolean transicionValida(SuscripcionStatus actual, SuscripcionStatus destino) {
        return switch (actual) {
            case TRIAL -> destino == SuscripcionStatus.ACTIVE
                    || destino == SuscripcionStatus.SUSPENDED || destino == SuscripcionStatus.CANCELLED;
            case ACTIVE -> destino == SuscripcionStatus.SUSPENDED || destino == SuscripcionStatus.CANCELLED;
            case SUSPENDED -> destino == SuscripcionStatus.ACTIVE || destino == SuscripcionStatus.CANCELLED;
            case EXPIRED -> destino == SuscripcionStatus.CANCELLED;
            case CANCELLED -> false;
        };
    }

    private void validarTransicion(
            Suscripcion suscripcion,
            SuscripcionStatus destino,
            LocalDate fechaInicio,
            LocalDate fechaVencimiento,
            LocalDate hoy
    ) {
        if (!transicionValida(suscripcion.getStatus(), destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado de la suscripción de " + suscripcion.getStatus() + " a " + destino);
        }
        if (destino == SuscripcionStatus.ACTIVE
                && fechaVencimiento.isBefore(hoy)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede activar o reactivar una suscripción vencida mediante la operación normal");
        }
        if (suscripcion.getStatus() == SuscripcionStatus.SUSPENDED
                && destino == SuscripcionStatus.ACTIVE
                && fechaInicio.isAfter(hoy)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reactivar una suscripción antes del inicio de su período");
        }
    }

    private TipoEventoSuscripcion eventoCambioEstado(
            SuscripcionStatus anterior,
            SuscripcionStatus nuevo
    ) {
        if (nuevo == SuscripcionStatus.CANCELLED) return TipoEventoSuscripcion.CANCELLED;
        if (nuevo == SuscripcionStatus.SUSPENDED) return TipoEventoSuscripcion.SUSPENDED;
        if (anterior == SuscripcionStatus.SUSPENDED && nuevo == SuscripcionStatus.ACTIVE) {
            return TipoEventoSuscripcion.REACTIVATED;
        }
        return TipoEventoSuscripcion.ADMINISTRATIVE_CHANGE;
    }

    private String observacionFinal(String observacion, SuscripcionStatus destino) {
        if (observacion != null && !observacion.isBlank()) return observacion.trim();
        return switch (destino) {
            case SUSPENDED -> "Suspensión administrativa";
            case ACTIVE -> "Reactivación administrativa";
            case CANCELLED -> "Cancelación administrativa";
            default -> "Cambio administrativo de estado";
        };
    }

    private TipoEventoSuscripcion eventoRegularizacion(
            SuscripcionStatus estadoAnterior,
            SuscripcionStatus estadoNuevo
    ) {
        if (estadoAnterior == estadoNuevo) return TipoEventoSuscripcion.ADMINISTRATIVE_CHANGE;
        if (estadoNuevo == SuscripcionStatus.SUSPENDED) return TipoEventoSuscripcion.SUSPENDED;
        if (estadoAnterior == SuscripcionStatus.SUSPENDED) return TipoEventoSuscripcion.REACTIVATED;
        return TipoEventoSuscripcion.ADMINISTRATIVE_CHANGE;
    }

    private AdminSuscripcionDto aDto(Suscripcion suscripcion) {
        return new AdminSuscripcionDto(
                suscripcion.getSuscripcionId(),
                suscripcion.getStatus(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.getPrecioContratado(),
                suscripcion.getPlan().getPlanId(),
                suscripcion.getPlan().getNombre(),
                suscripcion.getNegocio().getNegocioId(),
                suscripcion.getNegocio().getNombre(),
                versionDe(suscripcion)
        );
    }

    private String versionDe(Suscripcion suscripcion) {
        String precio = suscripcion.getPrecioContratado() == null
                ? "null" : suscripcion.getPrecioContratado().stripTrailingZeros().toPlainString();
        String estado = suscripcion.getStatus() == null ? "null" : suscripcion.getStatus().name();
        String contenido = String.join("|",
                String.valueOf(suscripcion.getSuscripcionId()),
                estado,
                String.valueOf(suscripcion.getFechaInicio()),
                String.valueOf(suscripcion.getFechaVencimiento()),
                precio,
                String.valueOf(suscripcion.getPlan().getPlanId())
        );
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No está disponible SHA-256", e);
        }
    }
}
