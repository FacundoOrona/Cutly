package com.dev.cutly.empleado.dto.perfil;

import java.math.BigDecimal;

public record EmpleadoServicioDto(Long servicioId, Long negocioId, String nombre, String descripcion,
                                  BigDecimal precio, Integer duracionMinutos, boolean activo) {
}
