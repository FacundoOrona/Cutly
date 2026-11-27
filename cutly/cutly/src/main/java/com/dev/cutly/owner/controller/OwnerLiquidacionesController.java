package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.liquidacion.AnularLiquidacionRequestDto;
import com.dev.cutly.owner.dto.liquidacion.CrearLiquidacionEmpleadoRequestDto;
import com.dev.cutly.owner.service.OwnerLiquidacionesService;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}")
public class OwnerLiquidacionesController {
    private final OwnerLiquidacionesService service;
    public OwnerLiquidacionesController(OwnerLiquidacionesService service) { this.service=service; }

    @PostMapping("/liquidaciones")
    public ResponseEntity<?> crear(@PathVariable Long negocioId,
            @RequestHeader(value="Idempotency-Key", required=false) String key,
            @Valid @RequestBody CrearLiquidacionEmpleadoRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(negocioId,request,key,email()));
    }
    @GetMapping("/liquidaciones")
    public Page<?> historial(@PathVariable Long negocioId, @RequestParam(required=false) Long empleadoId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return service.historial(negocioId,empleadoId,page,size,email());
    }
    @GetMapping("/liquidaciones/{liquidacionId}")
    public Object detalleLiquidacion(@PathVariable Long negocioId,@PathVariable Long liquidacionId) {
        return service.detalleLiquidacion(negocioId,liquidacionId,email());
    }
    @PostMapping("/liquidaciones/{liquidacionId}/anulacion")
    public Object anular(@PathVariable Long negocioId,@PathVariable Long liquidacionId,
            @Valid @RequestBody AnularLiquidacionRequestDto request) {
        return service.anular(negocioId,liquidacionId,request.motivo(),email());
    }
    @GetMapping("/empleados/resumen-remuneraciones")
    public Object resumen(@PathVariable Long negocioId) { return service.resumen(negocioId,email()); }
    @GetMapping("/remuneraciones")
    public Page<?> remuneraciones(@PathVariable Long negocioId, @RequestParam(required=false) Long empleadoId,
            @RequestParam(required=false) EstadoRemuneracion estado, @RequestParam(required=false) LocalDate desde,
            @RequestParam(required=false) LocalDate hasta, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return service.remuneraciones(negocioId,empleadoId,estado,desde,hasta,page,size,email());
    }
    @GetMapping("/remuneraciones/{remuneracionId}")
    public Object detalle(@PathVariable Long negocioId,@PathVariable Long remuneracionId) {
        return service.detalleRemuneracion(negocioId,remuneracionId,email());
    }
    private String email() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        return authentication==null?null:authentication.getName();
    }
}
