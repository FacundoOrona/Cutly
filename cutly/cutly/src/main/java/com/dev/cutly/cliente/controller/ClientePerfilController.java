package com.dev.cutly.cliente.controller;

import com.dev.cutly.cliente.dto.ClienteErrorResponseDto;
import com.dev.cutly.cliente.dto.perfil.ActualizarClientePerfilRequestDto;
import com.dev.cutly.cliente.dto.perfil.CambiarContrasenaRequestDto;
import com.dev.cutly.cliente.service.ClientePerfilService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/perfil")
public class ClientePerfilController {
    private final ClientePerfilService service;

    public ClientePerfilController(ClientePerfilService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> obtenerPerfil() {
        try {
            return ResponseEntity.ok(service.obtener(emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el perfil");
        }
    }

    @PatchMapping
    public ResponseEntity<?> actualizarPerfil(@Valid @RequestBody ActualizarClientePerfilRequestDto request) {
        try {
            return ResponseEntity.ok(service.actualizar(request, emailClienteAutenticado()));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo actualizar el perfil");
        }
    }

    @PatchMapping("/cambiar-contrasena")
    public ResponseEntity<?> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequestDto request) {
        try {
            service.cambiarContrasena(request, emailClienteAutenticado());
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo cambiar la contraseña");
        }
    }

    private String emailClienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<ClienteErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ClienteErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<ClienteErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ClienteErrorResponseDto(mensaje));
    }
}
