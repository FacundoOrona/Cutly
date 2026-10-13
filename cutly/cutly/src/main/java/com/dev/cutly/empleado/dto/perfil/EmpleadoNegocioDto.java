package com.dev.cutly.empleado.dto.perfil;

public record EmpleadoNegocioDto(Long negocioId, String nombre, String descripcion, String telefono,
                                 String email, String calle, String numero, String ciudad,
                                 String provincia, String codigoPostal) {
}
