package com.dev.cutly.empleado.controller;

import com.dev.cutly.empleado.dto.cliente.EmpleadoClienteLocalDto;
import com.dev.cutly.empleado.service.EmpleadoClienteLocalService;
import com.dev.cutly.empleado.dto.perfil.EmpleadoErrorResponseDto;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/empleado/clientes-locales")
public class EmpleadoClienteLocalController {
    private final EmpleadoClienteLocalService service;

    public EmpleadoClienteLocalController(EmpleadoClienteLocalService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> buscar(@RequestParam("q") String texto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication == null ? null : authentication.getName();
            List<EmpleadoClienteLocalDto> resultado = service.buscar(email, texto);
            return ResponseEntity.ok(resultado);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new EmpleadoErrorResponseDto(e.getReason()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EmpleadoErrorResponseDto("No se pudo consultar los clientes locales"));
        }
    }
}
