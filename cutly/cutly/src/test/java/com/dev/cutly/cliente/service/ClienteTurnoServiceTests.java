package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.cliente.ClienteTurnoDto;
import com.dev.cutly.cliente.dto.cliente.CrearTurnoRequestDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteTurnoServiceTests {
    private static final String EMAIL = "cliente@cutly.local";
    private static final Long CLIENTE_ID = 1L;
    private static final Long NEGOCIO_ID = 2L;
    private static final Long SERVICIO_ID = 3L;
    private static final BigDecimal PRECIO = new BigDecimal("8500.00");

    @Mock private TurnoRepository turnoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NegocioRepository negocioRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private EmpleadoServiciorRepository empleadoServicioRepository;
    @Mock private HorarioNegocioRepository horarioRepository;

    private ClienteTurnoService service;
    private Usuario cliente;
    private Negocio negocio;
    private Servicio servicio;
    private Empleado empleado;
    private HorarioNegocio horario;
    private LocalDateTime inicio;

    @BeforeEach
    void setUp() {
        service = new ClienteTurnoService(turnoRepository, usuarioRepository, negocioRepository,
                servicioRepository, empleadoRepository, empleadoServicioRepository, horarioRepository);
        cliente = usuario(CLIENTE_ID, Rol.CLIENT, "Carla", "Cliente");
        Usuario owner = usuario(10L, Rol.OWNER, "Olga", "Owner");
        negocio = new Negocio();
        negocio.setNegocioId(NEGOCIO_ID);
        negocio.setNombre("Corte Norte");
        negocio.setStatus(NegocioStatus.ACTIVE);
        negocio.setOwner(owner);

        servicio = new Servicio();
        servicio.setServicioId(SERVICIO_ID);
        servicio.setNombre("Corte");
        servicio.setPrecio(PRECIO);
        servicio.setDuracionMinutos(45);
        servicio.setActivo(true);
        servicio.setNegocio(negocio);

        empleado = empleado(20L, negocio, true, "Eva", "Estilista");
        inicio = LocalDate.now().plusDays(14).atTime(10, 0);
        horario = horario(inicio.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(18, 0), false);

        lenient().when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cliente));
        lenient().when(usuarioRepository.findWithLockingByUsuarioId(CLIENTE_ID)).thenReturn(Optional.of(cliente));
        lenient().when(negocioRepository.findByNegocioIdAndStatus(NEGOCIO_ID, NegocioStatus.ACTIVE)).thenReturn(Optional.of(negocio));
        lenient().when(servicioRepository.findByServicioIdAndNegocio_NegocioId(SERVICIO_ID, NEGOCIO_ID)).thenReturn(Optional.of(servicio));
        lenient().when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID)).thenReturn(List.of(horario));
        lenient().when(turnoRepository.findTurnosClienteSolapados(eq(CLIENTE_ID), any(), any())).thenReturn(List.of());
        lenient().when(turnoRepository.findTurnosSolapadosPorEmpleado(eq(20L), any(), any())).thenReturn(List.of());
        lenient().when(empleadoRepository.findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(NEGOCIO_ID)).thenReturn(List.of(empleado));
        lenient().when(empleadoRepository.findWithLockingByEmpleadoId(20L)).thenReturn(Optional.of(empleado));
        lenient().when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(20L, SERVICIO_ID))
                .thenReturn(Optional.of(new EmpleadoServicio()));
        lenient().when(turnoRepository.save(any(Turno.class))).thenAnswer(invocation -> {
            Turno saved = invocation.getArgument(0);
            saved.setTurnoId(99L);
            return saved;
        });
    }

    @Test
    void creaTurnoConHorarioPrecioEstadoRelacionesYDtoEsperados() {
        ClienteTurnoDto dto = service.crear(request(null, inicio, "Preferencia: tijera"), EMAIL);

        ArgumentCaptor<Turno> captor = ArgumentCaptor.forClass(Turno.class);
        verify(turnoRepository).save(captor.capture());
        Turno guardado = captor.getValue();
        assertEquals(inicio, guardado.getFechaHoraInicio());
        assertEquals(inicio.plusMinutes(45), guardado.getFechaHoraFin());
        assertEquals(TurnoStatus.PENDING, guardado.getStatus());
        assertEquals(PRECIO, guardado.getPrecio());
        assertEquals("Preferencia: tijera", guardado.getObservaciones());
        assertSame(cliente, guardado.getCliente());
        assertSame(negocio, guardado.getNegocio());
        assertSame(empleado, guardado.getEmpleado());
        assertSame(servicio, guardado.getServicio());
        assertEquals(99L, dto.turnoId());
        assertEquals(inicio.plusMinutes(45), dto.fechaHoraFin());
        assertEquals(20L, dto.empleadoId());
        assertEquals(PRECIO, dto.precio());
        assertEquals("Corte Norte", dto.negocioNombre());
        assertEquals("Eva", dto.empleadoNombre());
    }

    @Test
    void clienteInexistenteEs401() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.UNAUTHORIZED, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void cuentaConRolIncorrectoEs403() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(CLIENTE_ID, Rol.OWNER, "O", "W")));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(usuarioRepository, never()).findWithLockingByUsuarioId(anyLong());
    }

    @Test
    void negocioInexistenteOInactivoEs404() {
        when(negocioRepository.findByNegocioIdAndStatus(NEGOCIO_ID, NegocioStatus.ACTIVE)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void servicioInexistenteODeOtroNegocioEs404() {
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(SERVICIO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void servicioInactivoEs404() {
        servicio.setActivo(false);
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void empleadoSolicitadoInexistenteDeOtroNegocioOInactivoEs404() {
        when(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(77L, NEGOCIO_ID)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crear(request(77L, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void empleadoInactivoSeDescartaEnRevalidacion() {
        empleado.setActivo(false);
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void empleadoIncompatibleEs409() {
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(20L, SERVICIO_ID))
                .thenReturn(Optional.empty());
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(empleadoRepository, never()).findWithLockingByEmpleadoId(anyLong());
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void inicioNuloEs400EnLugarDeNullPointerException() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.crear(request(null, null, null), EMAIL));
        verifyNoInteractions(negocioRepository, turnoRepository);
    }

    @Test
    void inicioEnElPasadoEs400() {
        LocalDateTime pasado = LocalDateTime.now().minusDays(1);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.crear(request(null, pasado, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void diaCerradoEs409() {
        horario.setCerrado(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void inicioAntesDeAperturaEs409() {
        horario.setHoraApertura(LocalTime.of(10, 1));
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void finDespuesDelCierreEs409PeroFinExactoAlCierreSePermite() {
        horario.setHoraCierre(LocalTime.of(10, 44));
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());

        horario.setHoraCierre(LocalTime.of(10, 45));
        ClienteTurnoDto dto = service.crear(request(null, inicio, null), EMAIL);
        assertEquals(inicio.plusMinutes(45), dto.fechaHoraFin());
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void puedeReservarEnElSegundoTramoDelDia() {
        HorarioNegocio segundoTramo = horario(inicio.getDayOfWeek(), LocalTime.of(17, 0), LocalTime.of(20, 0), false);
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario(inicio.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.NOON, false), segundoTramo));
        LocalDateTime segundoInicio = inicio.toLocalDate().atTime(17, 0);

        ClienteTurnoDto dto = service.crear(request(null, segundoInicio, null), EMAIL);

        assertEquals(segundoInicio, dto.fechaHoraInicio());
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void rechazaReservaEnLaPausaEntreTramos() {
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario(inicio.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.NOON, false),
                        horario(inicio.getDayOfWeek(), LocalTime.of(17, 0), LocalTime.of(20, 0), false)));

        assertStatus(HttpStatus.CONFLICT, () -> service.crear(
                request(null, inicio.toLocalDate().atTime(13, 0), null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void rechazaServicioQueAtraviesaElFinDelPrimerTramoAunqueElSegundoEsteAbierto() {
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario(inicio.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.NOON, false),
                        horario(inicio.getDayOfWeek(), LocalTime.of(17, 0), LocalTime.of(20, 0), false)));

        assertStatus(HttpStatus.CONFLICT, () -> service.crear(
                request(null, inicio.toLocalDate().atTime(11, 30), null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void seleccionaSegundoEmpleadoSiPrimeroTieneSolapamiento() {
        Empleado segundo = empleado(21L, negocio, true, "Luz", "Estilista");
        when(empleadoRepository.findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(NEGOCIO_ID))
                .thenReturn(List.of(empleado, segundo));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(eq(20L), any(), any())).thenReturn(List.of(new Turno()));
        when(empleadoRepository.findWithLockingByEmpleadoId(21L)).thenReturn(Optional.of(segundo));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(21L, SERVICIO_ID))
                .thenReturn(Optional.of(new EmpleadoServicio()));

        ClienteTurnoDto dto = service.crear(request(null, inicio, null), EMAIL);

        assertEquals(21L, dto.empleadoId());
        verify(turnoRepository).findTurnosSolapadosPorEmpleado(20L, inicio, inicio.plusMinutes(45));
        verify(turnoRepository).findTurnosSolapadosPorEmpleado(21L, inicio, inicio.plusMinutes(45));
        verify(turnoRepository).save(argThat(t -> t.getEmpleado().getEmpleadoId().equals(21L)));
    }

    @Test
    void solapamientoDelClienteAunqueElEmpleadoSeaDistintoEs409() {
        when(turnoRepository.findTurnosClienteSolapados(CLIENTE_ID, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of(new Turno()));
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());
        verify(empleadoRepository, never()).findWithLockingByEmpleadoId(anyLong());
    }

    @Test
    void turnoConsecutivoDelEmpleadoSePermiteEnElLimite() {
        LocalDateTime finAnterior = inicio;
        LocalDateTime finNuevo = inicio.plusMinutes(45);
        when(turnoRepository.findTurnosSolapadosPorEmpleado(20L, inicio, finNuevo)).thenReturn(List.of());
        ClienteTurnoDto dto = service.crear(request(null, inicio, null), EMAIL);
        assertEquals(finNuevo, dto.fechaHoraFin());
        verify(turnoRepository).findTurnosSolapadosPorEmpleado(20L, finAnterior, finNuevo);
        verify(turnoRepository).save(any(Turno.class));
    }

    @Test
    void conflictoDelEmpleadoProvoca409CuandoNoHayAlternativa() {
        when(turnoRepository.findTurnosSolapadosPorEmpleado(20L, inicio, inicio.plusMinutes(45)))
                .thenReturn(List.of(new Turno()));
        assertStatus(HttpStatus.CONFLICT, () -> service.crear(request(null, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void requestEmpleadoInactivoNoSeSustituyeAutomaticamente() {
        when(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(20L, NEGOCIO_ID)).thenReturn(Optional.of(empleado));
        empleado.setActivo(false);
        assertStatus(HttpStatus.NOT_FOUND, () -> service.crear(request(20L, inicio, null), EMAIL));
        verify(turnoRepository, never()).save(any());
    }

    private CrearTurnoRequestDto request(Long empleadoId, LocalDateTime fechaHoraInicio, String observaciones) {
        return new CrearTurnoRequestDto(NEGOCIO_ID, SERVICIO_ID, empleadoId, fechaHoraInicio, observaciones);
    }

    private static Usuario usuario(Long id, Rol rol, String nombre, String apellido) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(id);
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        return usuario;
    }

    private static Empleado empleado(Long id, Negocio negocio, boolean activo, String nombre, String apellido) {
        Empleado empleado = new Empleado();
        empleado.setEmpleadoId(id);
        empleado.setNegocio(negocio);
        empleado.setActivo(activo);
        empleado.setUsuario(usuario(id + 100, Rol.EMPLOYEE, nombre, apellido));
        return empleado;
    }

    private static HorarioNegocio horario(java.time.DayOfWeek dia, LocalTime apertura, LocalTime cierre, boolean cerrado) {
        HorarioNegocio horario = new HorarioNegocio();
        horario.setDiaSemana(DiaSemana.valueOf(dia.name()));
        horario.setHoraApertura(apertura);
        horario.setHoraCierre(cierre);
        horario.setCerrado(cerrado);
        return horario;
    }

    private static void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable executable) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, executable);
        assertEquals(expected, exception.getStatusCode());
    }
}
