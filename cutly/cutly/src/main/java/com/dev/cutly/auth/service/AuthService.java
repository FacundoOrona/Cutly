package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.AuthResponse;
import com.dev.cutly.auth.dto.LoginRequest;
import com.dev.cutly.auth.dto.RegistroRequest;
import com.dev.cutly.auth.model.TokenInvalido;
import com.dev.cutly.auth.model.Usuario;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.auth.repository.UsuarioRepository;
import com.dev.cutly.auth.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenInvalidoRepository tokenInvalidoRepository;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, TokenInvalidoRepository tokenInvalidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenInvalidoRepository = tokenInvalidoRepository;
    }

    public AuthResponse registrarUsuario(RegistroRequest request) {
        if(usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("El email ya se encuentra registrado");
        }

//        if (!request.contrasena().equals(request.contrasenConfirmada())){
//            throw new RuntimeException("Las contraseñas introducidas no coinciden");
//        }

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

    public AuthResponse inciarSesion (LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Credenciales invalidas: Usuario no existente"));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())){
            throw new RuntimeException("Credenciales invalidas: contraseña incorrecta");
        }

        String token = jwtUtil.generarToken(usuario.getEmail());

        return new AuthResponse(token, usuario.getUsuarioId(), usuario.getNombreUsuario(),
                usuario.getNombre(), usuario.getApellido(), usuario.getEmail());
    }

    public void cerrarSesion(String token) {
        if(!tokenInvalidoRepository.existsByToken(token)) {
            tokenInvalidoRepository.save(TokenInvalido.of(token));
        }
    }

}
