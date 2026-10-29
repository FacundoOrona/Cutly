package com.dev.cutly.owner.dto.turno;

import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrearOwnerTurnoManualRequestDto(
        @NotNull(message = "El servicio es obligatorio")
        @Positive(message = "El servicio indicado no es válido")
        Long servicioId,
        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        LocalDateTime fechaHoraInicio,
        @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
        String observaciones,
        @Positive(message = "El cliente local indicado no es válido")
        Long clienteLocalId,
        @Valid
        CrearClienteLocalRequestDto clienteNuevo,
        @Positive(message = "El empleado indicado no es válido")
        Long empleadoId
) {

    @AssertTrue(message = "Debe indicar exactamente un cliente local existente o nuevo")
    public boolean isClienteOptionValid() {
        return (clienteLocalId != null) != (clienteNuevo != null);
    }
}
