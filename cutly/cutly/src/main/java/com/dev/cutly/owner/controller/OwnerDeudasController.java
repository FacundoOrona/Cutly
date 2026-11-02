package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.pago.CrearAbonoRequestDto;
import com.dev.cutly.owner.service.OwnerDeudasService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/deudas")
public class OwnerDeudasController {
    private final OwnerDeudasService service;
    public OwnerDeudasController(OwnerDeudasService service) { this.service = service; }
    @GetMapping public ResponseEntity<?> listar(@PathVariable Long negocioId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { try { return ResponseEntity.ok(service.listar(negocioId, q, estado, page, size, email())); } catch (ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(e.getReason()); } }
    @GetMapping("/{deudaId}") public ResponseEntity<?> obtener(@PathVariable Long negocioId, @PathVariable Long deudaId) { try { return ResponseEntity.ok(service.obtener(negocioId, deudaId, email())); } catch (ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(e.getReason()); } }
    @PostMapping("/{deudaId}/abonos") public ResponseEntity<?> abonar(@PathVariable Long negocioId, @PathVariable Long deudaId, @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CrearAbonoRequestDto request) { try { return ResponseEntity.ok(service.abonar(negocioId, deudaId, request, key, email())); } catch (ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(e.getReason()); } }
    private String email() { Authentication a = SecurityContextHolder.getContext().getAuthentication(); return a == null ? null : a.getName(); }
}
