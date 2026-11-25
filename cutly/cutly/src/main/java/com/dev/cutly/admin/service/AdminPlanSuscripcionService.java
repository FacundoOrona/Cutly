package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.plan.ActualizarPlanSuscripcionRequestDto;
import com.dev.cutly.admin.dto.plan.AdminPlanSuscripcionDto;
import com.dev.cutly.admin.dto.plan.CrearPlanSuscripcionRequestDto;
import com.dev.cutly.auditoria.entity.AuditLog;
import com.dev.cutly.auditoria.repository.AuditoriaRepository;
import com.dev.cutly.suscripcion.config.PlanSuscripcionReadiness;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class AdminPlanSuscripcionService {

    private final PlanSuscripcionRepository planRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanSuscripcionReadiness readiness;
    private final ObjectMapper objectMapper;
    @PersistenceContext
    private EntityManager entityManager;

    public AdminPlanSuscripcionService(
            PlanSuscripcionRepository planRepository,
            AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository,
            PlanSuscripcionReadiness readiness,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.readiness = readiness;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminPlanSuscripcionDto> listar() {
        return planRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminPlanSuscripcionDto obtener(Long id) {
        return aDto(buscarPlan(id));
    }

    @Transactional
    public AdminPlanSuscripcionDto crear(
            CrearPlanSuscripcionRequestDto request,
            String emailActor,
            String ipAddress
    ) {
        verificarInicializacion();
        String nombrePresentacion = PlanSuscripcion.normalizarNombrePresentacion(request.nombre());
        validarLongitudNombre(nombrePresentacion);
        String nombreNormalizado = PlanSuscripcion.claveNombre(nombrePresentacion);
        if (planRepository.existsByNombreNormalizado(nombreNormalizado)) {
            throw conflicto("Ya existe un plan con ese nombre");
        }

        Usuario actor = obtenerActor(emailActor);
        PlanSuscripcion plan = PlanSuscripcion.crearPlanComercial(
                nombrePresentacion, request.descripcion(), request.precioMensual(), request.maxNegocios()
        );
        planRepository.save(plan);
        entityManager.flush();
        registrarAuditoria("CREATE", plan.getPlanId(), Map.of("nuevo", snapshot(plan, true)), actor, ipAddress);
        return aDto(plan);
    }

    @Transactional
    public AdminPlanSuscripcionDto editar(
            Long id,
            ActualizarPlanSuscripcionRequestDto request,
            String emailActor,
            String ipAddress
    ) {
        verificarInicializacion();
        PlanSuscripcion plan = buscarPlan(id);
        verificarVersion(plan, request.versionEsperada());
        verificarComercial(plan);

        String nombrePresentacion = PlanSuscripcion.normalizarNombrePresentacion(request.nombre());
        validarLongitudNombre(nombrePresentacion);
        String nombreNormalizado = PlanSuscripcion.claveNombre(nombrePresentacion);
        List<PlanSuscripcion> coincidencias = planRepository.findByNombreNormalizado(nombreNormalizado);
        if (coincidencias.stream().anyMatch(otro -> !otro.getPlanId().equals(plan.getPlanId()))) {
            throw conflicto("Ya existe un plan con ese nombre");
        }

        Map<String, Object> anterior = snapshot(plan, true);
        if (Objects.equals(plan.getNombre(), nombrePresentacion)
                && Objects.equals(plan.getDescripcion(), request.descripcion())
                && mismoImporte(plan.getPrecioMensual(), request.precioMensual())
                && Objects.equals(plan.getMaxNegocios(), request.maxNegocios())) {
            return aDto(plan);
        }

        plan.actualizarDatosComerciales(nombrePresentacion, request.descripcion(), request.precioMensual(), request.maxNegocios());
        planRepository.save(plan);
        entityManager.flush();
        Map<String, Object> nuevo = snapshot(plan, true);
        registrarAuditoria("UPDATE", plan.getPlanId(), Map.of(
                "antes", anterior,
                "despues", nuevo,
                "cambios", diferencias(anterior, nuevo)
        ), obtenerActor(emailActor), ipAddress);
        return aDto(plan);
    }

    @Transactional
    public AdminPlanSuscripcionDto activar(Long id, Long versionEsperada, String emailActor, String ipAddress) {
        return cambiarEstado(id, versionEsperada, true, emailActor, ipAddress);
    }

    @Transactional
    public AdminPlanSuscripcionDto desactivar(Long id, Long versionEsperada, String emailActor, String ipAddress) {
        return cambiarEstado(id, versionEsperada, false, emailActor, ipAddress);
    }

    private AdminPlanSuscripcionDto cambiarEstado(
            Long id,
            Long versionEsperada,
            boolean activo,
            String emailActor,
            String ipAddress
    ) {
        verificarInicializacion();
        PlanSuscripcion plan = buscarPlan(id);
        verificarVersion(plan, versionEsperada);
        verificarComercial(plan);
        if (plan.isActivo() == activo) {
            return aDto(plan);
        }

        boolean estadoAnterior = plan.isActivo();
        String accion = activo ? "ACTIVATE" : "DEACTIVATE";
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", plan.getNombre());
        datos.put("tipo", plan.getTipo());
        datos.put("estadoAnterior", estadoAnterior);
        datos.put("estadoNuevo", activo);

        plan.cambiarActivo(activo);
        planRepository.save(plan);
        entityManager.flush();
        registrarAuditoria(accion, plan.getPlanId(), datos, obtenerActor(emailActor), ipAddress);
        return aDto(plan);
    }

    private void verificarInicializacion() {
        if (!readiness.isReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, readiness.mensajeNoDisponible());
        }
    }

    private PlanSuscripcion buscarPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el plan con id " + id));
    }

    private void verificarVersion(PlanSuscripcion plan, Long versionEsperada) {
        if (plan.getVersion() == null) {
            throw conflicto("El plan todavía requiere inicialización de versión");
        }
        if (!plan.getVersion().equals(versionEsperada)) {
            throw conflicto("El plan fue modificado por otro administrador; vuelva a cargarlo");
        }
    }

    private void verificarComercial(PlanSuscripcion plan) {
        if (plan.getTipo() == null || plan.getTipo() != com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion.COMERCIAL) {
            throw conflicto("Solo se permiten operaciones sobre planes COMERCIAL clasificados");
        }
    }

    private Usuario obtenerActor(String emailActor) {
        if (emailActor == null || emailActor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al administrador autenticado");
        }
        Usuario actor = usuarioRepository.findByEmail(emailActor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al administrador autenticado"));
        if (actor.getRol() != Rol.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es SUPER_ADMIN");
        }
        return actor;
    }

    private void validarLongitudNombre(String nombre) {
        if (nombre.isBlank() || nombre.codePointCount(0, nombre.length()) > 50) {
            throw new IllegalArgumentException("El nombre normalizado debe contener entre 1 y 50 caracteres");
        }
        if (PlanSuscripcion.claveNombre(nombre).equals(PlanSuscripcion.claveNombre("Prueba gratuita"))) {
            throw new IllegalArgumentException("El nombre Prueba gratuita está reservado para el plan TRIAL");
        }
    }

    private boolean mismoImporte(BigDecimal primero, BigDecimal segundo) {
        return primero != null && segundo != null && primero.compareTo(segundo) == 0;
    }

    private Map<String, Object> snapshot(PlanSuscripcion plan, boolean completo) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("planId", plan.getPlanId());
        snapshot.put("nombre", plan.getNombre());
        snapshot.put("tipo", plan.getTipo());
        snapshot.put("activo", plan.isActivo());
        if (completo) {
            snapshot.put("descripcion", plan.getDescripcion());
            snapshot.put("precioMensual", plan.getPrecioMensual());
            snapshot.put("maxNegocios", plan.getMaxNegocios());
        }
        return snapshot;
    }

    private Map<String, Object> diferencias(Map<String, Object> anterior, Map<String, Object> nuevo) {
        Map<String, Object> cambios = new LinkedHashMap<>();
        anterior.forEach((campo, valorAnterior) -> {
            Object valorNuevo = nuevo.get(campo);
            boolean iguales = valorAnterior instanceof BigDecimal anteriorDecimal
                    && valorNuevo instanceof BigDecimal nuevoDecimal
                    ? anteriorDecimal.compareTo(nuevoDecimal) == 0
                    : Objects.equals(valorAnterior, valorNuevo);
            if (!iguales) {
                Map<String, Object> valores = new LinkedHashMap<>();
                valores.put("anterior", valorAnterior);
                valores.put("nuevo", valorNuevo);
                cambios.put(campo, valores);
            }
        });
        return cambios;
    }

    private void registrarAuditoria(
            String accion,
            Long planId,
            Object detalle,
            Usuario actor,
            String ipAddress
    ) {
        AuditLog auditoria = new AuditLog();
        auditoria.setAccion(accion);
        auditoria.setEntidad("PlanSuscripcion");
        auditoria.setEntidadId(planId);
        auditoria.setDescripcion(serializar(detalle));
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setIpAddress(ipAddress);
        auditoria.setUsuario(actor);
        auditoriaRepository.save(auditoria);
    }

    private String serializar(Object detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría del plan", e);
        }
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private AdminPlanSuscripcionDto aDto(PlanSuscripcion plan) {
        return new AdminPlanSuscripcionDto(
                plan.getPlanId(),
                plan.getNombre(),
                plan.getDescripcion(),
                plan.getPrecioMensual(),
                plan.getMaxNegocios(),
                plan.getTipo(),
                plan.isActivo(),
                plan.getVersion()
        );
    }
}
