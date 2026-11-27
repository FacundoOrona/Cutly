package com.dev.cutly.owner.dto.liquidacion;

import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record RemuneracionPagoDto(Long remuneracionId, Long empleadoId, Long turnoId, Long servicioId,
        String clienteNombre, String servicioNombre, LocalDateTime fechaHoraInicio, EstadoRemuneracion estado, BigDecimal precioAplicado,
        BigDecimal porcentajeAplicado, BigDecimal importeConfirmado, BigDecimal totalPagado,
        BigDecimal saldoPendiente, String estadoPago, List<AplicacionLiquidacionDto> aplicaciones) {}
