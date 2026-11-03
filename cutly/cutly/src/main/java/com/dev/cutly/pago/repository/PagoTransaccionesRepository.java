package com.dev.cutly.pago.repository;

import com.dev.cutly.pago.entity.Pago;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.enums.TipoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

public interface PagoTransaccionesRepository {
    Page<Pago> buscarTransacciones(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q, Pageable pageable);

    long contarTransaccionesFiltradas(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q);

    BigDecimal sumarTransaccionesFiltradas(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q);

    BigDecimal sumarCobrosDirectosFiltrados(Long negocioId, LocalDateTime desde, LocalDateTime hasta,
            Collection<MetodoPago> metodos, TipoTransaccion tipo, Long clienteId,
            Long clienteLocalId, String q);
}
