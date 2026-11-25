package com.dev.cutly.notificacion.controller;

import com.dev.cutly.notificacion.service.CentroNotificacionesService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping({"/api/cliente/notificaciones", "/api/owner/notificaciones", "/api/empleado/notificaciones"})
public class CentroNotificacionesController {
    private static final Logger log = LoggerFactory.getLogger(CentroNotificacionesController.class);
    private final CentroNotificacionesService service;
    public CentroNotificacionesController(CentroNotificacionesService service) { this.service = service; }

    @GetMapping public ResponseEntity<?> listar(Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean noLeidas) {
        return ResponseEntity.ok(service.listar(auth.getName(), page, size, noLeidas));
    }
    @GetMapping("/no-leidas/contador") public ResponseEntity<?> contar(Authentication auth) {
        String email = auth == null ? null : auth.getName();
        log.info("AUTH_TRACE endpointInvoked=CentroNotificacionesController.contar identityPresent={} identity={} authenticated={} authorities={}", email != null, maskEmail(email), auth != null && auth.isAuthenticated(), auth == null ? "[]" : auth.getAuthorities());
        return ResponseEntity.ok(java.util.Map.of("cantidad", service.contarNoLeidas(auth.getName())));
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "<absent>";
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
    @PatchMapping("/{id}/leer") public ResponseEntity<?> leer(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(service.leer(auth.getName(), id));
    }
    @PatchMapping("/leer-todas") public ResponseEntity<?> leerTodas(Authentication auth) {
        return ResponseEntity.ok(java.util.Map.of("actualizadas", service.leerTodas(auth.getName())));
    }
}
