package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.plan.ActualizarPlanSuscripcionRequestDto;
import com.dev.cutly.admin.dto.plan.AdminPlanSuscripcionDto;
import com.dev.cutly.admin.dto.plan.CambiarEstadoPlanSuscripcionRequestDto;
import com.dev.cutly.admin.dto.plan.CrearPlanSuscripcionRequestDto;
import com.dev.cutly.admin.service.AdminPlanSuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/admin/planes-suscripcion")
public class AdminPlanSuscripcionController {

    private final AdminPlanSuscripcionService adminPlanService;

    public AdminPlanSuscripcionController(AdminPlanSuscripcionService adminPlanService) {
        this.adminPlanService = adminPlanService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return manejar(() -> ResponseEntity.ok(adminPlanService.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return manejar(() -> ResponseEntity.ok(adminPlanService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @Valid @RequestBody CrearPlanSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        return manejar(() -> {
            AdminPlanSuscripcionDto creado = adminPlanService.crear(
                    request, emailActor(), httpRequest.getRemoteAddr()
            );
            return ResponseEntity.created(URI.create("/api/admin/planes-suscripcion/" + creado.planId()))
                    .body(creado);
        });
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPlanSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        return manejar(() -> ResponseEntity.ok(adminPlanService.editar(
                id, request, emailActor(), httpRequest.getRemoteAddr()
        )));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoPlanSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        return manejar(() -> ResponseEntity.ok(adminPlanService.activar(
                id, request.versionEsperada(), emailActor(), httpRequest.getRemoteAddr()
        )));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoPlanSuscripcionRequestDto request,
            HttpServletRequest httpRequest
    ) {
        return manejar(() -> ResponseEntity.ok(adminPlanService.desactivar(
                id, request.versionEsperada(), emailActor(), httpRequest.getRemoteAddr()
        )));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> manejarValidacion(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "La solicitud no es válida" : error.getDefaultMessage())
                .orElse("La solicitud no es válida");
        return ResponseEntity.badRequest().body(new ErrorResponseDto(mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> manejarJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponseDto("El cuerpo de la solicitud no es válido"));
    }

    private ResponseEntity<?> manejar(Supplier<ResponseEntity<?>> operacion) {
        try {
            return operacion.get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto(e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponseDto(e.getReason() == null ? "La operación no es válida" : e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (RuntimeException e) {
            if (esConflictoDeVersion(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDto("El plan fue modificado por otro administrador; vuelva a cargarlo"));
            }
            if (esNombreNormalizadoDuplicado(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ErrorResponseDto("Ya existe un plan con ese nombre normalizado"));
            }
            if (e instanceof DataAccessException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponseDto("No se pudo completar la operación de planes"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al administrar planes"));
        }
    }

    private boolean esConflictoDeVersion(Throwable exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof OptimisticLockingFailureException
                    || causa instanceof OptimisticLockException
                    || causa instanceof StaleStateException) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private boolean esNombreNormalizadoDuplicado(Throwable exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if (constraint != null && constraint.toLowerCase(java.util.Locale.ROOT)
                        .contains("uq_planes_suscripcion_nombre_normalizado")) {
                    return true;
                }
            }
            if (causa instanceof SQLException sqlException) {
                boolean esDuplicado = sqlException.getErrorCode() == 1062
                        || "23505".equals(sqlException.getSQLState());
                String mensaje = sqlException.getMessage();
                if (esDuplicado && mensaje != null
                        && mensaje.toLowerCase(java.util.Locale.ROOT)
                        .contains("uq_planes_suscripcion_nombre_normalizado")) {
                    return true;
                }
            }
            causa = causa.getCause();
        }
        return false;
    }

    private String emailActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
