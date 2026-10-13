package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.dto.notificacion.EmpleadoNotificacionDto;
import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class EmpleadoNotificacionService {
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpleadoNotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<EmpleadoNotificacionDto> listar(String emailEmpleado) {
        Usuario empleado = obtenerEmpleado(emailEmpleado);
        return notificacionRepository.findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(empleado.getUsuarioId())
                .stream().map(this::aDto).toList();
    }

    public EmpleadoNotificacionDto marcarComoLeida(Long notificacionId, String emailEmpleado) {
        Usuario empleado = obtenerEmpleado(emailEmpleado);
        Notificacion notificacion = notificacionRepository.findByNotificacionIdAndUsuario_UsuarioId(notificacionId, empleado.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la notificación solicitada"));
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion = notificacionRepository.save(notificacion);
        }
        return aDto(notificacion);
    }

    public int marcarTodasComoLeidas(String emailEmpleado) {
        Usuario empleado = obtenerEmpleado(emailEmpleado);
        List<Notificacion> noLeidas = notificacionRepository
                .findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(empleado.getUsuarioId())
                .stream().filter(notificacion -> !notificacion.isLeida()).toList();
        noLeidas.forEach(notificacion -> notificacion.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
        return noLeidas.size();
    }

    private Usuario obtenerEmpleado(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado");
        }
        Usuario empleado = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado"));
        if (empleado.getRol() != Rol.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un empleado");
        }
        return empleado;
    }

    private EmpleadoNotificacionDto aDto(Notificacion notificacion) {
        return new EmpleadoNotificacionDto(notificacion.getNotificacionId(), notificacion.getTitulo(),
                notificacion.getMensaje(), notificacion.isLeida(), notificacion.getFechaCreacion());
    }
}
