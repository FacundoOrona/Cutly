package com.dev.cutly.cliente.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DniLocalNormalizerTests {

    @Test
    void eliminaFormatoYConservaLosCerosIniciales() {
        assertEquals("00123456", DniLocalNormalizer.normalizar(" 00.123-456\t"));
    }

    @Test
    void rechazaLongitudesQueNoCoincidenConElDniDeRegistro() {
        assertThrows(ResponseStatusException.class, () -> DniLocalNormalizer.normalizar("123456"));
        assertThrows(ResponseStatusException.class, () -> DniLocalNormalizer.normalizar("123456789"));
    }

    @Test
    void rechazaCaracteresNoAsciiNoNumericosYValoresEnBlanco() {
        assertThrows(ResponseStatusException.class, () -> DniLocalNormalizer.normalizar("１２３４５６７"));
        assertThrows(ResponseStatusException.class, () -> DniLocalNormalizer.normalizar("12.345.67A"));
        assertThrows(ResponseStatusException.class, () -> DniLocalNormalizer.normalizar("   "));
    }
}
