package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.usuario.AdminUsuarioDto;
import com.dev.cutly.admin.service.AdminUsuarioService;
import com.dev.cutly.auth.dto.AuthResponse;
import com.dev.cutly.auth.dto.RegistroOwnerRequest;
import com.dev.cutly.auth.service.AuthService;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/owners")
public class AdminOwnerController {

    private final AuthService authService;
    private final AdminUsuarioService adminUsuarioService;

    public AdminOwnerController(AuthService authService, AdminUsuarioService adminUsuarioService) {
        this.authService = authService;
        this.adminUsuarioService = adminUsuarioService;
    }

    @PostMapping
    public ResponseEntity<?> registrarOwner(@Valid @RequestBody RegistroOwnerRequest request) {
        try {
            AuthResponse response = authService.registrarOwner(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activarOwner(@PathVariable Long id) {
        return actualizarEstadoOwner(id, UsuarioStatus.ACTIVE);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarOwner(@PathVariable Long id) {
        return actualizarEstadoOwner(id, UsuarioStatus.INACTIVE);
    }

    private ResponseEntity<?> actualizarEstadoOwner(Long id, UsuarioStatus status) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String emailActor = authentication == null ? null : authentication.getName();
            AdminUsuarioDto owner = adminUsuarioService.actualizarEstadoOwner(id, status, emailActor);
            return ResponseEntity.ok(owner);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el owner con id " + id));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponseDto(e.getReason()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado del owner"));
        }
    }
}
