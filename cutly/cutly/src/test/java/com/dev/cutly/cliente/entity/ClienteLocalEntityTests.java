package com.dev.cutly.cliente.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClienteLocalEntityTests {

    @Test
    void toStringNoIncluyeDatosPersonalesNiRelaciones() {
        ClienteLocal cliente = new ClienteLocal();
        cliente.setClienteLocalId(1L);
        cliente.setDniPresentado("12.345.678");
        cliente.setDniNormalizado("12345678");
        cliente.setTelefono("+54 11 5555 0101");

        String representacion = cliente.toString();

        assertFalse(representacion.contains("12.345.678"));
        assertFalse(representacion.contains("12345678"));
        assertFalse(representacion.contains("5555 0101"));
    }

    @Test
    void igualdadDeInstanciasNoRecorreRelaciones() {
        ClienteLocal primero = new ClienteLocal();
        primero.setDniNormalizado("12345678");
        ClienteLocal segundo = new ClienteLocal();
        segundo.setDniNormalizado("12345678");

        assertNotEquals(primero, segundo);
    }
}
