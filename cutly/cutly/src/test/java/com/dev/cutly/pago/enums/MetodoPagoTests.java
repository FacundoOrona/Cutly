package com.dev.cutly.pago.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetodoPagoTests {
    @Test void conservaMetodosHistoricosYAgregaLosNuevos() {
        for (String nombre : new String[]{"CASH", "DEBIT_CARD", "CREDIT_CARD", "TRANSFER", "MERCADO_PAGO",
                "EFECTIVO", "TRANSFERENCIA_QR", "DEBITO", "CREDITO", "OTRO"}) {
            assertTrue(java.util.Arrays.stream(MetodoPago.values()).anyMatch(m -> m.name().equals(nombre)), nombre);
        }
    }
}
