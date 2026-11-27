package com.dev.cutly.liquidacion.entity;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.liquidacion.enums.EstadoLiquidacion;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class LiquidacionEmpleadoTests {
    @Test
    void anularConservaDatosOriginalesYRegistraResponsable() {
        Negocio negocio = new Negocio();
        Empleado empleado = new Empleado();
        Usuario owner = new Usuario();
        LiquidacionEmpleado liquidacion = new LiquidacionEmpleado(negocio, empleado, owner, LocalDate.now(),
                MetodoPago.EFECTIVO, null, null, new BigDecimal("1250.00"), "key-1", "hash");

        liquidacion.anular("Pago cargado por error", owner);

        assertEquals(EstadoLiquidacion.ANULADA, liquidacion.getEstado());
        assertEquals("Pago cargado por error", liquidacion.getMotivoAnulacion());
        assertNotNull(liquidacion.getFechaAnulacion());
        assertSame(owner, liquidacion.getOwnerAnulador());
        assertEquals(new BigDecimal("1250.00"), liquidacion.getImporteTotal());
    }

    @Test
    void noPermiteAnularDosVeces() {
        Usuario owner = new Usuario();
        LiquidacionEmpleado liquidacion = new LiquidacionEmpleado(new Negocio(), new Empleado(), owner,
                LocalDate.now(), MetodoPago.TRANSFERENCIA_QR, null, null, BigDecimal.ONE, "key-2", "hash");
        liquidacion.anular("Motivo", owner);
        assertThrows(IllegalStateException.class, () -> liquidacion.anular("Segundo motivo", owner));
        assertEquals("Motivo", liquidacion.getMotivoAnulacion());
    }
}
