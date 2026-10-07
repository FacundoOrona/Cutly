package com.dev.cutly.admin.dto.estadistica;

public record AdminEstadisticasUsuariosDto(
        long totalUsuarios,
        long usuariosActivos,
        long usuariosInactivos,
        long usuariosBloqueados,
        long clientes,
        long owners,
        long empleados,
        long administradores
) {
}
