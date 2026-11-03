package com.dev.cutly.owner.dto.transaccion;

import java.util.List;

public record OwnerTransaccionesPageDto(
        List<OwnerTransaccionDto> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        OwnerTransaccionesResumenDto resumen
) {}
