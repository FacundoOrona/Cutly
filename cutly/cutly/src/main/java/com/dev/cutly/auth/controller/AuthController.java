package com.dev.cutly.auth.controller;

import com.dev.cutly.auth.dto.*;
import com.dev.cutly.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController (AuthService authService) {
        this.authService = authService;
    }

    //Registro de clientes - usuarios
    @PostMapping("/registro-cliente")
    public ResponseEntity<?> registroCliente (@Valid @RequestBody RegistroClienteRequest request) {
        try {
            AuthResponse response = authService.registrarCliente(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/iniciarSesion")
    public ResponseEntity<?> inciarSesion (@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.inciarSesion(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o contraseña incorrectos");
        }
    }

    @PostMapping("/cerrarSesion")
    public ResponseEntity<?> cerrarSesion (@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            authService.cerrarSesion(token);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
