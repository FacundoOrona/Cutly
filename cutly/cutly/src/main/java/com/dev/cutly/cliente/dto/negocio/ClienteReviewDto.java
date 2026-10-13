package com.dev.cutly.cliente.dto.negocio;
public record ClienteReviewDto(Long reviewId, Integer puntuacion, String comentario, Long clienteId, String nombreCliente, Long negocioId) {}
