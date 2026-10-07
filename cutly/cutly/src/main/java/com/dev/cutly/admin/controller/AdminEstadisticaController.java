package com.dev.cutly.admin.controller;

import com.dev.cutly.admin.dto.AdminEstadisticasNegociosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasIngresosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasSuscripcionesDto;
import com.dev.cutly.admin.dto.AdminEstadisticasTurnosDto;
import com.dev.cutly.admin.dto.AdminEstadisticasUsuariosDto;
import com.dev.cutly.admin.dto.AdminResumenEstadisticasDto;
import com.dev.cutly.admin.dto.ErrorResponseDto;
import com.dev.cutly.admin.service.AdminEstadisticaService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminEstadisticaController {

    private final AdminEstadisticaService adminEstadisticaService;

    public AdminEstadisticaController(AdminEstadisticaService adminEstadisticaService) {
        this.adminEstadisticaService = adminEstadisticaService;
    }

    // 7/10 : totales de usuarios, empleados, negocios y suscripciones.
    @GetMapping("/estadisticas/resumen")
    public ResponseEntity<?> obtenerResumen() {
        try {
            AdminResumenEstadisticasDto resumen = adminEstadisticaService.obtenerResumen();
            return ResponseEntity.ok(resumen);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudo consultar el resumen estadístico"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar el resumen"));
        }
    }

    // 7/10: usuarios por estado y por rol.
    @GetMapping("/estadisticas/usuarios")
    public ResponseEntity<?> obtenerEstadisticasUsuarios() {
        try {
            AdminEstadisticasUsuariosDto estadisticas = adminEstadisticaService.obtenerEstadisticasUsuarios();
            return ResponseEntity.ok(estadisticas);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudieron consultar las estadísticas de usuarios"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar las estadísticas de usuarios"));
        }
    }

    // 7/10: negocios por estado
    @GetMapping("/estadisticas/negocios")
    public ResponseEntity<?> obtenerEstadisticasNegocios() {
        try {
            AdminEstadisticasNegociosDto estadisticas = adminEstadisticaService.obtenerEstadisticasNegocios();
            return ResponseEntity.ok(estadisticas);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudieron consultar las estadísticas de negocios"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar las estadísticas de negocios"));
        }
    }

    // 7/10: : total de turnos y conteos por estado.
    @GetMapping("/estadisticas/turnos")
    public ResponseEntity<?> obtenerEstadisticasTurnos() {
        try {
            AdminEstadisticasTurnosDto estadisticas = adminEstadisticaService.obtenerEstadisticasTurnos();
            return ResponseEntity.ok(estadisticas);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudieron consultar las estadísticas de turnos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar las estadísticas de turnos"));
        }
    }

    // 7/10: suma de los pagos registrados y cantidad de pagos
    @GetMapping("/estadisticas/ingresos")
    public ResponseEntity<?> obtenerEstadisticasIngresos() {
        try {
            AdminEstadisticasIngresosDto estadisticas = adminEstadisticaService.obtenerEstadisticasIngresos();
            return ResponseEntity.ok(estadisticas);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudieron consultar las estadísticas de ingresos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar las estadísticas de ingresos"));
        }
    }

    // 7/10: total y conteos por estado.
    @GetMapping("/estadisticas/suscripciones")
    public ResponseEntity<?> obtenerEstadisticasSuscripciones() {
        try {
            AdminEstadisticasSuscripcionesDto estadisticas = adminEstadisticaService.obtenerEstadisticasSuscripciones();
            return ResponseEntity.ok(estadisticas);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("No se pudieron consultar las estadísticas de suscripciones"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("Ocurrió un error inesperado al consultar las estadísticas de suscripciones"));
        }
    }
}
