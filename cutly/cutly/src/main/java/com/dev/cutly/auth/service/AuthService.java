package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.AuthResponse;
import com.dev.cutly.auth.dto.RegistroRequest;
import com.dev.cutly.auth.model.Usuario;
import com.dev.cutly.auth.repository.UsuarioRepository;
import com.dev.cutly.auth.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse registrarUsuario(RegistroRequest request) {
        if(usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("El email ya se encuentra registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(request.nombreUsuario());
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        String token = jwtUtil.generarToken(usuarioGuardado.getEmail());

        return new AuthResponse(token, usuarioGuardado.getUsuarioId(),
                usuarioGuardado.getNombreUsuario(), usuarioGuardado.getNombre(),
                usuarioGuardado.getApellido(), usuarioGuardado.getEmail()
        );
    }

}
