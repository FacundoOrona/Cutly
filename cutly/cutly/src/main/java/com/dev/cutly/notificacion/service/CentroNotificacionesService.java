package com.dev.cutly.notificacion.service;

import com.dev.cutly.notificacion.dto.NotificacionDto;
import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
@Transactional
public class CentroNotificacionesService {
    private final NotificacionRepository repo;
    private final UsuarioRepository usuarios;
    public CentroNotificacionesService(NotificacionRepository repo, UsuarioRepository usuarios) { this.repo = repo; this.usuarios = usuarios; }

    @Transactional(readOnly = true)
    public Page<NotificacionDto> listar(String email, int page, int size, boolean noLeidas) {
        var usuario = usuarioOperativo(email);
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida");
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "notificacionId")));
        Page<Notificacion> data = noLeidas ? repo.findByUsuario_UsuarioIdAndLeidaFalse(usuario.getUsuarioId(), pageable)
                : repo.findByUsuario_UsuarioId(usuario.getUsuarioId(), pageable);
        return data.map(this::dto);
    }
    @Transactional(readOnly = true)
    public long contarNoLeidas(String email) {
        var usuario = usuarioOperativo(email);
        return repo.countByUsuario_UsuarioIdAndLeidaFalse(usuario.getUsuarioId());
    }
    public NotificacionDto leer(String email, Long id) {
        var usuario = usuarioOperativo(email);
        var n = repo.findByNotificacionIdAndUsuario_UsuarioId(id, usuario.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la notificación"));
        if (!n.isLeida()) { n.setLeida(true); n.setFechaLectura(LocalDateTime.now()); }
        return dto(n);
    }
    public int leerTodas(String email) { return repo.marcarTodasLeidas(usuarioOperativo(email).getUsuarioId(), LocalDateTime.now()); }
    private com.dev.cutly.usuario.entity.Usuario usuarioOperativo(String email) {
        var resultado = usuarios.findByEmail(email);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var u = resultado.get();
        if (u.getRol() == Rol.SUPER_ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (u.getRol() != Rol.CLIENT && u.getRol() != Rol.OWNER && u.getRol() != Rol.EMPLOYEE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return u;
    }
    private NotificacionDto dto(Notificacion n) { return new NotificacionDto(n.getNotificacionId(), n.getTipoEvento(), n.getTitulo(), n.getMensaje(), n.getNegocioId(), n.getRecursoTipo(), n.getRecursoId(), n.isLeida(), n.getFechaCreacion(), n.getFechaLectura()); }
}
