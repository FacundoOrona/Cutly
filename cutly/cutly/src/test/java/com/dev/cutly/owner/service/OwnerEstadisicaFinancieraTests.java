package com.dev.cutly.owner.service;

import com.dev.cutly.pago.repository.DeudaClienteRepository;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import com.dev.cutly.turno.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerEstadisicaFinancieraTests {
    @Mock TurnoRepository turnoRepository;
    @Mock PagoRepository pagoRepository;
    @Mock OwnerNegocioService ownerNegocioService;
    @Mock DeudaClienteRepository deudaRepository;
    @Mock ResultadoFinancieroTurnoRepository resultadoRepository;

    @Test void separaDineroRecibidoYNoCuentaLaDeudaComoIngreso() {
        OwnerEstadisicaService service = new OwnerEstadisicaService(turnoRepository, pagoRepository, ownerNegocioService, deudaRepository, resultadoRepository);
        when(pagoRepository.sumarCobrosDirectos(5L, null, null)).thenReturn(new BigDecimal("400"));
        when(pagoRepository.sumarAbonosPorNegocioYPeriodo(5L, null, null)).thenReturn(new BigDecimal("100"));
        when(pagoRepository.contarCobrosDirectos(5L, null, null)).thenReturn(1L);
        when(pagoRepository.contarAbonosPorNegocioYPeriodo(5L, null, null)).thenReturn(2L);
        when(resultadoRepository.contarTurnosCobrados(5L, null, null)).thenReturn(1L);
        when(resultadoRepository.contarTurnosGratuitos(5L, null, null)).thenReturn(1L);
        when(deudaRepository.sumarSaldoPendiente(5L, null, null)).thenReturn(new BigDecimal("600"));
        when(deudaRepository.contarPendientes(5L, null, null)).thenReturn(1L);
        when(deudaRepository.contarParciales(5L, null, null)).thenReturn(1L);
        when(deudaRepository.contarPagadas(5L, null, null)).thenReturn(1L);
        when(turnoRepository.contarCompletadosSinRegularizacion(5L, null, null)).thenReturn(1L);

        var result = service.obtenerEstadisticasIngresos(5L, null, null, "owner@test.local");
        assertEquals(new BigDecimal("500"), result.ingresosTotales());
        assertEquals(new BigDecimal("400"), result.ingresosCobrosDirectos());
        assertEquals(new BigDecimal("100"), result.ingresosAbonos());
        assertEquals(3L, result.cantidadPagos());
        assertEquals(new BigDecimal("600"), result.deudaPendienteTotal());
        assertEquals(1L, result.deudasPendientes());
        assertEquals(1L, result.deudasParciales());
        assertEquals(1L, result.deudasPagadas());
        assertEquals(1L, result.turnosPendientesRegularizacion());
    }
}
