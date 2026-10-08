package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.notificacion.OwnerNotificacionDto;
import com.dev.cutly.owner.dto.notificacion.OwnerNotificacionesLeidasDto;
import com.dev.cutly.owner.service.OwnerNotificacionService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/notificaciones")
public class OwnerNotificacionController {

    private final OwnerNotificacionService ownerNotificacionService;

    public OwnerNotificacionController(OwnerNotificacionService ownerNotificacionService) {
        this.ownerNotificacionService = ownerNotificacionService;
    }

    @GetMapping
    public ResponseEntity<?> listarNotificaciones() {
        try {
            List<OwnerNotificacionDto> notificaciones = ownerNotificacionService
                    .listarNotificaciones(emailOwnerAutenticado());
            return ResponseEntity.ok(notificaciones);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de notificaciones");
        }
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable("id") Long notificacionId) {
        try {
            return ResponseEntity.ok(ownerNotificacionService.marcarComoLeida(
                    notificacionId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo marcar la notificación como leída");
        }
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas() {
        try {
            int actualizadas = ownerNotificacionService.marcarTodasComoLeidas(emailOwnerAutenticado());
            return ResponseEntity.ok(new OwnerNotificacionesLeidasDto(actualizadas));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron marcar las notificaciones como leídas");
        }
    }

    private String emailOwnerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<OwnerErrorResponseDto> respuestaError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new OwnerErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<OwnerErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OwnerErrorResponseDto(mensaje));
    }
}
