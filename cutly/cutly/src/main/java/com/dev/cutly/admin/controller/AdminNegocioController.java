package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.negocio.AdminNegocioDto;
import com.dev.cutly.admin.service.AdminNegocioService;
import com.dev.cutly.negocio.enums.NegocioStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminNegocioController {

    private final AdminNegocioService adminNegocioService;

    public AdminNegocioController(AdminNegocioService adminNegocioService) {
        this.adminNegocioService = adminNegocioService;
    }

    @GetMapping("/negocios")
    public ResponseEntity<?> listarNegocios() {
        try {
            List<AdminNegocioDto> negocios = adminNegocioService.listarNegocios();
            return ResponseEntity.ok(negocios);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de negocios"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar los negocios"));
        }
    }

    @GetMapping("/negocios/{id}")
    public ResponseEntity<?> obtenerNegocio(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminNegocioService.obtenerNegocio(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el negocio con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar el negocio"));
        }
    }

    @PatchMapping("/negocios/{id}/suspender")
    public ResponseEntity<?> suspenderNegocio(@PathVariable Long id) {
        return actualizarEstadoNegocio(id, NegocioStatus.SUSPENDED);
    }

    @PatchMapping("/negocios/{id}/reactivar")
    public ResponseEntity<?> reactivarNegocio(@PathVariable Long id) {
        return actualizarEstadoNegocio(id, NegocioStatus.ACTIVE);
    }

    @PatchMapping("/negocios/{id}/desactivar")
    public ResponseEntity<?> desactivarNegocio(@PathVariable Long id) {
        return actualizarEstadoNegocio(id, NegocioStatus.INACTIVE);
    }

    private ResponseEntity<?> actualizarEstadoNegocio(Long id, NegocioStatus status) {
        try {
            AdminNegocioDto negocio = adminNegocioService.actualizarEstado(id, status);
            return ResponseEntity.ok(negocio);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el negocio con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado del negocio"));
        }
    }

}
