package com.dev.cutly.owner.dto.liquidacion;

import com.dev.cutly.liquidacion.enums.EstadoLiquidacion;
import com.dev.cutly.pago.enums.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LiquidacionEmpleadoDto(Long liquidacionId, Long empleadoId, String empleadoNombre,
        LocalDate fechaEfectiva, LocalDateTime fechaRegistro, MetodoPago metodoPago, String descripcionMetodo,
        String observaciones, BigDecimal importeTotal, EstadoLiquidacion estado, String motivoAnulacion,
        LocalDateTime fechaAnulacion, String ownerRegistrante, String ownerAnulador,
        List<AplicacionLiquidacionDto> aplicaciones) {}
