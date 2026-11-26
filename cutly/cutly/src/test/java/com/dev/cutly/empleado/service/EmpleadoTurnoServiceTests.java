package com.dev.cutly.empleado.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.cliente.service.ClienteLocalService;
import com.dev.cutly.empleado.dto.turno.CrearEmpleadoTurnoRequestDto;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EmpleadoTurnoServiceTests {
    private static final String EMAIL = "empleado@test.local";
    private static final Long USUARIO_ID = 5L;
    private static final Long EMPLEADO_ID = 9L;

    @Mock private TurnoRepository turnoRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionTurnoService notificacionTurnoService;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpleadoServiciorRepository empleadoServicioRepository;
    @Mock private ClienteLocalRepository clienteLocalRepository;
    @Mock private ClienteLocalService clienteLocalService;
    @Mock private TurnoHorarioValidator horarioValidator;

    private EmpleadoTurnoService service;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        service = new EmpleadoTurnoService(turnoRepository, empleadoRepository, usuarioRepository, notificacionTurnoService,
                servicioRepository, empleadoServicioRepository, clienteLocalRepository, clienteLocalService, horarioValidator);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(USUARIO_ID);
        usuario.setNombre("Empleada");
        usuario.setApellido("Test");
        usuario.setRol(Rol.EMPLOYEE);
        empleado = new Empleado();
        empleado.setEmpleadoId(EMPLEADO_ID);
        empleado.setUsuario(usuario);
        empleado.setActivo(true);
        Negocio negocio = new Negocio();
        negocio.setNegocioId(2L);
        negocio.setNombre("Negocio");
        negocio.setStatus(NegocioStatus.ACTIVE);
        Usuario owner = new Usuario(); owner.setUsuarioId(3L); owner.setNombre("Owner"); owner.setApellido("Test");
        negocio.setOwner(owner);
        empleado.setNegocio(negocio);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(empleadoRepository.findByUsuario_UsuarioId(USUARIO_ID)).thenReturn(Optional.of(empleado));
    }

    @Test
    void creaTurnoConfirmadoParaEmpleadoAutenticadoConClienteExistenteYNotificaSoloAlOwner() {
        Servicio servicio = servicioValido();
        ClienteLocal cliente = new ClienteLocal(); cliente.setClienteLocalId(31L);
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicio));
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, 2L)).thenReturn(Optional.of(cliente));
        when(empleadoRepository.findWithLockingByEmpleadoId(EMPLEADO_ID)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(EMPLEADO_ID, 7L))
                .thenReturn(Optional.of(new com.dev.cutly.servicio.entity.EmpleadoServicio()));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(org.mockito.ArgumentMatchers.eq(EMPLEADO_ID), any(), any()))
                .thenReturn(List.of());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(call -> {
            Turno turno = call.getArgument(0); turno.setTurnoId(90L); return turno;
        });
        var request = new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(3).withHour(10), 31L, null);

        EmpleadoTurnoDto result = service.crear(request, EMAIL);

        assertEquals(TurnoStatus.CONFIRMED, result.status());
        assertEquals(EMPLEADO_ID, result.empleadoId());
        assertEquals(31L, result.clienteLocalId());
        verify(empleadoRepository).findWithLockingByEmpleadoId(EMPLEADO_ID);
        verify(notificacionTurnoService).notificarTurnoCreadoPorEmpleado(any(Turno.class));
        verify(notificacionTurnoService, never()).notificarNuevoTurnoAsignado(any());
    }

    @Test
    void creaClienteLocalYTurnoEnElMismoFlujo() {
        Servicio servicio = servicioValido();
        ClienteLocal cliente = new ClienteLocal(); cliente.setClienteLocalId(32L); cliente.setNombre("Ana"); cliente.setApellido("Test");
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicio));
        when(empleadoRepository.findWithLockingByEmpleadoId(EMPLEADO_ID)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(EMPLEADO_ID, 7L))
                .thenReturn(Optional.of(new com.dev.cutly.servicio.entity.EmpleadoServicio()));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(org.mockito.ArgumentMatchers.eq(EMPLEADO_ID), any(), any()))
                .thenReturn(List.of());
        when(clienteLocalService.registrar(empleado.getNegocio(), "Ana", "Test", "123456", "555"))
                .thenReturn(cliente);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(call -> call.getArgument(0));
        var request = new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(3).withHour(10), null,
                new com.dev.cutly.empleado.dto.cliente.CrearEmpleadoClienteLocalRequestDto("Ana", "Test", "123456", "555"));

        service.crear(request, EMAIL);

        verify(clienteLocalService).registrar(empleado.getNegocio(), "Ana", "Test", "123456", "555");
        verify(turnoRepository).save(argThat(turno -> turno.getClienteLocal() == cliente
                && turno.getStatus() == TurnoStatus.CONFIRMED && turno.getEmpleado() == empleado));
    }

    @Test
    void rechazaClienteDeOtroNegocio() {
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicioValido()));
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(99L, 2L)).thenReturn(Optional.empty());
        var request = new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(2), 99L, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(request, EMAIL));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(empleadoRepository, never()).findWithLockingByEmpleadoId(any());
    }

    @Test
    void rechazaEmpleadoInactivo() {
        empleado.setActivo(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(
                new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(2), 31L, null), EMAIL));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(servicioRepository, never()).findByServicioIdAndNegocio_NegocioId(any(), any());
    }

    @Test
    void rechazaServicioSinAsignarAlEmpleado() {
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicioValido()));
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, 2L))
                .thenReturn(Optional.of(new ClienteLocal()));
        when(empleadoRepository.findWithLockingByEmpleadoId(EMPLEADO_ID)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(EMPLEADO_ID, 7L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(
                new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(2), 31L, null), EMAIL));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void noCreaTurnoSiElHorarioEsInvalidoOEstaOcupado() {
        Servicio servicio = servicioValido();
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicio));
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, 2L))
                .thenReturn(Optional.of(new ClienteLocal()));
        when(empleadoRepository.findWithLockingByEmpleadoId(EMPLEADO_ID)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(EMPLEADO_ID, 7L))
                .thenReturn(Optional.of(new com.dev.cutly.servicio.entity.EmpleadoServicio()));
        var inicio = java.time.LocalDateTime.now().plusDays(2);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Horario inválido"))
                .when(horarioValidator).validar(2L, inicio, inicio.plusMinutes(30));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(
                new CrearEmpleadoTurnoRequestDto(7L, inicio, 31L, null), EMAIL));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(turnoRepository, never()).findTurnosSolapadosPorEmpleado(any(), any(), any());
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    @Test
    void rechazaSolapamientoAntesDeCrearClienteNuevo() {
        Servicio servicio = servicioValido();
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(7L, 2L)).thenReturn(Optional.of(servicio));
        when(empleadoRepository.findWithLockingByEmpleadoId(EMPLEADO_ID)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(EMPLEADO_ID, 7L))
                .thenReturn(Optional.of(new com.dev.cutly.servicio.entity.EmpleadoServicio()));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(org.mockito.ArgumentMatchers.eq(EMPLEADO_ID), any(), any()))
                .thenReturn(List.of(new Turno()));
        var request = new CrearEmpleadoTurnoRequestDto(7L, java.time.LocalDateTime.now().plusDays(2), null,
                new com.dev.cutly.empleado.dto.cliente.CrearEmpleadoClienteLocalRequestDto("Ana", "Test", "123456", "555"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(request, EMAIL));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(clienteLocalService, never()).registrar(any(), any(), any(), any(), any());
        verify(turnoRepository, never()).save(any(Turno.class));
    }

    private Servicio servicioValido() {
        Servicio servicio = new Servicio(); servicio.setServicioId(7L); servicio.setNombre("Corte");
        servicio.setActivo(true); servicio.setDuracionMinutos(30); servicio.setPrecio(new BigDecimal("1250.00"));
        servicio.setNegocio(empleado.getNegocio()); return servicio;
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

    @Test
    void completarComoEmpleadoSoloCambiaElEstadoYConservaElPrecio() {
        Negocio negocio = new Negocio(); negocio.setNegocioId(2L); negocio.setNombre("Negocio"); empleado.setNegocio(negocio);
        ClienteLocal cliente = new ClienteLocal(); cliente.setClienteLocalId(31L); cliente.setNombre("Cliente"); cliente.setApellido("Local");
        Servicio servicio = new Servicio(); servicio.setServicioId(7L); servicio.setNombre("Corte");
        Turno turno = new Turno(); turno.setTurnoId(21L); turno.setNegocio(negocio); turno.setClienteLocal(cliente); turno.setEmpleado(empleado);
        turno.setServicio(servicio); turno.setStatus(TurnoStatus.CONFIRMED); turno.setPrecio(new BigDecimal("1250.00"));
        when(turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(turno)).thenReturn(turno);

        service.completar(21L, EMAIL);

        assertEquals(TurnoStatus.COMPLETED, turno.getStatus());
        assertEquals(new BigDecimal("1250.00"), turno.getPrecio());
        verify(turnoRepository).save(turno);
        verify(notificacionTurnoService).notificarCambioEstado(
                turno, TurnoStatus.CONFIRMED, TurnoStatus.COMPLETED, empleado.getUsuario());
    }

    @Test
    void iniciarTurnoPropioConfirmadoLoPasaAEnCursoYNotifica() {
        Turno turno = turno(TurnoStatus.CONFIRMED);
        when(turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID)).thenReturn(Optional.of(turno));
        when(turnoRepository.save(turno)).thenReturn(turno);

        EmpleadoTurnoDto result = service.iniciar(21L, EMAIL);

        assertEquals(TurnoStatus.IN_PROGRESS, turno.getStatus());
        assertEquals(TurnoStatus.IN_PROGRESS, result.status());
        verify(turnoRepository).findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID);
        verify(turnoRepository).save(turno);
        verify(notificacionTurnoService).notificarCambioEstado(turno, TurnoStatus.CONFIRMED, TurnoStatus.IN_PROGRESS, empleado.getUsuario());
    }

    @Test
    void iniciarTurnoYaEnCursoEsIdempotenteYNoNotificaDeNuevo() {
        Turno turno = turno(TurnoStatus.IN_PROGRESS);
        when(turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID)).thenReturn(Optional.of(turno));

        EmpleadoTurnoDto result = service.iniciar(21L, EMAIL);

        assertEquals(TurnoStatus.IN_PROGRESS, result.status());
        verify(turnoRepository, never()).save(any(Turno.class));
        verify(notificacionTurnoService, never()).notificarCambioEstado(any(), any(), any(), any());
    }

    @Test
    void iniciarTurnoPendienteDevuelveConflicto() {
        Turno turno = turno(TurnoStatus.PENDING);
        when(turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID)).thenReturn(Optional.of(turno));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.iniciar(21L, EMAIL));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(turnoRepository, never()).save(any(Turno.class));
        verify(notificacionTurnoService, never()).notificarCambioEstado(any(), any(), any(), any());
    }

    @Test
    void iniciarTurnoDeOtroEmpleadoNoLoEncuentra() {
        when(turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(21L, EMPLEADO_ID)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.iniciar(21L, EMAIL));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(turnoRepository, never()).save(any(Turno.class));
        verify(notificacionTurnoService, never()).notificarCambioEstado(any(), any(), any(), any());
    }

    private Turno turno(TurnoStatus status) {
        Negocio negocio = new Negocio();
        negocio.setNegocioId(2L);
        negocio.setNombre("Negocio");
        empleado.setNegocio(negocio);
        ClienteLocal cliente = new ClienteLocal();
        cliente.setClienteLocalId(31L);
        cliente.setNombre("Cliente");
        cliente.setApellido("Local");
        Servicio servicio = new Servicio();
        servicio.setServicioId(7L);
        servicio.setNombre("Corte");
        Turno turno = new Turno();
        turno.setTurnoId(21L);
        turno.setNegocio(negocio);
        turno.setClienteLocal(cliente);
        turno.setEmpleado(empleado);
        turno.setServicio(servicio);
        turno.setStatus(status);
        return turno;
    }
}
