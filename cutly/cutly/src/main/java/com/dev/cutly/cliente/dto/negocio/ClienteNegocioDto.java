package com.dev.cutly.cliente.dto.negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
public record ClienteNegocioDto(Long negocioId, String nombre, String descripcion, String telefono, String email, NegocioStatus status, String calle, String numero, String ciudad, String provincia, String codigoPostal) {}
