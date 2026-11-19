package com.dev.cutly.admin.dto.suscripcion;

import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegularizarSuscripcionRequestDto(
        Long planId,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        SuscripcionStatus status,
        @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal precioContratado,
        @NotBlank String versionEsperada,
        @Size(max = 1000) String observacion
) {
}
