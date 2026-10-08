package com.dev.cutly.notificacion.repository;

import com.dev.cutly.notificacion.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(Long usuarioId);

    Optional<Notificacion> findByNotificacionIdAndUsuario_UsuarioId(Long notificacionId, Long usuarioId);
}
