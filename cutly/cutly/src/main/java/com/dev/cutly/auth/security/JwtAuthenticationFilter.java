package com.dev.cutly.auth.security;

import com.dev.cutly.empleado.service.EmpleadoAccesoService;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import com.dev.cutly.auth.repository.TokenInvalidoRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String DIAG = "[TEMP-EMP-NOTIF-401]";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenInvalidoRepository tokenInvalidoRepository;
    private final EmpleadoAccesoService empleadoAccesoService;
    private final ControlAccesoSuscripcion controlAccesoSuscripcion;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsService userDetailsService,
                                   TokenInvalidoRepository tokenInvalidoRepository,
                                   EmpleadoAccesoService empleadoAccesoService,
                                   ControlAccesoSuscripcion controlAccesoSuscripcion) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenInvalidoRepository = tokenInvalidoRepository;
        this.empleadoAccesoService = empleadoAccesoService;
        this.controlAccesoSuscripcion = controlAccesoSuscripcion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info("{} FILTER_IN method={} uri={}", DIAG, request.getMethod(), request.getRequestURI());
        try {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || tokenInvalidoRepository.existsByToken(token)) {
            sendUnauthorized(response, token.isEmpty() ? "empty_token" : "revoked_token");
            return;
        }

        try {
            String email = jwtUtil.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()
                        || !jwtUtil.isTokenValid(token, userDetails)) {
                    sendUnauthorized(response, "user_disabled_locked_or_invalid_token");
                    return;
                }

                if (request.getRequestURI().startsWith("/api/empleado/")
                        && userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE"))
                        && !empleadoAccesoService.tienePerfilActivo(email)) {
                    sendUnauthorized(response, "employee_profile_inactive");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            var autenticacion = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacion != null && autenticacion.isAuthenticated()) {
                boolean permitido = autenticacion.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .filter(role -> role.equals("ROLE_OWNER") || role.equals("ROLE_EMPLOYEE") || role.equals("ROLE_CLIENT"))
                        .findFirst()
                        .map(role -> controlAccesoSuscripcion.tieneAcceso(autenticacion.getName(), role, request))
                        .orElse(true);
                if (!permitido) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido por el estado de la suscripción");
                    return;
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "jwt_or_user_lookup_exception:" + e.getClass().getSimpleName());
            return;
        }

        filterChain.doFilter(request, response);
        } finally {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("{} FILTER_OUT method={} uri={} status={} committed={} authenticated={} authorities={}",
                    DIAG, request.getMethod(), request.getRequestURI(), response.getStatus(), response.isCommitted(),
                    authentication != null && authentication.isAuthenticated(),
                    authentication == null ? "[]" : authentication.getAuthorities());
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String reason) throws IOException {
        log.warn("{} FILTER_401 reason={}", DIAG, reason);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT invalido, expirado o revocado");
    }
}

