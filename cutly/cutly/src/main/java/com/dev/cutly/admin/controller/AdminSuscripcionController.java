package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.suscripcion.AdminSuscripcionDto;
import com.dev.cutly.admin.dto.suscripcion.AsignarSuscripcionRequestDto;
import com.dev.cutly.admin.dto.suscripcion.ObservacionCambioSuscripcionRequestDto;
import com.dev.cutly.admin.dto.suscripcion.RegularizarSuscripcionRequestDto;
import com.dev.cutly.admin.service.AdminSuscripcionService;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.LockTimeoutException;
import org.hibernate.exception.LockAcquisitionException;

import java.sql.SQLException;

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

    @PostMapping("/negocios/{negocioId}/suscripcion")
    public ResponseEntity<?> asignarSuscripcion(
            @PathVariable Long negocioId,
            @Valid @RequestBody AsignarSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        try {
            AdminSuscripcionDto asignada = adminSuscripcionService.asignar(
                    negocioId, request, emailAdministrador(), httpRequest.getRemoteAddr()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(asignada);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto(e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponseDto(e.getReason() == null ? "La operación no es válida" : e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (RuntimeException e) {
            return errorEscritura(e, "No se pudo asignar la suscripción", true);
        }
    }

    @PutMapping("/suscripciones/{id}/regularizar")
    public ResponseEntity<?> regularizarSuscripcion(
            @PathVariable Long id,
            @Valid @RequestBody RegularizarSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        try {
            AdminSuscripcionDto regularizada = adminSuscripcionService.regularizar(
                    id, request, emailAdministrador(), httpRequest.getRemoteAddr()
            );
            return ResponseEntity.ok(regularizada);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto(e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponseDto(e.getReason() == null ? "La operación no es válida" : e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (RuntimeException e) {
            return errorEscritura(e, "No se pudo regularizar la suscripción", false);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> manejarValidacion(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null
                        ? "La solicitud no es válida" : error.getDefaultMessage())
                .orElse("La solicitud no es válida");
        return ResponseEntity.badRequest().body(new ErrorResponseDto(mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> manejarJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto("El cuerpo de la solicitud no es válido"));
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
    public ResponseEntity<?> activarSuscripcion(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ObservacionCambioSuscripcionRequestDto body,
            HttpServletRequest request
    ) {
        return actualizarEstadoSuscripcion(id, SuscripcionStatus.ACTIVE, body, request);
    }

    @PatchMapping("/suscripciones/{id}/suspender")
    public ResponseEntity<?> suspenderSuscripcion(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ObservacionCambioSuscripcionRequestDto body,
            HttpServletRequest request
    ) {
        return actualizarEstadoSuscripcion(id, SuscripcionStatus.SUSPENDED, body, request);
    }

    @PatchMapping("/suscripciones/{id}/cancelar")
    public ResponseEntity<?> cancelarSuscripcion(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ObservacionCambioSuscripcionRequestDto body,
            HttpServletRequest request
    ) {
        return actualizarEstadoSuscripcion(id, SuscripcionStatus.CANCELLED, body, request);
    }

    private ResponseEntity<?> actualizarEstadoSuscripcion(
            Long id,
            SuscripcionStatus status,
            ObservacionCambioSuscripcionRequestDto body,
            HttpServletRequest request
    ) {
        try {
            AdminSuscripcionDto suscripcion = adminSuscripcionService.actualizarEstado(
                    id, status, emailAdministrador(), request.getRemoteAddr(),
                    body == null ? null : body.observacion()
            );
            return ResponseEntity.ok(suscripcion);
        } catch (NoSuchElementException | ResponseStatusException e) {
            if (e instanceof ResponseStatusException statusException) {
                return ResponseEntity.status(statusException.getStatusCode())
                        .body(new ErrorResponseDto(statusException.getReason()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la suscripción con id " + id));
        } catch (RuntimeException e) {
            return errorEscritura(e, "No se pudo actualizar el estado de la suscripción", false);
        }
    }

    private String emailAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<ErrorResponseDto> errorEscritura(
            RuntimeException exception,
            String mensaje,
            boolean traducirDuplicadoNegocio
    ) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof PessimisticLockingFailureException
                    || causa instanceof PessimisticLockException
                    || causa instanceof LockTimeoutException
                    || causa instanceof LockAcquisitionException) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDto("La suscripción o el negocio está siendo modificado; reintente"));
            }
            if (traducirDuplicadoNegocio && causa instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 1062 || "23505".equals(sqlException.getSQLState()))) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDto("El negocio ya tiene una suscripción asociada"));
            }
            causa = causa.getCause();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto(mensaje));
    }

}
