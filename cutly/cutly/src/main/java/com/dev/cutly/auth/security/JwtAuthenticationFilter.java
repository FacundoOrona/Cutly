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

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
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
        String authHeader = request.getHeader("Authorization");
        boolean bearer = authHeader != null && authHeader.startsWith("Bearer ");
        if (!bearer) {
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response);
            return;
        }
        boolean revoked = tokenInvalidoRepository.existsByToken(token);
        if (revoked) {
            sendUnauthorized(response);
            return;
        }

        try {
            String email = jwtUtil.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                boolean tokenValid = userDetails.isEnabled() && userDetails.isAccountNonLocked()
                        && jwtUtil.isTokenValid(token, userDetails);
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked() || !tokenValid) {
                    sendUnauthorized(response);
                    return;
                }

                if (request.getRequestURI().startsWith("/api/empleado/")
                        && userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE"))
                        && !empleadoAccesoService.tienePerfilActivo(email)) {
                    sendUnauthorized(response);
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
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT invalido, expirado o revocado");
    }
}

