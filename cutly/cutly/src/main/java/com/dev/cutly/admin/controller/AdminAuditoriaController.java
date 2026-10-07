package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.AdminAuditoriaDto;
import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.service.AdminAuditoriaService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
public class AdminAuditoriaController {

    private final AdminAuditoriaService adminAuditoriaService;

    public AdminAuditoriaController(AdminAuditoriaService adminAuditoriaService) {
        this.adminAuditoriaService = adminAuditoriaService;
    }

    @GetMapping("/api/admin/auditoria")
    public ResponseEntity<?> listarAuditorias() {
        try {
            List<AdminAuditoriaDto> auditorias = adminAuditoriaService.listarAuditorias();
            return ResponseEntity.ok(auditorias);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la lista de auditorías"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al listar las auditorías"));
        }
    }

    @GetMapping("/api/auditoria/auditoria/{id}")
    public ResponseEntity<?> obtenerAuditoria(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminAuditoriaService.obtenerAuditoria(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No se encontró la auditoría con id " + id));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar la auditoría"));
        }
    }
}
