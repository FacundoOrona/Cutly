package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.suscripcion.AdminSuscripcionDto;
import com.dev.cutly.admin.service.AdminSuscripcionService;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminSuscripcionController {

    private final AdminSuscripcionService adminSuscripcionService;

    public AdminSuscripcionController(AdminSuscripcionService adminSuscripcionService) {
        this.adminSuscripcionService = adminSuscripcionService;
    }

    @GetMapping("/suscripciones")
    public ResponseEntity<?> listarSuscripciones() {
        try {
            List<AdminSuscripcionDto> suscripciones = adminSuscripcionService.listarSuscripciones();
            return ResponseEntity.ok(suscripciones);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de suscripciones"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar las suscripciones"));
        }
    }

    @GetMapping("/suscripciones/{id}")
    public ResponseEntity<?> obtenerSuscripcion(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminSuscripcionService.obtenerSuscripcion(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la suscripción con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la suscripción"));
        }
    }

    @GetMapping("/suscripciones/vencidas")
    public ResponseEntity<?> listarVencidas() {
        try {
            List<AdminSuscripcionDto> suscripciones = adminSuscripcionService.listarVencidas();
            return ResponseEntity.ok(suscripciones);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar las suscripciones vencidas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar las suscripciones vencidas"));
        }
    }

    @GetMapping("/suscripciones/proximas-a-vencer")
    public ResponseEntity<?> listarProximasAVencer() {
        try {
            List<AdminSuscripcionDto> suscripciones = adminSuscripcionService.listarProximasAVencer();
            return ResponseEntity.ok(suscripciones);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar las próximas suscripciones a vencer"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar las próximas suscripciones a vencer"));
        }
    }

    @PatchMapping("/suscripciones/{id}/activar")
    public ResponseEntity<?> activarSuscripcion(@PathVariable Long id) {
        return actualizarEstadoSuscripcion(id, SuscripcionStatus.ACTIVE);
    }

    @PatchMapping("/suscripciones/{id}/suspender")
    public ResponseEntity<?> suspenderSuscripcion(@PathVariable Long id) {
        return actualizarEstadoSuscripcion(id, SuscripcionStatus.SUSPENDED);
    }

    private ResponseEntity<?> actualizarEstadoSuscripcion(Long id, SuscripcionStatus status) {
        try {
            AdminSuscripcionDto suscripcion = adminSuscripcionService.actualizarEstado(id, status);
            return ResponseEntity.ok(suscripcion);
        } catch (NoSuchElementException | ResponseStatusException e) {
            if (e instanceof ResponseStatusException statusException) {
                return ResponseEntity.status(statusException.getStatusCode())
                        .body(new ErrorResponseDto(statusException.getReason()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la suscripción con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado de la suscripción"));
        }
    }

}
