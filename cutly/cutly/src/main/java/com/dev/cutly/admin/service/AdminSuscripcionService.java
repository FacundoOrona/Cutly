package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.suscripcion.AdminSuscripcionDto;
import com.dev.cutly.admin.dto.suscripcion.AdminHistorialSuscripcionDto;
import com.dev.cutly.admin.dto.suscripcion.AsignarSuscripcionRequestDto;
import com.dev.cutly.admin.dto.suscripcion.RegularizarSuscripcionRequestDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
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
import java.util.ArrayList;

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
    public Page<AdminSuscripcionDto> listarSuscripciones(SuscripcionStatus status, Long ownerId, Long planId, Pageable pageable) {
        if ((ownerId != null && ownerId < 1) || (planId != null && planId < 1)) {
            throw new IllegalArgumentException("Los identificadores de OWNER y plan deben ser mayores que cero");
        }
        Pageable ordenado = paginaValidada(pageable, "fechaInicio", Sort.Direction.DESC, "suscripcionId");
        Specification<Suscripcion> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (ownerId != null) predicates.add(cb.equal(root.get("owner").get("usuarioId"), ownerId));
            if (planId != null) predicates.add(cb.equal(root.get("plan").get("planId"), planId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return suscripcionRepository.findAll(filtros, ordenado).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public AdminSuscripcionDto obtenerSuscripcion(Long id) {
        return suscripcionRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la suscripción con id " + id));
    }

    @Transactional(readOnly = true)
    public AdminSuscripcionDto obtenerSuscripcionDeOwner(Long ownerId) {
        return suscripcionRepository.findByOwner_UsuarioId(ownerId)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("El OWNER no tiene una suscripción asignada"));
    }

    @Transactional(readOnly = true)
    public AdminSuscripcionDto consultarSuscripcionDeOwner(Long ownerId) {
        return suscripcionRepository.findByOwner_UsuarioId(ownerId).map(this::aDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<AdminSuscripcionDto> listarVencidas(Pageable pageable) {
        Pageable ordenado = paginaValidada(pageable, "fechaVencimiento", Sort.Direction.ASC, "suscripcionId");
        LocalDate hoy = RelojSuscripcion.fechaActual();
        Specification<Suscripcion> filtros = (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), SuscripcionStatus.EXPIRED),
                cb.and(cb.equal(root.get("status"), SuscripcionStatus.ACTIVE),
                        cb.lessThan(root.get("fechaVencimiento"), hoy)));
        return suscripcionRepository.findAll(filtros, ordenado).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public Page<AdminSuscripcionDto> listarProximasAVencer(Pageable pageable) {
        Pageable ordenado = paginaValidada(pageable, "fechaVencimiento", Sort.Direction.ASC, "suscripcionId");
        LocalDate hoy = RelojSuscripcion.fechaActual();
        Specification<Suscripcion> filtros = (root, query, cb) -> cb.and(
                root.get("status").in(ESTADOS_ACTIVOS),
                cb.between(root.get("fechaVencimiento"), hoy, hoy.plusDays(DIAS_PROXIMOS_A_VENCER)));
        return suscripcionRepository.findAll(filtros, ordenado).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public List<AdminHistorialSuscripcionDto> obtenerHistorial(Long id) {
        if (!suscripcionRepository.existsById(id)) throw new NoSuchElementException("No se encontró la suscripción con id " + id);
        return historialService.consultarPorSuscripcion(id)
                .stream().map(h -> new AdminHistorialSuscripcionDto(h.getHistorialSuscripcionId(), h.getTipoEvento(),
                        h.getEstadoAnterior(), h.getEstadoNuevo(), h.getPlanIdAnterior(), h.getNombrePlanAnterior(), h.getMaxNegociosAnterior(),
                        h.getPlanIdNuevo(), h.getNombrePlanNuevo(), h.getMaxNegociosNuevo(), h.getPrecioContratadoAnterior(), h.getPrecioContratadoNuevo(),
                        h.getFechaInicioPeriodo(), h.getFechaInicioAnterior(), h.getFechaInicioNueva(),
                        h.getFechaVencimientoAnterior(), h.getFechaVencimientoNueva(),
                        h.getAdministradorResponsable() == null ? null : h.getAdministradorResponsable().getUsuarioId(),
                        h.getAdministradorResponsable() == null ? null : h.getAdministradorResponsable().getNombre() + " " + h.getAdministradorResponsable().getApellido(),
                        h.getAdministradorResponsable() == null ? h.getIdentificacionSistema() : h.getAdministradorResponsable().getEmail(),
                        h.getFechaEvento(), h.getObservacion())).toList();
    }

    private Pageable paginaValidada(Pageable pageable, String campoPredeterminado, Sort.Direction direccion, String desempate) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw new IllegalArgumentException("La página debe ser >= 0 y el tamaño debe estar entre 1 y 100");
        Set<String> campos = Set.of("suscripcionId", "status", "fechaInicio", "fechaVencimiento", "precioContratado", "plan.planId", "owner.usuarioId");
        for (Sort.Order order : pageable.getSort()) if (!campos.contains(order.getProperty())) throw new IllegalArgumentException("Campo de ordenamiento no permitido: " + order.getProperty());
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(direccion, campoPredeterminado);
        if (sort.getOrderFor("suscripcionId") == null) {
            Sort.Order principal = sort.iterator().next();
            sort = sort.and(Sort.by(principal.getDirection(), desempate));
        }
        return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
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
        LocalDate hoy = RelojSuscripcion.fechaActual();
        boolean activacion = status == SuscripcionStatus.ACTIVE;
        if (activacion && suscripcion.getPlan().getTipo() != TipoPlanSuscripcion.COMERCIAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Asigne primero un plan COMERCIAL antes de activar la suscripción");
        }
        if (suscripcion.getStatus() == status && !activacion) {
            return aDto(suscripcion);
        }
        LocalDate inicioNuevo = activacion ? hoy : suscripcion.getFechaInicio();
        LocalDate vencimientoNuevo = activacion ? hoy.plusDays(30) : suscripcion.getFechaVencimiento();
        validarTransicion(suscripcion, status, inicioNuevo, vencimientoNuevo, hoy);
        Usuario administrador = obtenerSuperAdmin(emailAdministrador);
        SnapshotSuscripcion anterior = SnapshotSuscripcion.capturar(suscripcion);
        if (activacion) {
            suscripcion.setFechaInicio(inicioNuevo);
            suscripcion.setFechaVencimiento(vencimientoNuevo);
        }
        suscripcion.setStatus(status);
        Suscripcion guardada = suscripcionRepository.save(suscripcion);
        SnapshotSuscripcion nuevo = SnapshotSuscripcion.capturar(guardada);
        TipoEventoSuscripcion evento = activacion && anterior.estado() == SuscripcionStatus.ACTIVE
                ? TipoEventoSuscripcion.RENEWED : eventoCambioEstado(anterior.estado(), status);
        String observacionFinal = activacion && anterior.estado() == SuscripcionStatus.ACTIVE
                ? (observacion == null || observacion.isBlank() ? "Renovación administrativa" : observacion.trim())
                : observacionFinal(observacion, status);
        historialService.registrar(
                guardada, evento, activacion ? inicioNuevo : null, anterior, nuevo, administrador, null, observacionFinal,
                activacion ? (anterior.estado() == SuscripcionStatus.ACTIVE
                        ? "Renovación administrativa: nuevo período de 31 días desde " + hoy
                        : "Activación administrativa: período de 31 días desde " + hoy)
                        : "Cambio administrativo de estado a " + status, ipAddress
        );
        return aDto(guardada);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdminSuscripcionDto asignar(
            Long ownerId,
            AsignarSuscripcionRequestDto request,
            String emailAdministrador,
            String ipAddress
    ) {
        Usuario administrador = obtenerSuperAdmin(emailAdministrador);
        Usuario owner = usuarioRepository.findWithLockingByUsuarioId(ownerId)
                .filter(usuario -> usuario.getRol() == Rol.OWNER)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el OWNER con id " + ownerId));
        if (suscripcionRepository.existsByOwner_UsuarioId(ownerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El OWNER ya tiene una suscripción; no se sobrescribirá");
        }

        PlanSuscripcion plan = planRepository.findWithLockingByPlanId(request.planId())
                .orElseThrow(() -> new NoSuchElementException(
                        "No se encontró el plan con id " + request.planId()));
        validarPlanComercialActivo(plan);
        LocalDate hoy = RelojSuscripcion.fechaActual();

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setOwner(owner);
        suscripcion.setPlan(plan);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaVencimiento(hoy.plusDays(30));
        suscripcion.setStatus(SuscripcionStatus.PENDING);
        suscripcion.asignarPrecioContratado(plan.getPrecioMensual());
        Suscripcion guardada = suscripcionRepository.save(suscripcion);

        historialService.registrar(
                guardada,
                TipoEventoSuscripcion.SUBSCRIPTION_ASSIGNED,
                hoy,
                null,
                SnapshotSuscripcion.capturar(guardada),
                administrador,
                null,
                request.observacion(),
                "Asignación administrativa de suscripción pendiente de confirmación de pago al OWNER " + ownerId,
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

        boolean conversionPruebaAComercial = planActual.getTipo() == TipoPlanSuscripcion.TRIAL
                && planNuevo.getTipo() == TipoPlanSuscripcion.COMERCIAL;
        SuscripcionStatus estadoNuevo = request.status() == null
                ? (conversionPruebaAComercial ? SuscripcionStatus.SUSPENDED : suscripcion.getStatus())
                : request.status();
        if (estadoNuevo == SuscripcionStatus.ACTIVE && estadoNuevo != suscripcion.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La activación debe realizarse mediante la operación administrativa de activación");
        }
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
        BigDecimal precioContratadoNuevo = request.precioContratado();
        if (precioContratadoNuevo == null && conversionPruebaAComercial) {
            precioContratadoNuevo = planNuevo.getPrecioMensual();
        }
        if (precioContratadoNuevo != null) {
            if (planNuevo.getTipo() == TipoPlanSuscripcion.COMERCIAL
                    && precioContratadoNuevo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio de una suscripción COMERCIAL debe ser mayor que cero");
            }
            if (planNuevo.getTipo() == TipoPlanSuscripcion.TRIAL
                    && precioContratadoNuevo.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("El precio de una suscripción TRIAL debe ser cero");
            }
            if (suscripcion.getPrecioContratado() == null
                    || suscripcion.getPrecioContratado().compareTo(precioContratadoNuevo) != 0) {
                suscripcion.corregirPrecioContratadoAdministrativamente(precioContratadoNuevo);
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
        if (plan.getMaxNegocios() == null || plan.getMaxNegocios() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El plan debe permitir al menos un negocio");
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
        if (asignacion && !periodoHistorico && !vencimiento.equals(inicio.plusDays(30))) {
            throw new IllegalArgumentException("Una asignación vigente o futura debe durar 31 días inclusivos");
        }
    }

    private boolean transicionValida(SuscripcionStatus actual, SuscripcionStatus destino) {
        return switch (actual) {
            case PENDING -> destino == SuscripcionStatus.ACTIVE
                    || destino == SuscripcionStatus.SUSPENDED || destino == SuscripcionStatus.CANCELLED;
            case TRIAL -> destino == SuscripcionStatus.ACTIVE
                    || destino == SuscripcionStatus.SUSPENDED || destino == SuscripcionStatus.CANCELLED;
            case ACTIVE -> destino == SuscripcionStatus.ACTIVE
                    || destino == SuscripcionStatus.SUSPENDED || destino == SuscripcionStatus.CANCELLED;
            case SUSPENDED -> destino == SuscripcionStatus.ACTIVE || destino == SuscripcionStatus.CANCELLED;
            case EXPIRED -> destino == SuscripcionStatus.ACTIVE || destino == SuscripcionStatus.CANCELLED;
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
                    "El período de activación debe incluir la fecha actual");
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
        if (anterior == SuscripcionStatus.PENDING && nuevo == SuscripcionStatus.ACTIVE) {
            return TipoEventoSuscripcion.ACTIVATED;
        }
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
            case PENDING -> "Pendiente de confirmación de pago";
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
        long cantidadNegocios = negocioRepository.countByOwner_UsuarioId(suscripcion.getOwner().getUsuarioId());
        Integer maxNegocios = suscripcion.getPlan().getMaxNegocios();
        return new AdminSuscripcionDto(
                suscripcion.getSuscripcionId(),
                suscripcion.getStatus(),
                suscripcion.getFechaInicio(),
                suscripcion.getFechaVencimiento(),
                suscripcion.getPrecioContratado(),
                suscripcion.getPlan().getPlanId(),
                suscripcion.getPlan().getNombre(),
                suscripcion.getOwner().getUsuarioId(),
                suscripcion.getOwner().getNombre() + " " + suscripcion.getOwner().getApellido(),
                suscripcion.getOwner().getEmail(),
                maxNegocios,
                cantidadNegocios,
                maxNegocios == null ? 0 : Math.max(0, maxNegocios - cantidadNegocios),
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
