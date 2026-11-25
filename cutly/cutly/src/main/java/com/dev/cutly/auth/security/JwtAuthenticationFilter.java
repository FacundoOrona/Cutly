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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
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
        log.info("AUTH_TRACE request method={} path={} authorizationPresent={} bearerFormat={}", request.getMethod(), request.getRequestURI(), authHeader != null, bearer);
        if (!bearer) {
            log.info("AUTH_TRACE authenticationDeferred method={} path={} reason=no_bearer_header", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.warn("AUTH_TRACE rejected component=JwtAuthenticationFilter reason=empty_bearer_token method={} path={} status=401", request.getMethod(), request.getRequestURI());
            sendUnauthorized(response);
            return;
        }
        boolean revoked = tokenInvalidoRepository.existsByToken(token);
        log.info("AUTH_TRACE tokenRevoked={} method={} path={}", revoked, request.getMethod(), request.getRequestURI());
        if (revoked) {
            log.warn("AUTH_TRACE rejected component=JwtAuthenticationFilter reason=revoked_token method={} path={} status=401", request.getMethod(), request.getRequestURI());
            sendUnauthorized(response);
            return;
        }

        try {
            String email = jwtUtil.extractEmail(token);
            log.info("AUTH_TRACE jwtEmailExtracted={} email={} method={} path={}", email != null && !email.isBlank(), maskEmail(email), request.getMethod(), request.getRequestURI());

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                log.info("AUTH_TRACE userFound=true email={} enabled={} accountNonLocked={} role={} method={} path={}", maskEmail(email), userDetails.isEnabled(), userDetails.isAccountNonLocked(), userDetails.getAuthorities(), request.getMethod(), request.getRequestURI());

                boolean tokenValid = userDetails.isEnabled() && userDetails.isAccountNonLocked()
                        && jwtUtil.isTokenValid(token, userDetails);
                log.info("AUTH_TRACE jwtValid={} email={} method={} path={}", tokenValid, maskEmail(email), request.getMethod(), request.getRequestURI());
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked() || !tokenValid) {
                    log.warn("AUTH_TRACE rejected component=JwtAuthenticationFilter reason=user_or_token_invalid email={} enabled={} accountNonLocked={} jwtValid={} status=401", maskEmail(email), userDetails.isEnabled(), userDetails.isAccountNonLocked(), tokenValid);
                    sendUnauthorized(response);
                    return;
                }

                if (request.getRequestURI().startsWith("/api/empleado/")
                        && userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE"))
                        && !empleadoAccesoService.tienePerfilActivo(email)) {
                    log.warn("AUTH_TRACE rejected component=JwtAuthenticationFilter reason=employee_profile_inactive email={} status=401", maskEmail(email));
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
                    log.warn("AUTH_TRACE rejected component=ControlAccesoSuscripcion reason=access_denied method={} path={} status=403", request.getMethod(), request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso restringido por el estado de la suscripción");
                    return;
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.warn("AUTH_TRACE rejected component=JwtAuthenticationFilter reason={} exceptionType={} method={} path={} status=401", e instanceof UsernameNotFoundException ? "user_not_found" : "jwt_or_argument_error", e.getClass().getSimpleName(), request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
        log.info("AUTH_TRACE downstreamCompleted method={} path={} finalStatus={}", request.getMethod(), request.getRequestURI(), response.getStatus());
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "<absent>";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        log.warn("AUTH_TRACE finalStatusProducer=JwtAuthenticationFilter status=401");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token JWT invalido, expirado o revocado");
    }
}

