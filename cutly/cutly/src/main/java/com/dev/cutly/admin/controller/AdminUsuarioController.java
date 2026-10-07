package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.service.AdminUsuarioService;
import com.dev.cutly.usuario.dto.AdminUsuarioDto;
import com.dev.cutly.usuario.dto.ErrorResponseDto;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/admin")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) UsuarioStatus status,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String dni,
            @PageableDefault(size = 20, sort = "usuarioId", direction = DESC) Pageable pageable
    ) {
        try {
            Page<AdminUsuarioDto> usuarios = adminUsuarioService.listarUsuarios(
                    rol, status, nombre, apellido, email, dni, pageable
            );
            return ResponseEntity.ok(usuarios);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de usuarios"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar los usuarios"));
        }
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        try {
            AdminUsuarioDto usuario = adminUsuarioService.obtenerUsuario(id);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(e.getMessage()));
        } catch (java.util.NoSuchElementException | EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el usuario con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar el usuario"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar el usuario"));
        }
    }

    @PatchMapping("/usuarios/{id}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable Long id) {
        return actualizarEstadoUsuario(id, UsuarioStatus.ACTIVE);
    }

    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Long id) {
        return actualizarEstadoUsuario(id, UsuarioStatus.INACTIVE);
    }

    @PatchMapping("/usuarios/{id}/bloquear")
    public ResponseEntity<?> bloquearUsuario(@PathVariable Long id) {
        return actualizarEstadoUsuario(id, UsuarioStatus.BLOCKED);
    }

    @PatchMapping("/usuarios/{id}/desbloquear")
    public ResponseEntity<?> desbloquearUsuario(@PathVariable Long id) {
        return actualizarEstadoUsuario(id, UsuarioStatus.ACTIVE);
    }

    private ResponseEntity<?> actualizarEstadoUsuario(Long id, UsuarioStatus status) {
        try {
            AdminUsuarioDto usuario = adminUsuarioService.actualizarEstado(id, status);
            return ResponseEntity.ok(usuario);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró el usuario con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo actualizar el estado del usuario"));
        }
    }

}
