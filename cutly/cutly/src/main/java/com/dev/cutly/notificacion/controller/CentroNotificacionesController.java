package com.dev.cutly.notificacion.controller;

import com.dev.cutly.notificacion.service.CentroNotificacionesService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/cliente/notificaciones", "/api/owner/notificaciones", "/api/empleado/notificaciones"})
public class CentroNotificacionesController {
    private final CentroNotificacionesService service;
    public CentroNotificacionesController(CentroNotificacionesService service) { this.service = service; }

    @GetMapping public ResponseEntity<?> listar(Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean noLeidas) {
        return ResponseEntity.ok(service.listar(auth.getName(), page, size, noLeidas));
    }
    @GetMapping("/no-leidas/contador") public ResponseEntity<?> contar(Authentication auth) {
        return ResponseEntity.ok(java.util.Map.of("cantidad", service.contarNoLeidas(auth.getName())));
    }
    @PatchMapping("/{id}/leer") public ResponseEntity<?> leer(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(service.leer(auth.getName(), id));
    }
    @PatchMapping("/leer-todas") public ResponseEntity<?> leerTodas(Authentication auth) {
        return ResponseEntity.ok(java.util.Map.of("actualizadas", service.leerTodas(auth.getName())));
    }
}
