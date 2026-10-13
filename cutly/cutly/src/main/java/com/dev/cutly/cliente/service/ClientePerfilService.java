package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.perfil.ActualizarClientePerfilRequestDto;
import com.dev.cutly.cliente.dto.perfil.CambiarContrasenaRequestDto;
import com.dev.cutly.cliente.dto.perfil.ClientePerfilDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ClientePerfilService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientePerfilService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ClientePerfilDto obtener(String emailCliente) {
        return aDto(obtenerCliente(emailCliente));
    }

    public ClientePerfilDto actualizar(ActualizarClientePerfilRequestDto request, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        if (request.email() != null && !request.email().equalsIgnoreCase(cliente.getEmail())
                && usuarioRepository.existsByEmailAndUsuarioIdNot(request.email(), cliente.getUsuarioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        if (request.dni() != null && !request.dni().equals(cliente.getDni())
                && usuarioRepository.existsByDniAndUsuarioIdNot(request.dni(), cliente.getUsuarioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (request.nombre() != null) cliente.setNombre(request.nombre());
        if (request.apellido() != null) cliente.setApellido(request.apellido());
        if (request.dni() != null) cliente.setDni(request.dni());
        if (request.email() != null) cliente.setEmail(request.email());
        return aDto(usuarioRepository.save(cliente));
    }

    public void cambiarContrasena(CambiarContrasenaRequestDto request, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        if (!passwordEncoder.matches(request.contrasenaActual(), cliente.getContrasena())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.nuevaContrasena(), cliente.getContrasena())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente");
        }
        cliente.setContrasena(passwordEncoder.encode(request.nuevaContrasena()));
        usuarioRepository.save(cliente);
    }

    private Usuario obtenerCliente(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado");
        }
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado"));
        if (cliente.getRol() != Rol.CLIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un cliente");
        }
        return cliente;
    }

    private ClientePerfilDto aDto(Usuario cliente) {
        return new ClientePerfilDto(cliente.getUsuarioId(), cliente.getNombre(), cliente.getApellido(),
                cliente.getDni(), cliente.getEmail(), cliente.getRol(), cliente.getStatus());
    }
}
