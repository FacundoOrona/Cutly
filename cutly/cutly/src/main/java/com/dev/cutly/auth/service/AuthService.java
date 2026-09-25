package com.dev.cutly.auth.service;

import com.dev.cutly.auth.dto.AuthResponse;
import com.dev.cutly.auth.dto.LoginRequest;
import com.dev.cutly.auth.dto.RegistroClienteRequest;
import com.dev.cutly.auth.dto.RegistroOwnerRequest;
import com.dev.cutly.auth.entity.TokenInvalido;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
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
                clienteGuardado.getStatus(), clienteGuardado.getRol()
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
        // El rol de la cuenta se define en el servidor; nunca se acepta desde el DTO.
        usuario.setRol(Rol.OWNER);
        usuario.setStatus(UsuarioStatus.ACTIVE);
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));

        Usuario ownerGuardado = usuarioRepository.save(usuario);
        String token = jwtUtil.generarToken(ownerGuardado.getEmail());

        return new AuthResponse(token, ownerGuardado.getUsuarioId(),
                ownerGuardado.getDni(), ownerGuardado.getNombre(),
                ownerGuardado.getApellido(), ownerGuardado.getEmail(),
                ownerGuardado.getStatus(), ownerGuardado.getRol()
        );
    }

    public AuthResponse inciarSesion (LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Credenciales invalidas: Usuario no existente"));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())){
            throw new RuntimeException("Credenciales invalidas: contraseña incorrecta");
        }

        if (usuario.getStatus() != UsuarioStatus.ACTIVE) {
            throw new RuntimeException("La cuenta no está activa");
        }

        String token = jwtUtil.generarToken(usuario.getEmail());

        return new AuthResponse(token, usuario.getUsuarioId(), usuario.getDni(),
                usuario.getNombre(), usuario.getApellido(), usuario.getEmail(),
                usuario.getStatus(), usuario.getRol());
    }

    public void cerrarSesion(String token) {
        if(!tokenInvalidoRepository.existsByToken(token)) {
            tokenInvalidoRepository.save(TokenInvalido.of(token));
        }
    }

}
