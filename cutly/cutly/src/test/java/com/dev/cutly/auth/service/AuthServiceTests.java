package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.RegistroOwnerRequest;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.security.JwtUtil;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    @Test
    void registrarOwnerInformaCuandoElDniYaEstaRegistrado() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AuthService authService = new AuthService(
                usuarioRepository,
                mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(JwtUtil.class),
                mock(TokenInvalidoRepository.class)
        );
        RegistroOwnerRequest request = new RegistroOwnerRequest(
                "Ana", "Pérez", "ana@example.com", "44047980", "password-segura"
        );
        when(usuarioRepository.findByEmail(request.email())).thenReturn(java.util.Optional.empty());
        when(usuarioRepository.existsByDni(request.dni())).thenReturn(true);

        RuntimeException error = assertThrows(RuntimeException.class, () -> authService.registrarOwner(request));

        assertEquals("El DNI ya se encuentra registrado", error.getMessage());
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
