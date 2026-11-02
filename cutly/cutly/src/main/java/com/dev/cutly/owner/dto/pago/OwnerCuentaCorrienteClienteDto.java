package com.dev.cutly.owner.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OwnerCuentaCorrienteClienteDto(
        Long clienteId,
        Long clienteLocalId,
        String tipoCliente,
        String nombreCliente,
        String apellidoCliente,
        long cantidadDeudasPendientes,
        BigDecimal saldoPendienteTotal,
        LocalDateTime fechaDeudaMasAntigua
) { }
