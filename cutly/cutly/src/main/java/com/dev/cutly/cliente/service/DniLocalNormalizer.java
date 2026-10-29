package com.dev.cutly.cliente.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class DniLocalNormalizer {

    private DniLocalNormalizer() {
    }

    public static String normalizar(String dniPresentado) {
        if (dniPresentado == null || dniPresentado.isBlank()) {
            throw dniInvalido();
        }

        StringBuilder normalizado = new StringBuilder(dniPresentado.length());
        dniPresentado.codePoints().forEach(caracter -> {
            if (caracter == '.' || caracter == '-' || Character.isWhitespace(caracter)) {
                return;
            }
            if (caracter < '0' || caracter > '9') {
                throw dniInvalido();
            }
            normalizado.append((char) caracter);
        });

        if (normalizado.length() < 7 || normalizado.length() > 8) {
            throw dniInvalido();
        }
        return normalizado.toString();
    }

    private static ResponseStatusException dniInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El DNI debe contener 7 u 8 dígitos ASCII");
    }
}
