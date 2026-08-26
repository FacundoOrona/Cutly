package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.AuthResponse;
import com.dev.cutly.auth.dto.RegistroRequest;
import com.dev.cutly.auth.model.Usuario;
import com.dev.cutly.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }



}
