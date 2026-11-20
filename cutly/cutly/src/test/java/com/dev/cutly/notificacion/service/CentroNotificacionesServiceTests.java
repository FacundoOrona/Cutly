package com.dev.cutly.notificacion.service;

import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CentroNotificacionesServiceTests {

    @Mock private NotificacionRepository notificaciones;
    @Mock private UsuarioRepository usuarios;

    @Test
    void ownerNoPuedeMarcarNotificacionDeOtroUsuario() {
        Usuario owner = new Usuario();
        owner.setUsuarioId(10L);
        owner.setRol(Rol.OWNER);
        when(usuarios.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(notificaciones.findByNotificacionIdAndUsuario_UsuarioId(42L, 10L))
                .thenReturn(Optional.empty());
        CentroNotificacionesService service = new CentroNotificacionesService(notificaciones, usuarios);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.leer("owner@example.com", 42L));

        assertEquals(404, exception.getStatusCode().value());
        verify(notificaciones).findByNotificacionIdAndUsuario_UsuarioId(42L, 10L);
    }
}
