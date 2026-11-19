package com.dev.cutly.suscripcion.service;

import com.dev.cutly.empleado.repository.EmpleadoRepository;
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

@Service
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
        if ("/api/owner/negocios".equals(path) && HttpMethod.POST.matches(request.getMethod())) {
            return ownerPuedeCrearNegocio(email);
        }
        if ("/api/owner/negocios".equals(path) && HttpMethod.GET.matches(request.getMethod())) {
            return ownerPuedeConsultarTodos(email);
        }
        if ("ROLE_OWNER".equals(rol) && path.startsWith("/api/owner/negocios/")) {
            String[] partes = path.substring("/api/owner/negocios/".length()).split("/");
            if (partes.length > 0 && partes[0].matches("\\d+")) {
                long negocioId = Long.parseLong(partes[0]);
                if (esRenovacion(path)) return true;
                Suscripcion suscripcion = suscripcion(negocioId);
                return suscripcion != null && ownerTieneAcceso(suscripcion, esLectura(request));
            }
            return ownerPuedeConsultarTodos(email) && esLectura(request);
        }
        if ("ROLE_OWNER".equals(rol) && path.startsWith("/api/owner/")) {
            return ownerPuedeConsultarTodos(email) && esLectura(request);
        }
        if ("ROLE_EMPLOYEE".equals(rol) && path.startsWith("/api/empleado/")) {
            return empleadoRepository.findByUsuario_Email(email)
                    .map(e -> suscripcion(e.getNegocio().getNegocioId()))
                    .map(this::estadoVigente)
                    .orElse(false);
        }
        if ("ROLE_CLIENT".equals(rol) && path.matches("/api/cliente/negocios/\\d+(/.*)?")) {
            String[] partes = path.split("/");
            Suscripcion suscripcion = suscripcion(Long.parseLong(partes[4]));
            return suscripcion != null && !estadoBloqueado(suscripcion);
        }
        return true;
    }

    public boolean clientePuedeReservar(Long negocioId) {
        Suscripcion suscripcion = suscripcion(negocioId);
        return suscripcion != null && estadoVigente(suscripcion);
    }

    public boolean clientePuedeConsultarNegocio(Long negocioId) {
        Suscripcion suscripcion = suscripcion(negocioId);
        if (suscripcion == null || estadoBloqueado(suscripcion)) return false;
        if (suscripcion.getStatus() == SuscripcionStatus.EXPIRED
                || suscripcion.getStatus() == SuscripcionStatus.ACTIVE) return true;
        return suscripcion.getStatus() == SuscripcionStatus.TRIAL
                && !suscripcion.getFechaVencimiento().isBefore(RelojSuscripcion.fechaActual());
    }

    public static boolean suscripcionHabilitada(Suscripcion suscripcion) {
        return suscripcion != null
                && (suscripcion.getStatus() == SuscripcionStatus.ACTIVE
                || suscripcion.getStatus() == SuscripcionStatus.TRIAL)
                && !suscripcion.getFechaInicio().isAfter(RelojSuscripcion.fechaActual())
                && !suscripcion.getFechaVencimiento().isBefore(RelojSuscripcion.fechaActual());
    }

    private boolean ownerPuedeCrearNegocio(String email) {
        return usuarioRepository.findByEmail(email)
                .map(owner -> owner.getRol() == Rol.OWNER)
                .orElse(false);
    }

    private boolean ownerPuedeConsultarTodos(String email) {
        return usuarioRepository.findByEmail(email)
                .map(owner -> negocioRepository.findByOwner_UsuarioId(owner.getUsuarioId()).stream()
                        .map(n -> suscripcion(n.getNegocioId()))
                        .anyMatch(s -> s != null && !estadoBloqueado(s)))
                .orElse(false);
    }

    private boolean ownerTieneAcceso(Suscripcion suscripcion, boolean lectura) {
        if (estadoBloqueado(suscripcion)) return false;
        return lectura || estadoVigente(suscripcion);
    }

    private boolean estadoVigente(Suscripcion suscripcion) {
        if (estadoBloqueado(suscripcion)) return false;
        return (suscripcion.getStatus() == SuscripcionStatus.TRIAL
                || suscripcion.getStatus() == SuscripcionStatus.ACTIVE)
                && !suscripcion.getFechaVencimiento().isBefore(RelojSuscripcion.fechaActual());
    }

    private boolean estadoBloqueado(Suscripcion suscripcion) {
        return suscripcion.getStatus() == SuscripcionStatus.SUSPENDED
                || suscripcion.getStatus() == SuscripcionStatus.CANCELLED;
    }

    private Suscripcion suscripcion(Long negocioId) {
        return suscripcionRepository.findByNegocio_NegocioId(negocioId).orElse(null);
    }

    private boolean esLectura(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod());
    }

    private boolean esRenovacion(String path) {
        return path.matches("/api/owner/negocios/\\d+/suscripcion/renovar");
    }
}
