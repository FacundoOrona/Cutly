package com.dev.cutly.turno.entity;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnoIntegrityTests {

    @Test
    void aceptaTurnoOnlineConEmpleado() {
        Turno turno = new Turno();
        turno.setCliente(new Usuario());
        turno.setEmpleado(new Empleado());

        assertDoesNotThrow(turno::validarRelacionesClienteYResponsable);
    }

    @Test
    void aceptaTurnoLocalAtendidoPorOwner() {
        Turno turno = new Turno();
        turno.setClienteLocal(new ClienteLocal());
        turno.setOwnerResponsable(new Usuario());

        assertDoesNotThrow(turno::validarRelacionesClienteYResponsable);
    }

    @Test
    void rechazaClienteAusenteODuplicado() {
        Turno sinCliente = new Turno();
        sinCliente.setEmpleado(new Empleado());

        Turno dosClientes = new Turno();
        dosClientes.setCliente(new Usuario());
        dosClientes.setClienteLocal(new ClienteLocal());
        dosClientes.setEmpleado(new Empleado());

        assertThrows(IllegalStateException.class, sinCliente::validarRelacionesClienteYResponsable);
        assertThrows(IllegalStateException.class, dosClientes::validarRelacionesClienteYResponsable);
    }

    @Test
    void rechazaResponsableAusenteODuplicado() {
        Turno sinResponsable = new Turno();
        sinResponsable.setClienteLocal(new ClienteLocal());

        Turno dosResponsables = new Turno();
        dosResponsables.setClienteLocal(new ClienteLocal());
        dosResponsables.setEmpleado(new Empleado());
        dosResponsables.setOwnerResponsable(new Usuario());

        assertThrows(IllegalStateException.class, sinResponsable::validarRelacionesClienteYResponsable);
        assertThrows(IllegalStateException.class, dosResponsables::validarRelacionesClienteYResponsable);
    }
}
