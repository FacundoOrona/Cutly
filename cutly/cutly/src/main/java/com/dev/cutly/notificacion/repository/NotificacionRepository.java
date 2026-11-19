package com.dev.cutly.notificacion.repository;

import com.dev.cutly.notificacion.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuario_UsuarioIdOrderByFechaCreacionDescNotificacionIdDesc(Long usuarioId);

    Optional<Notificacion> findByNotificacionIdAndUsuario_UsuarioId(Long notificacionId, Long usuarioId);

    Page<Notificacion> findByUsuario_UsuarioId(Long usuarioId, Pageable pageable);

    Page<Notificacion> findByUsuario_UsuarioIdAndLeidaFalse(Long usuarioId, Pageable pageable);

    long countByUsuario_UsuarioIdAndLeidaFalse(Long usuarioId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("update Notificacion n set n.leida = true, n.fechaLectura = :ahora where n.usuario.usuarioId = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId,
                         @org.springframework.data.repository.query.Param("ahora") java.time.LocalDateTime ahora);

    Optional<Notificacion> findByClaveEvento(String claveEvento);
}
