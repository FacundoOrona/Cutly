package com.dev.cutly.empleado.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoTurnoServiceTests {
    private static final String EMAIL = "empleado@test.local";
    private static final Long USUARIO_ID = 5L;
    private static final Long EMPLEADO_ID = 9L;

    @Mock private TurnoRepository turnoRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private EmpleadoTurnoService service;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        service = new EmpleadoTurnoService(turnoRepository, empleadoRepository, usuarioRepository);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(USUARIO_ID);
        usuario.setNombre("Empleada");
        usuario.setApellido("Test");
        usuario.setRol(Rol.EMPLOYEE);
        empleado = new Empleado();
        empleado.setEmpleadoId(EMPLEADO_ID);
        empleado.setUsuario(usuario);
        empleado.setActivo(true);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findByUsuario_UsuarioId(USUARIO_ID)).thenReturn(Optional.of(empleado));
    }

    @Test
    void mapeaClienteLocalSinCambiarElResponsableEmpleado() {
        Negocio negocio = new Negocio();
        negocio.setNegocioId(2L);
        negocio.setNombre("Negocio");
        empleado.setNegocio(negocio);
        ClienteLocal clienteLocal = new ClienteLocal();
        clienteLocal.setClienteLocalId(31L);
        clienteLocal.setNombre("Cliente");
        clienteLocal.setApellido("Local");
        Servicio servicio = new Servicio();
        servicio.setServicioId(7L);
        servicio.setNombre("Corte");
        Turno turno = new Turno();
        turno.setTurnoId(40L);
        turno.setNegocio(negocio);
        turno.setClienteLocal(clienteLocal);
        turno.setEmpleado(empleado);
        turno.setServicio(servicio);
        when(turnoRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Turno>>any(), any(Sort.class)
        )).thenReturn(List.of(turno));

        EmpleadoTurnoDto dto = service.listar(EMAIL, null, null, false).get(0);

        assertNull(dto.clienteId());
        assertEquals(31L, dto.clienteLocalId());
        assertEquals(TipoClienteTurno.LOCAL, dto.tipoCliente());
        assertEquals("Cliente", dto.clienteNombre());
        assertEquals("Local", dto.clienteApellido());
        assertEquals(EMPLEADO_ID, dto.empleadoId());
    }
}
