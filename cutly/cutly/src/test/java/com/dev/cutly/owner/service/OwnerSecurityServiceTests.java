package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.CambiarContrasenaOwnerRequestDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OwnerSecurityServiceTests {
    @Test
    void cambiaContrasenaDelOwnerAutenticadoUsandoElEncoder() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario owner = new Usuario();
        owner.setRol(Rol.OWNER);
        owner.setContrasena("hash-anterior");
        when(repository.findByEmail("owner@cutly.test")).thenReturn(Optional.of(owner));
        when(encoder.matches("actual123", "hash-anterior")).thenReturn(true);
        when(encoder.matches("nueva123", "hash-anterior")).thenReturn(false);
        when(encoder.encode("nueva123")).thenReturn("hash-nuevo");

        new OwnerSecurityService(repository, encoder).cambiarContrasena(
                new CambiarContrasenaOwnerRequestDto("actual123", "nueva123"), "owner@cutly.test");

        assertEquals("hash-nuevo", owner.getContrasena());
        verify(repository).save(owner);
    }

    @Test
    void rechazaUnaContrasenaActualIncorrecta() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Usuario owner = new Usuario();
        owner.setRol(Rol.OWNER);
        owner.setContrasena("hash-anterior");
        when(repository.findByEmail("owner@cutly.test")).thenReturn(Optional.of(owner));
        when(encoder.matches("incorrecta", "hash-anterior")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> new OwnerSecurityService(repository, encoder)
                .cambiarContrasena(new CambiarContrasenaOwnerRequestDto("incorrecta", "nueva123"), "owner@cutly.test"));
        verify(repository, never()).save(owner);
    }
}
