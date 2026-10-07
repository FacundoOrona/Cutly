package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.dto.empleado.AdminEmpleadoDto;
import com.dev.cutly.admin.service.AdminEmpleadoService;
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
public class AdminEmpleadoController {

    private final AdminEmpleadoService empleadoService;

    public AdminEmpleadoController(AdminEmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping("/empleados")
    public ResponseEntity<?> listarEmpleados() {
        try {
            List<AdminEmpleadoDto> empleados = empleadoService.listarEmpleados();
            return ResponseEntity.ok(empleados);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de empleados"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar los empleados"));
        }
    }

    @GetMapping("/empleados/{id}")
    public ResponseEntity<?> obtenerEmpleado(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(empleadoService.obtenerEmpleado(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el empleado con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar el empleado"));
        }
    }

    @PatchMapping("/empleados/{id}/activar")
    public ResponseEntity<?> activarEmpleado(@PathVariable Long id) {
        return actualizarEstadoEmpleado(id, UsuarioStatus.ACTIVE);
    }

    @PatchMapping("/empleados/{id}/desactivar")
    public ResponseEntity<?> desactivarEmpleado(@PathVariable Long id) {
        return actualizarEstadoEmpleado(id, UsuarioStatus.INACTIVE);
    }

    private ResponseEntity<?> actualizarEstadoEmpleado(Long id, UsuarioStatus status) {
        try {
            AdminEmpleadoDto empleado = empleadoService.actualizarEstado(id, status);
            return ResponseEntity.ok(empleado);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el empleado con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado del empleado"));
        }
    }
}
