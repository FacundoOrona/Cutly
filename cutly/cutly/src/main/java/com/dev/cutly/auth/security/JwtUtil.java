package com.dev.cutly.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {

    private static final long EXPIRACION = 1000 * 60 * 60 * 10;
    private final Key singingKey;

    public JwtUtil(@Value("${security.jwt.secret}") String secretKey) {
        this.singingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
