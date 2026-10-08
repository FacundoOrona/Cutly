package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.notificacion.OwnerNotificacionDto;
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
public class OwnerNotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public OwnerNotificacionService(
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<OwnerNotificacionDto> listarNotificaciones(String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return notificacionRepository
                .findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(owner.getUsuarioId())
                .stream().map(this::aDto).toList();
    }

    public OwnerNotificacionDto marcarComoLeida(Long notificacionId, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Notificacion notificacion = notificacionRepository
                .findByNotificacionIdAndUsuario_UsuarioId(notificacionId, owner.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró la notificación solicitada"));
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion = notificacionRepository.save(notificacion);
        }
        return aDto(notificacion);
    }

    public int marcarTodasComoLeidas(String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        List<Notificacion> noLeidas = notificacionRepository
                .findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(owner.getUsuarioId())
                .stream().filter(notificacion -> !notificacion.isLeida()).toList();
        noLeidas.forEach(notificacion -> notificacion.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
        return noLeidas.size();
    }

    private Usuario obtenerOwnerAutenticado(String emailOwner) {
        if (emailOwner == null || emailOwner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al owner autenticado");
        }
        Usuario owner = usuarioRepository.findByEmail(emailOwner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al owner autenticado"));
        if (owner.getRol() != Rol.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta autenticada no es un owner");
        }
        return owner;
    }

    private OwnerNotificacionDto aDto(Notificacion notificacion) {
        return new OwnerNotificacionDto(
                notificacion.getNotificacionId(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion()
        );
    }
}
