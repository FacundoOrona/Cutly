package com.dev.cutly.owner.dto.pago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record OwnerDeudaDto(Long deudaId, Long negocioId, Long turnoId, Long clienteId, Long clienteLocalId,
                            BigDecimal montoOriginal, BigDecimal totalAbonado, BigDecimal saldo,
                            String estado, LocalDateTime fechaOrigen, List<OwnerPagoDto> abonos,
                            BigDecimal montoAcordado) { }
