package com.dev.cutly.cliente.dto.negocio;
import java.math.BigDecimal;
public record ClienteServicioDto(Long servicioId, Long negocioId, String nombre, String descripcion, BigDecimal precio, Integer duracionMinutos) {}
