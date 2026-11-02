package com.dev.cutly.owner.dto.pago;
import com.dev.cutly.pago.enums.MetodoPago;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CrearAbonoRequestDto(@NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
                                   @NotNull MetodoPago metodoPago,
                                   @Size(max = 100) String descripcionOtro) { }
