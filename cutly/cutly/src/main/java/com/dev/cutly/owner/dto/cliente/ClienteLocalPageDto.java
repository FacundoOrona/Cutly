package com.dev.cutly.owner.dto.cliente;

import java.util.List;

public record ClienteLocalPageDto(
        List<ClienteLocalDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
