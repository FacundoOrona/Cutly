package com.dev.cutly.suscripcion.service;

import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ControlAccesoSuscripcion {
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SuscripcionRepository suscripcionRepository;

    public ControlAccesoSuscripcion(UsuarioRepository usuarioRepository,
                                    NegocioRepository negocioRepository,
                                    EmpleadoRepository empleadoRepository,
                                    SuscripcionRepository suscripcionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.empleadoRepository = empleadoRepository;
        this.suscripcionRepository = suscripcionRepository;
    }

    public boolean tieneAcceso(String email, String rol, HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("ROLE_OWNER".equals(rol)
                && HttpMethod.PATCH.matches(request.getMethod())
                && (path.matches("/api/owner/notificaciones/\\d+/leer")
                || "/api/owner/notificaciones/leer-todas".equals(path))) return true;
        if ("ROLE_OWNER".equals(rol) && "/api/owner/suscripcion".equals(path)
                && esLectura(request)) return true;
        if ("ROLE_OWNER".equals(rol)) {
            Usuario owner = usuarioRepository.findByEmail(email).orElse(null);
            if (owner == null || owner.getRol() != Rol.OWNER) return false;
            if ("/api/owner/negocios".equals(path) && HttpMethod.POST.matches(request.getMethod())) {
                return ownerPuedeCrearNegocio(owner);
            }
            if ("/api/owner/negocios".equals(path) && HttpMethod.GET.matches(request.getMethod())) return true;
            return ownerTieneAcceso(owner.getUsuarioId(), esLectura(request));
        }
        Long ownerId = null;
        if ("ROLE_EMPLOYEE".equals(rol) && path.startsWith("/api/empleado/")) {
            ownerId = empleadoRepository.findByUsuario_Email(email)
                    .map(e -> e.getNegocio().getOwner().getUsuarioId()).orElse(null);
        } else if ("ROLE_CLIENT".equals(rol) && path.matches("/api/cliente/negocios/\\d+(/.*)?")) {
            String[] partes = path.split("/");
            ownerId = negocioRepository.findById(Long.parseLong(partes[4]))
                    .map(n -> n.getOwner().getUsuarioId()).orElse(null);
        }
        if (ownerId == null) return true;
        Suscripcion suscripcion = suscripcion(ownerId);
        return suscripcion != null && (esLectura(request)
                ? puedeConsultar(suscripcion) : suscripcionHabilitada(suscripcion));
    }

    public boolean clientePuedeReservar(Long negocioId) {
        return negocioRepository.findById(negocioId)
                .map(n -> suscripcion(n.getOwner().getUsuarioId()))
                .map(ControlAccesoSuscripcion::suscripcionHabilitada).orElse(false);
    }

    public boolean clientePuedeConsultarNegocio(Long negocioId) {
        return negocioRepository.findById(negocioId)
                .map(n -> suscripcion(n.getOwner().getUsuarioId()))
                .map(ControlAccesoSuscripcion::puedeConsultar).orElse(false);
    }

    public Set<Long> negociosConsultablesPorCliente(Collection<Long> negocioIds) {
        return negocioIds.stream().filter(this::clientePuedeConsultarNegocio).collect(java.util.stream.Collectors.toSet());
    }

    public static boolean suscripcionHabilitada(Suscripcion suscripcion) {
        return suscripcion != null
                && (suscripcion.getStatus() == SuscripcionStatus.ACTIVE
                || suscripcion.getStatus() == SuscripcionStatus.TRIAL)
                && !suscripcion.getFechaInicio().isAfter(RelojSuscripcion.fechaActual())
                && !suscripcion.getFechaVencimiento().isBefore(RelojSuscripcion.fechaActual());
    }

    private boolean ownerPuedeCrearNegocio(Usuario owner) {
        Suscripcion suscripcion = suscripcion(owner.getUsuarioId());
        if (suscripcion == null) return !owner.isTrialSuscripcionUtilizado();
        Integer maxNegocios = suscripcion.getPlan().getMaxNegocios();
        return suscripcionHabilitada(suscripcion) && maxNegocios != null
                && negocioRepository.countByOwner_UsuarioId(owner.getUsuarioId()) < maxNegocios;
    }

    private boolean ownerTieneAcceso(Long ownerId, boolean lectura) {
        Suscripcion suscripcion = suscripcion(ownerId);
        if (suscripcion == null) return false;
        if (lectura) return puedeConsultar(suscripcion);
        return suscripcionHabilitada(suscripcion);
    }

    private static boolean puedeConsultar(Suscripcion suscripcion) {
        return suscripcion != null
                && suscripcion.getStatus() != SuscripcionStatus.PENDING
                && suscripcion.getStatus() != SuscripcionStatus.SUSPENDED
                && suscripcion.getStatus() != SuscripcionStatus.CANCELLED
                && !suscripcion.getFechaInicio().isAfter(RelojSuscripcion.fechaActual());
    }

    private Suscripcion suscripcion(Long ownerId) {
        return suscripcionRepository.findByOwner_UsuarioId(ownerId).orElse(null);
    }

    private boolean esLectura(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod());
    }
}
