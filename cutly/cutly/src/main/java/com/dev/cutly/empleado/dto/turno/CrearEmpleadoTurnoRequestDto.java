package com.dev.cutly.empleado.dto.turno;

import com.dev.cutly.empleado.dto.cliente.CrearEmpleadoClienteLocalRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CrearEmpleadoTurnoRequestDto(
        @NotNull(message = "El servicio es obligatorio") @Positive(message = "El servicio indicado no es válido")
        Long servicioId,
        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        LocalDateTime fechaHoraInicio,
        @Positive(message = "El cliente local indicado no es válido")
        Long clienteLocalId,
        @Valid CrearEmpleadoClienteLocalRequestDto clienteNuevo
) {
    @AssertTrue(message = "Debe indicar exactamente un cliente local existente o nuevo")
    public boolean isClienteOptionValid() {
        return (clienteLocalId != null) != (clienteNuevo != null);
    }
}
