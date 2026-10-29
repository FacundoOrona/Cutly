package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.owner.dto.cliente.ClienteLocalDto;
import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import com.dev.cutly.owner.dto.turno.CrearOwnerTurnoManualRequestDto;
import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.enums.TipoResponsableTurno;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class OwnerTurnosServiceTests {
    private static final Long NEGOCIO_ID = 11L;
    private static final String EMAIL_OWNER = "owner@test.local";

    @Mock private TurnoRepository turnoRepository;
    @Mock private OwnerNegocioService ownerNegocioService;
    @Mock private ClienteLocalRepository clienteLocalRepository;
    @Mock private OwnerClienteLocalService ownerClienteLocalService;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private EmpleadoServiciorRepository empleadoServicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TurnoHorarioValidator horarioValidator;

    private OwnerTurnosService service;
    private Negocio negocio;

    @BeforeEach
    void setUp() {
        service = new OwnerTurnosService(turnoRepository, ownerNegocioService, clienteLocalRepository,
                ownerClienteLocalService, servicioRepository, empleadoRepository,
                empleadoServicioRepository, usuarioRepository, horarioValidator);
        negocio = new Negocio();
        negocio.setNegocioId(NEGOCIO_ID);
        negocio.setNombre("Negocio test");
        negocio.setStatus(NegocioStatus.ACTIVE);
        Usuario owner = usuario(3L, "Olga", "Owner");
        owner.setRol(Rol.OWNER);
        negocio.setOwner(owner);
        when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, EMAIL_OWNER)).thenReturn(negocio);
    }

    @Test
    void mapeaUsuarioClienteYEmpleadoSinAlterarCamposExistentes() {
        Usuario cliente = usuario(1L, "Cliente", "Online");
        Usuario empleadoUsuario = usuario(2L, "Empleada", "Test");
        Empleado empleado = new Empleado();
        empleado.setEmpleadoId(22L);
        empleado.setUsuario(empleadoUsuario);

        OwnerTurnoDto dto = mapear(turno(cliente, null, empleado, null));

        assertEquals(1L, dto.clienteId());
        assertEquals("Cliente", dto.clienteNombre());
        assertEquals(22L, dto.empleadoId());
        assertEquals("Empleada", dto.empleadoNombre());
        assertNull(dto.clienteLocalId());
        assertEquals(TipoClienteTurno.USUARIO, dto.tipoCliente());
        assertNull(dto.ownerResponsableId());
        assertEquals(TipoResponsableTurno.EMPLEADO, dto.tipoResponsable());
        assertEquals("Empleada", dto.responsableNombre());
    }

    @Test
    void mapeaClienteLocalConEmpleado() {
        ClienteLocal clienteLocal = clienteLocal(31L, "Cliente", "Local");
        Empleado empleado = empleado(22L, "Empleada", "Test");

        OwnerTurnoDto dto = mapear(turno(null, clienteLocal, empleado, null));

        assertNull(dto.clienteId());
        assertEquals(31L, dto.clienteLocalId());
        assertEquals(TipoClienteTurno.LOCAL, dto.tipoCliente());
        assertEquals("Cliente", dto.clienteNombre());
        assertEquals(22L, dto.empleadoId());
        assertEquals(TipoResponsableTurno.EMPLEADO, dto.tipoResponsable());
    }

    @Test
    void mapeaUsuarioClienteConOwnerResponsable() {
        Usuario cliente = usuario(1L, "Cliente", "Online");
        Usuario owner = usuario(3L, "Olga", "Owner");

        OwnerTurnoDto dto = mapear(turno(cliente, null, null, owner));

        assertEquals(1L, dto.clienteId());
        assertEquals(TipoClienteTurno.USUARIO, dto.tipoCliente());
        assertNull(dto.empleadoId());
        assertEquals(3L, dto.ownerResponsableId());
        assertEquals(TipoResponsableTurno.OWNER, dto.tipoResponsable());
        assertEquals("Olga", dto.responsableNombre());
        assertNull(dto.empleadoNombre());
    }

    @Test
    void mapeaClienteLocalYOwnerResponsable() {
        ClienteLocal clienteLocal = clienteLocal(31L, "Cliente", "Local");
        Usuario owner = usuario(3L, "Olga", "Owner");

        OwnerTurnoDto dto = mapear(turno(null, clienteLocal, null, owner));

        assertNull(dto.clienteId());
        assertEquals(31L, dto.clienteLocalId());
        assertEquals(TipoClienteTurno.LOCAL, dto.tipoCliente());
        assertNull(dto.empleadoId());
        assertEquals(3L, dto.ownerResponsableId());
        assertEquals(TipoResponsableTurno.OWNER, dto.tipoResponsable());
        assertEquals("Olga", dto.responsableNombre());
    }

    @Test
    void creaTurnoConfirmadoConClienteExistenteYEmpleadoResponsable() {
        configurarServicioYEmpleado();
        ClienteLocal local = clienteLocal(31L, "Cliente", "Local");
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, NEGOCIO_ID))
                .thenReturn(Optional.of(local));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno turno = invocation.getArgument(0);
            turno.setTurnoId(81L);
            return turno;
        });

        OwnerTurnoDto result = service.crearManual(NEGOCIO_ID,
                request(31L, null, 22L), EMAIL_OWNER);

        assertEquals(TurnoStatus.CONFIRMED, result.status());
        assertEquals(TipoClienteTurno.LOCAL, result.tipoCliente());
        assertEquals(TipoResponsableTurno.EMPLEADO, result.tipoResponsable());
        assertEquals(22L, result.empleadoId());
        verify(turnoRepository).save(argThat(turno -> turno.getClienteLocal() == local
                && turno.getEmpleado() != null && turno.getOwnerResponsable() == null
                && turno.getStatus() == TurnoStatus.CONFIRMED));
        verifyNoInteractions(ownerClienteLocalService);
    }

    @Test
    void creaClienteNuevoYTurnoConfirmadoConEmpleado() {
        configurarServicioYEmpleado();
        ClienteLocalDto creado = new ClienteLocalDto(32L, "Nuevo", "Cliente", "12345678", "5555");
        ClienteLocal referencia = clienteLocal(32L, "Nuevo", "Cliente");
        when(ownerClienteLocalService.registrar(eq(NEGOCIO_ID), any(CrearClienteLocalRequestDto.class), eq(EMAIL_OWNER)))
                .thenReturn(creado);
        when(clienteLocalRepository.getReferenceById(32L)).thenReturn(referencia);
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerTurnoDto result = service.crearManual(NEGOCIO_ID,
                request(null, new CrearClienteLocalRequestDto("Nuevo", "Cliente", "12345678", "5555"), 22L),
                EMAIL_OWNER);

        assertEquals(TurnoStatus.CONFIRMED, result.status());
        assertEquals(32L, result.clienteLocalId());
        verify(turnoRepository).save(argThat(turno -> turno.getClienteLocal() == referencia
                && turno.getEmpleado() != null && turno.getOwnerResponsable() == null));
    }

    @Test
    void creaTurnoConfirmadoConClienteExistenteAtendidoPorOwnerYConsultaAgendaGlobal() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicio(inicio);
        ClienteLocal local = clienteLocal(31L, "Cliente", "Local");
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, NEGOCIO_ID))
                .thenReturn(Optional.of(local));
        Usuario ownerBloqueado = negocio.getOwner();
        when(usuarioRepository.findWithLockingByUsuarioId(3L)).thenReturn(Optional.of(ownerBloqueado));
        when(turnoRepository.findTurnosSolapadosPorOwner(3L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of());
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerTurnoDto result = service.crearManual(NEGOCIO_ID,
                request(31L, null, null, inicio), EMAIL_OWNER);

        assertEquals(TurnoStatus.CONFIRMED, result.status());
        assertEquals(TipoResponsableTurno.OWNER, result.tipoResponsable());
        assertEquals(3L, result.ownerResponsableId());
        verify(turnoRepository).findTurnosSolapadosPorOwner(3L, inicio, inicio.plusMinutes(45));
        InOrder orden = inOrder(usuarioRepository, horarioValidator, turnoRepository);
        orden.verify(usuarioRepository).findWithLockingByUsuarioId(3L);
        orden.verify(horarioValidator).validar(NEGOCIO_ID, inicio, inicio.plusMinutes(45));
        orden.verify(turnoRepository).findTurnosSolapadosPorOwner(3L, inicio, inicio.plusMinutes(45));
    }

    @Test
    void creaClienteNuevoYTurnoConfirmadoAtendidoPorOwner() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicio(inicio);
        when(usuarioRepository.findWithLockingByUsuarioId(3L)).thenReturn(Optional.of(negocio.getOwner()));
        when(turnoRepository.findTurnosSolapadosPorOwner(3L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of());
        when(ownerClienteLocalService.registrar(eq(NEGOCIO_ID), any(CrearClienteLocalRequestDto.class), eq(EMAIL_OWNER)))
                .thenReturn(new ClienteLocalDto(32L, "Nuevo", "Cliente", "12345678", "5555"));
        when(clienteLocalRepository.getReferenceById(32L)).thenReturn(clienteLocal(32L, "Nuevo", "Cliente"));
        when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OwnerTurnoDto result = service.crearManual(NEGOCIO_ID,
                request(null, new CrearClienteLocalRequestDto("Nuevo", "Cliente", "12345678", "5555"), null, inicio),
                EMAIL_OWNER);

        assertEquals(TurnoStatus.CONFIRMED, result.status());
        assertEquals(32L, result.clienteLocalId());
        assertEquals(TipoResponsableTurno.OWNER, result.tipoResponsable());
        assertEquals(3L, result.ownerResponsableId());
    }

    @Test
    void rechazaClienteLocalDeOtroNegocioAntesDeBloquearAgenda() {
        configurarServicio(inicioFuturo());
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(77L, NEGOCIO_ID))
                .thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.crearManual(
                NEGOCIO_ID, request(77L, null, null), EMAIL_OWNER));

        verifyNoInteractions(usuarioRepository, horarioValidator, turnoRepository);
    }

    @Test
    void detectaSolapamientoDelOwnerEnOtroNegocio() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicio(inicio);
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, NEGOCIO_ID))
                .thenReturn(Optional.of(clienteLocal(31L, "Cliente", "Local")));
        when(usuarioRepository.findWithLockingByUsuarioId(3L)).thenReturn(Optional.of(negocio.getOwner()));
        when(turnoRepository.findTurnosSolapadosPorOwner(3L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of(new Turno()));

        assertStatus(HttpStatus.CONFLICT, () -> service.crearManual(
                NEGOCIO_ID, request(31L, null, null, inicio), EMAIL_OWNER));

        verify(turnoRepository, never()).save(any(Turno.class));
        verify(ownerClienteLocalService, never()).registrar(anyLong(), any(), any());
    }

    @Test
    void detectaSolapamientoDelEmpleadoYLuegoRechazaSinCrearTurno() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicioYEmpleado();
        ClienteLocal local = clienteLocal(31L, "Cliente", "Local");
        when(clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(31L, NEGOCIO_ID))
                .thenReturn(Optional.of(local));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(22L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of(new Turno()));

        assertStatus(HttpStatus.CONFLICT, () -> service.crearManual(
                NEGOCIO_ID, request(31L, null, 22L, inicio), EMAIL_OWNER));

        verify(turnoRepository, never()).save(any(Turno.class));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void horarioInvalidoSeRechazaAntesDeRegistrarClienteNuevo() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicio(inicio);
        when(usuarioRepository.findWithLockingByUsuarioId(3L)).thenReturn(Optional.of(negocio.getOwner()));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No hay horario laboral"))
                .when(horarioValidator).validar(NEGOCIO_ID, inicio, inicio.plusMinutes(45));

        assertStatus(HttpStatus.CONFLICT, () -> service.crearManual(NEGOCIO_ID,
                request(null, new CrearClienteLocalRequestDto("Nuevo", "Cliente", "12345678", "5555"), null, inicio),
                EMAIL_OWNER));

        verifyNoInteractions(ownerClienteLocalService);
        verify(turnoRepository, never()).findTurnosSolapadosPorOwner(anyLong(), any(), any());
    }

    @Test
    void fallaAlGuardarTurnoDespuesDeRegistrarClienteNuevoDentroDelServicioTransaccional() {
        configurarServicioYEmpleado();
        ClienteLocalDto creado = new ClienteLocalDto(32L, "Nuevo", "Cliente", "12345678", "5555");
        when(ownerClienteLocalService.registrar(eq(NEGOCIO_ID), any(CrearClienteLocalRequestDto.class), eq(EMAIL_OWNER)))
                .thenReturn(creado);
        when(clienteLocalRepository.getReferenceById(32L)).thenReturn(clienteLocal(32L, "Nuevo", "Cliente"));
        when(turnoRepository.save(any(Turno.class))).thenThrow(new IllegalStateException("fallo simulado"));

        assertThrows(IllegalStateException.class, () -> service.crearManual(NEGOCIO_ID,
                request(null, new CrearClienteLocalRequestDto("Nuevo", "Cliente", "12345678", "5555"), 22L),
                EMAIL_OWNER));

        verify(ownerClienteLocalService).registrar(eq(NEGOCIO_ID), any(CrearClienteLocalRequestDto.class), eq(EMAIL_OWNER));
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void rechazaAmbasOpcionesOSinClienteAunqueSeInvoqueDirectamenteElServicio() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.crearManual(NEGOCIO_ID,
                request(31L, new CrearClienteLocalRequestDto("Nuevo", "Cliente", "12345678", "5555"), null),
                EMAIL_OWNER));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.crearManual(NEGOCIO_ID,
                request(null, null, null), EMAIL_OWNER));
        verifyNoInteractions(servicioRepository, clienteLocalRepository, usuarioRepository, turnoRepository);
    }

    @Test
    void rechazaServicioDeOtroNegocio() {
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(55L, NEGOCIO_ID))
                .thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crearManual(NEGOCIO_ID,
                new CrearOwnerTurnoManualRequestDto(55L, inicioFuturo(), null, 31L, null, null), EMAIL_OWNER));
        verifyNoInteractions(usuarioRepository, turnoRepository);
    }

    @Test
    void validaAutorizacionDeTitularAntesDeConsultarServicio() {
        when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, EMAIL_OWNER))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crearManual(NEGOCIO_ID,
                request(31L, null, null), EMAIL_OWNER));
        verifyNoInteractions(servicioRepository, usuarioRepository, turnoRepository);
    }

    private OwnerTurnoDto mapear(Turno turno) {
        when(turnoRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Turno>>any(), any(Sort.class)
        )).thenReturn(List.of(turno));
        return service.listarTurnos(NEGOCIO_ID, null, null, null, EMAIL_OWNER).get(0);
    }

    private Turno turno(Usuario cliente, ClienteLocal local, Empleado empleado, Usuario owner) {
        Turno turno = new Turno();
        turno.setTurnoId(40L);
        turno.setNegocio(negocio);
        turno.setCliente(cliente);
        turno.setClienteLocal(local);
        turno.setEmpleado(empleado);
        turno.setOwnerResponsable(owner);
        Servicio servicio = new Servicio();
        servicio.setServicioId(50L);
        servicio.setNombre("Servicio test");
        turno.setServicio(servicio);
        return turno;
    }

    private Usuario usuario(Long id, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(id);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return usuario;
    }

    private Empleado empleado(Long id, String nombre, String apellido) {
        Empleado empleado = new Empleado();
        empleado.setEmpleadoId(id);
        empleado.setUsuario(usuario(id + 100L, nombre, apellido));
        return empleado;
    }

    private ClienteLocal clienteLocal(Long id, String nombre, String apellido) {
        ClienteLocal cliente = new ClienteLocal();
        cliente.setClienteLocalId(id);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        return cliente;
    }

    private void configurarServicioYEmpleado() {
        LocalDateTime inicio = inicioFuturo();
        configurarServicio(inicio);
        Empleado empleado = empleado(22L, "Eva", "Estilista");
        empleado.setNegocio(negocio);
        empleado.setActivo(true);
        when(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(22L, NEGOCIO_ID))
                .thenReturn(Optional.of(empleado));
        when(empleadoRepository.findWithLockingByEmpleadoId(22L)).thenReturn(Optional.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(22L, 50L))
                .thenReturn(Optional.of(new EmpleadoServicio()));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(22L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of());
    }

    private void configurarServicio(LocalDateTime inicio) {
        Servicio servicio = new Servicio();
        servicio.setServicioId(50L);
        servicio.setNombre("Corte");
        servicio.setPrecio(new java.math.BigDecimal("8500.00"));
        servicio.setDuracionMinutos(45);
        servicio.setActivo(true);
        servicio.setNegocio(negocio);
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(50L, NEGOCIO_ID))
                .thenReturn(Optional.of(servicio));
    }

    private LocalDateTime inicioFuturo() {
        return LocalDate.now().plusDays(14).atTime(10, 0);
    }

    private CrearOwnerTurnoManualRequestDto request(
            Long clienteLocalId,
            CrearClienteLocalRequestDto clienteNuevo,
            Long empleadoId
    ) {
        return request(clienteLocalId, clienteNuevo, empleadoId, inicioFuturo());
    }

    private CrearOwnerTurnoManualRequestDto request(
            Long clienteLocalId,
            CrearClienteLocalRequestDto clienteNuevo,
            Long empleadoId,
            LocalDateTime inicio
    ) {
        return new CrearOwnerTurnoManualRequestDto(50L, inicio, "nota", clienteLocalId, clienteNuevo, empleadoId);
    }

    private static void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable executable) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, executable);
        assertEquals(expected, exception.getStatusCode());
    }
}
