package com.dev.cutly.owner.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerDeudaListadoDto(
        Long deudaId,
        Long negocioId,
        Long turnoId,
        Long clienteId,
        Long clienteLocalId,
        String nombreCliente,
        String apellidoCliente,
        BigDecimal montoOriginal,
        BigDecimal totalAbonado,
        BigDecimal saldo,
        String estado,
        LocalDateTime fechaOrigen
) { }
