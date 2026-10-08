package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.owner.service.OwnerTurnosService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDate;
import com.dev.cutly.turno.enums.TurnoStatus;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/turnos")
public class OwnerTurnosController {

    private final OwnerTurnosService ownerTurnosService;

    public OwnerTurnosController(OwnerTurnosService ownerTurnosService) {
        this.ownerTurnosService = ownerTurnosService;
    }

    @GetMapping
    public ResponseEntity<?> listarTurnos(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(required = false) Long empleadoId,
            @RequestParam(required = false) TurnoStatus status
    ) {
        try {
            List<OwnerTurnoDto> turnos = ownerTurnosService.listarTurnos(
                    negocioId, fecha, empleadoId, status, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(turnos);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de turnos");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerTurno(
            @PathVariable Long negocioId,
            @PathVariable("id") Long turnoId
    ) {
        try {
            return ResponseEntity.ok(ownerTurnosService.obtenerTurno(
                    negocioId, turnoId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el turno");
        }
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long negocioId, @PathVariable("id") Long turnoId) {
        return actualizarEstado(negocioId, turnoId, ownerTurnosService::confirmar);
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Long negocioId, @PathVariable("id") Long turnoId) {
        return actualizarEstado(negocioId, turnoId, ownerTurnosService::rechazar);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long negocioId, @PathVariable("id") Long turnoId) {
        return actualizarEstado(negocioId, turnoId, ownerTurnosService::cancelar);
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<?> completar(@PathVariable Long negocioId, @PathVariable("id") Long turnoId) {
        return actualizarEstado(negocioId, turnoId, ownerTurnosService::completar);
    }

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<?> marcarNoShow(@PathVariable Long negocioId, @PathVariable("id") Long turnoId) {
        return actualizarEstado(negocioId, turnoId, ownerTurnosService::marcarNoShow);
    }

    private ResponseEntity<?> actualizarEstado(
            Long negocioId,
            Long turnoId,
            ActualizadorTurno actualizador
    ) {
        try {
            return ResponseEntity.ok(actualizador.actualizar(
                    negocioId, turnoId, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo actualizar el estado del turno");
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

    @FunctionalInterface
    private interface ActualizadorTurno {
        OwnerTurnoDto actualizar(Long negocioId, Long turnoId, String emailOwner);
    }
}
