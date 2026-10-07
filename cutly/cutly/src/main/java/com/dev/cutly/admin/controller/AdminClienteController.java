package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.cliente.AdminClienteDto;
import com.dev.cutly.admin.service.AdminClienteService;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminClienteController {

    private final AdminClienteService adminClienteService;

    public AdminClienteController(AdminClienteService adminClienteService) {
        this.adminClienteService = adminClienteService;
    }

    @GetMapping("/clientes")
    public ResponseEntity<?> listarClientes() {
        try {
            List<AdminClienteDto> clientes = adminClienteService.listarClientes();
            return ResponseEntity.ok(clientes);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de clientes"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar los clientes"));
        }
    }

    @GetMapping("/clientes/{id}")
    public ResponseEntity<?> obtenerCliente(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminClienteService.obtenerCliente(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el cliente con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar el cliente"));
        }
    }

    @PatchMapping("/cliente/{id}/activar")
    public ResponseEntity<?> activarCliente(@PathVariable Long id) {
        return actualizarEstadoCliente(id, UsuarioStatus.ACTIVE);
    }

    @PatchMapping("/cliente/{id}/desactivar")
    public ResponseEntity<?> desactivarCliente(@PathVariable Long id) {
        return actualizarEstadoCliente(id, UsuarioStatus.INACTIVE);
    }

    @PatchMapping("/cliente/{id}/bloquear")
    public ResponseEntity<?> bloquearCliente(@PathVariable Long id) {
        return actualizarEstadoCliente(id, UsuarioStatus.BLOCKED);
    }

    private ResponseEntity<?> actualizarEstadoCliente(Long id, UsuarioStatus status) {
        try {
            AdminClienteDto cliente = adminClienteService.actualizarEstado(id, status);
            return ResponseEntity.ok(cliente);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el cliente con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado del cliente"));
        }
    }

}
