package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.pago.CrearAbonoRequestDto;
import com.dev.cutly.pago.entity.*;
import com.dev.cutly.pago.enums.*;
import com.dev.cutly.pago.repository.*;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerDeudasServiceTests {
    @Mock DeudaClienteRepository deudaRepository;
    @Mock PagoRepository pagoRepository;
    @Mock OwnerNegocioService ownerNegocioService;
    OwnerDeudasService service;
    Negocio negocio;
    DeudaCliente deuda;
    @BeforeEach void setup() {
        service = new OwnerDeudasService(deudaRepository, pagoRepository, ownerNegocioService);
        Usuario owner = new Usuario(); owner.setUsuarioId(4L);
        negocio = new Negocio(); negocio.setNegocioId(8L); negocio.setOwner(owner);
        ResultadoFinancieroTurno resultado = new ResultadoFinancieroTurno(); resultado.setTipoResultado(DecisionFinanciera.CUENTA_CORRIENTE);
        Turno turno = new Turno(); turno.setTurnoId(12L); resultado.setTurno(turno);
        deuda = new DeudaCliente(); deuda.setDeudaId(10L); deuda.setNegocio(negocio); deuda.setResultadoFinanciero(resultado); deuda.setMontoOriginal(new BigDecimal("1000")); deuda.setFechaOrigen(LocalDateTime.now());
        when(ownerNegocioService.obtenerEntidadNegocioPropio(8L, "owner@test.local")).thenReturn(negocio);
        when(deudaRepository.findWithLockingByDeudaIdAndNegocio_NegocioId(10L, 8L)).thenReturn(Optional.of(deuda));
        lenient().when(pagoRepository.findByDeuda_DeudaIdOrderByFechaPagoAscPagoIdAsc(10L)).thenReturn(List.of());
    }
    @Test void registraAbonoParcialYDevuelveSaldoActual() {
        when(pagoRepository.sumarAbonos(10L)).thenReturn(new BigDecimal("200"), new BigDecimal("500"));
        var result = service.abonar(8L, 10L, new CrearAbonoRequestDto(new BigDecimal("300"), MetodoPago.TRANSFERENCIA_QR, null), "owner@test.local");
        verify(pagoRepository).save(argThat(p -> p.getMonto().compareTo(new BigDecimal("300")) == 0 && p.getDeuda() == deuda));
        assertEquals(new BigDecimal("500"), result.totalAbonado());
        assertEquals(new BigDecimal("500"), result.saldo());
        assertEquals("PARCIAL", result.estado());
    }
    @Test void permiteCancelarSaldoCompleto() {
        when(pagoRepository.sumarAbonos(10L)).thenReturn(new BigDecimal("200"), new BigDecimal("1000"));
        var result = service.abonar(8L, 10L, new CrearAbonoRequestDto(new BigDecimal("800"), MetodoPago.EFECTIVO, null), "owner@test.local");
        assertEquals(BigDecimal.ZERO, result.saldo());
        assertEquals("PAGADA", result.estado());
    }
    @Test void rechazaSobrepagoSinCrearMovimiento() {
        when(pagoRepository.sumarAbonos(10L)).thenReturn(new BigDecimal("200"));
        var ex = assertThrows(ResponseStatusException.class, () -> service.abonar(8L, 10L,
                new CrearAbonoRequestDto(new BigDecimal("801"), MetodoPago.EFECTIVO, null), "owner@test.local"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(pagoRepository, never()).save(any());
    }
}
