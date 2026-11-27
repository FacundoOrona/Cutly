package com.dev.cutly.owner.dto.empleado;

import java.math.BigDecimal;
import java.util.List;

public record OwnerRemuneracionEmpleadoDto(Long empleadoId, BigDecimal porcentajeGeneral,
                                            List<ServicioComisionDto> servicios) {
    public record ServicioComisionDto(Long servicioId, String nombre, BigDecimal porcentajeExcepcion,
                                      BigDecimal porcentajeAplicado, boolean usaPorcentajeGeneral) { }
}
