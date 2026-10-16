package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.*;
import com.dev.cutly.auth.entity.TokenInvalido;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import com.dev.cutly.auth.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
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

    public AuthResponse registrarCliente(RegistroClienteRequest request) {
        if(usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("El email ya se encuentra registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setDni(request.dni());
        usuario.setRol(Rol.CLIENT);
        usuario.setStatus(UsuarioStatus.ACTIVE);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));

        Usuario clienteGuardado = usuarioRepository.save(usuario);
        String token = jwtUtil.generarToken(clienteGuardado.getEmail());

        return new AuthResponse(token, clienteGuardado.getUsuarioId(),
                clienteGuardado.getDni(), clienteGuardado.getNombre(),
                clienteGuardado.getApellido(), clienteGuardado.getEmail(),
                clienteGuardado.getStatus(), clienteGuardado.getRol(),
                jwtUtil.extractExpiration(token).toInstant()
        );
    }

    public AuthResponse registrarOwner(RegistroOwnerRequest request) {
        if(usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("El email ya se encuentra registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setDni(request.dni());
        usuario.setRol(Rol.OWNER);
        usuario.setStatus(UsuarioStatus.ACTIVE);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));

        Usuario ownerGuardado = usuarioRepository.save(usuario);
        String token = jwtUtil.generarToken(ownerGuardado.getEmail());

        return new AuthResponse(token, ownerGuardado.getUsuarioId(),
                ownerGuardado.getDni(), ownerGuardado.getNombre(),
                ownerGuardado.getApellido(), ownerGuardado.getEmail(),
                ownerGuardado.getStatus(), ownerGuardado.getRol(),
                jwtUtil.extractExpiration(token).toInstant()
        );
    }

    public AuthResponse inciarSesion (LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.contrasena(), usuario.getContrasena())) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }

        if (usuario.getStatus() != UsuarioStatus.ACTIVE) {
            throw new BadCredentialsException("Email o contraseña incorrectos");
        }

        String token = jwtUtil.generarToken(usuario.getEmail());

        return new AuthResponse(token, usuario.getUsuarioId(), usuario.getDni(),
                usuario.getNombre(), usuario.getApellido(), usuario.getEmail(),
                usuario.getStatus(), usuario.getRol(),
                jwtUtil.extractExpiration(token).toInstant());
    }

    public void cerrarSesion(String token) {
        if(!tokenInvalidoRepository.existsByToken(token)) {
            tokenInvalidoRepository.save(TokenInvalido.of(token));
        }
    }

}
