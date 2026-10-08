package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerEstadisicaService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/estadisticas")
public class OwnerEstadisticaController {

    private final OwnerEstadisicaService ownerEstadisicaService;

    public OwnerEstadisticaController(OwnerEstadisicaService ownerEstadisicaService) {
        this.ownerEstadisicaService = ownerEstadisicaService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(ownerEstadisicaService.obtenerResumen(
                    negocioId, desde, hasta, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el resumen estadístico");
        }
    }

    @GetMapping("/turnos")
    public ResponseEntity<?> obtenerEstadisticasTurnos(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(ownerEstadisicaService.obtenerEstadisticasTurnos(
                    negocioId, desde, hasta, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar las estadísticas de turnos");
        }
    }

    @GetMapping("/ingresos")
    public ResponseEntity<?> obtenerEstadisticasIngresos(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(ownerEstadisicaService.obtenerEstadisticasIngresos(
                    negocioId, desde, hasta, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar las estadísticas de ingresos");
        }
    }

    @GetMapping("/empleados")
    public ResponseEntity<?> obtenerEstadisticasEmpleados(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(ownerEstadisicaService.obtenerEstadisticasEmpleados(
                    negocioId, desde, hasta, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar las estadísticas de empleados");
        }
    }

    @GetMapping("/servicios")
    public ResponseEntity<?> obtenerEstadisticasServicios(
            @PathVariable Long negocioId,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        try {
            return ResponseEntity.ok(ownerEstadisicaService.obtenerEstadisticasServicios(
                    negocioId, desde, hasta, emailOwnerAutenticado()
            ));
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar las estadísticas de servicios");
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
