package com.dev.cutly.cliente.controller;

import com.dev.cutly.cliente.dto.*;
import com.dev.cutly.cliente.service.ClienteNegocioService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/negocios")
public class ClienteNegocioController {

    private final ClienteNegocioService service;

    public ClienteNegocioController(ClienteNegocioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable
    ) {
        try {
            return ResponseEntity.ok(service.listar(nombre, pageable));
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar la lista de negocios");
        }
    }

    @GetMapping("/{negocioId}")
    public ResponseEntity<?> obtener(
            @PathVariable Long negocioId
    ) {
        try {
            return ResponseEntity.ok(service.obtener(negocioId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudo consultar el negocio");
        }
    }

    @GetMapping("/{negocioId}/servicios")
    public ResponseEntity<?> servicios(
            @PathVariable Long negocioId
    ) {
        try {
            return ResponseEntity.ok(service.servicios(negocioId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar los servicios del negocio");
        }
    }

    @GetMapping("/{negocioId}/empleados")
    public ResponseEntity<?> obtenerEmpleados(
            @PathVariable Long negocioId
    ) {
        try {
            return ResponseEntity.ok(service.empleados(negocioId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar los empleados del negocio");
        }
    }

    @GetMapping("/{negocioId}/reviews")
    public ResponseEntity<?> reviews(
            @PathVariable Long negocioId
    ) {
        try {
            return ResponseEntity.ok(service.reviews(negocioId));
        } catch (ResponseStatusException e) {
            return error(e);
        } catch (DataAccessException e) {
            return errorInterno("No se pudieron consultar las reseñas del negocio");
        }
    }

    private ResponseEntity<ClienteErrorResponseDto> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new ClienteErrorResponseDto(e.getReason()));
    }

    private ResponseEntity<ClienteErrorResponseDto> errorInterno(String mensaje) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ClienteErrorResponseDto(mensaje));
    }
}
