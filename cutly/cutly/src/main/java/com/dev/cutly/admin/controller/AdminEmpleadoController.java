package com.dev.cutly.admin.controller;

public class AdminEmpleadoController {

    /*
    El SUPER_ADMIN no debería necesitar administrar cada operación cotidiana de los empleados; eso corresponde al OWNER.

    Pero sí debería poder consultar y actuar administrativamente.

    GET /api/admin/empleados
    GET /api/admin/empleados/{id}

    Con filtros:

    GET /api/admin/empleados?negocioId=10
    GET /api/admin/empleados?estado=ACTIVE

    Y eventualmente:

    PATCH /api/admin/empleados/{id}/bloquear
    PATCH /api/admin/empleados/{id}/desbloquear
     */

}
