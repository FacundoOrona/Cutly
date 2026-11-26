package com.dev.cutly.empleado.service;

import com.dev.cutly.auth.config.SuperAdminInitializer;
import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.dto.turno.CrearEmpleadoTurnoRequestDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:employee_turno_create_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "security.jwt.secret=isolated-employee-test-secret-32-bytes-minimum",
        "app.bootstrap-super-admin.nombre=Test", "app.bootstrap-super-admin.apellido=Admin",
        "app.bootstrap-super-admin.dni=99999999", "app.bootstrap-super-admin.email=employee-admin@test.local",
        "app.bootstrap-super-admin.password=test-password-only"
})
class EmpleadoTurnoCreacionIntegrationTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String DNI_TEST = "7788990011";
    private static final String DATABASE_URL =
            "jdbc:h2:mem:employee_turno_create_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000";

    @Autowired private EmpleadoTurnoService service;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private EmpleadoRepository empleadoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private EmpleadoServiciorRepository empleadoServicioRepository;
    @Autowired private HorarioNegocioRepository horarioRepository;
    @Autowired private ClienteLocalRepository clienteLocalRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private org.springframework.core.env.Environment environment;
    @MockitoBean private SuperAdminInitializer superAdminInitializer;
    @MockitoBean private NotificacionTurnoService notificacionTurnoService;

    private Fixture fixture;

    @BeforeEach
    void preparar() {
        assertEquals(DATABASE_URL, environment.getRequiredProperty("spring.datasource.url"),
                "La prueba debe ejecutarse exclusivamente en la base H2 descartable");
        int seq = SEQUENCE.incrementAndGet();
        LocalDateTime inicio = LocalDate.now().plusDays(30).atTime(10, 0);
        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            Usuario owner = usuario("owner" + seq + "@test.local", Rol.OWNER, 1);
            Usuario empleadoUsuario = usuario("empleado" + seq + "@test.local", Rol.EMPLOYEE, 2);
            usuarioRepository.saveAll(List.of(owner, empleadoUsuario));
            Negocio negocio = new Negocio(); negocio.setNombre("Negocio " + seq); negocio.setStatus(NegocioStatus.ACTIVE);
            negocio.setOwner(owner); negocioRepository.save(negocio);
            Empleado empleado = new Empleado(); empleado.setUsuario(empleadoUsuario); empleado.setNegocio(negocio);
            empleado.setActivo(true); empleadoRepository.save(empleado);
            Servicio servicio = new Servicio(); servicio.setNombre("Corte"); servicio.setPrecio(new BigDecimal("20.00"));
            servicio.setDuracionMinutos(30); servicio.setActivo(true); servicio.setNegocio(negocio); servicioRepository.save(servicio);
            EmpleadoServicio asignacion = new EmpleadoServicio(); asignacion.setEmpleado(empleado); asignacion.setServicio(servicio);
            empleadoServicioRepository.save(asignacion);
            HorarioNegocio horario = new HorarioNegocio(); horario.setNegocio(negocio);
            horario.setDiaSemana(DiaSemana.valueOf(inicio.getDayOfWeek().name()));
            horario.setHoraApertura(LocalTime.of(9, 0)); horario.setHoraCierre(LocalTime.of(18, 0));
            horario.setCerrado(false); horarioRepository.save(horario);
            ClienteLocal cliente = new ClienteLocal(); cliente.setNegocio(negocio); cliente.setNombre("Cliente");
            cliente.setApellido("Existente"); cliente.setDniPresentado("11111111"); cliente.setDniNormalizado("11111111");
            cliente.setTelefono("555"); clienteLocalRepository.save(cliente);
            return new Fixture(negocio.getNegocioId(), empleado.getEmpleadoId(), servicio.getServicioId(),
                    cliente.getClienteLocalId(), empleadoUsuario.getEmail(), inicio);
        });
    }

    @AfterEach
    void limpiar() {
        if (fixture == null) return;
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            turnoRepository.deleteAll(turnoRepository.findTurnosSolapadosPorEmpleado(fixture.empleadoId(),
                    fixture.inicio().minusHours(1), fixture.inicio().plusHours(2)));
            clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(fixture.clienteId(), fixture.negocioId())
                    .ifPresent(clienteLocalRepository::delete);
            empleadoServicioRepository.deleteAll(empleadoServicioRepository
                    .findByEmpleado_EmpleadoIdAndServicio_ServicioId(fixture.empleadoId(), fixture.servicioId()).stream().toList());
            horarioRepository.deleteAll(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(
                    fixture.negocioId()));
            servicioRepository.deleteById(fixture.servicioId());
            Empleado empleado = empleadoRepository.findById(fixture.empleadoId()).orElseThrow();
            Long ownerId = empleado.getNegocio().getOwner().getUsuarioId();
            empleadoRepository.delete(empleado);
            negocioRepository.deleteById(fixture.negocioId());
            usuarioRepository.deleteAllById(List.of(ownerId, usuarioRepository.findByEmail(fixture.email()).orElseThrow().getUsuarioId()));
        });
        fixture = null;
    }

    @Test
    void solicitudesConcurrentesParaElMismoHorarioCreanUnSoloTurno() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            Future<HttpStatus> a = pool.submit(() -> crearTrasBarrera(ready, start));
            Future<HttpStatus> b = pool.submit(() -> crearTrasBarrera(ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            List<HttpStatus> results = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(s -> s == HttpStatus.CREATED).count());
            assertEquals(1, results.stream().filter(s -> s == HttpStatus.CONFLICT).count());
            assertEquals(1, turnoRepository.findTurnosSolapadosPorEmpleado(fixture.empleadoId(), fixture.inicio(),
                    fixture.inicio().plusMinutes(30)).size());
        } finally { start.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS); }
    }

    @Test
    void permiteTurnosContiguosParaElMismoEmpleado() {
        service.crear(new CrearEmpleadoTurnoRequestDto(fixture.servicioId(), fixture.inicio(), fixture.clienteId(), null),
                fixture.email());
        service.crear(new CrearEmpleadoTurnoRequestDto(fixture.servicioId(), fixture.inicio().plusMinutes(30),
                fixture.clienteId(), null), fixture.email());

        assertEquals(2, turnoRepository.findTurnosSolapadosPorEmpleado(fixture.empleadoId(), fixture.inicio(),
                fixture.inicio().plusMinutes(60)).size());
    }

    @Test
    void fallaLaNotificacionYRevierteClienteLocalYTurno() {
        String dni = DNI_TEST + fixture.negocioId();
        doThrow(new IllegalStateException("fallo inducido de notificación"))
                .when(notificacionTurnoService).notificarTurnoCreadoPorEmpleado(org.mockito.ArgumentMatchers.any());
        var request = new CrearEmpleadoTurnoRequestDto(fixture.servicioId(), fixture.inicio(), null,
                new com.dev.cutly.empleado.dto.cliente.CrearEmpleadoClienteLocalRequestDto("Ana", "Prueba", dni, "555"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.crear(request, fixture.email()));

        assertFalse(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(fixture.negocioId(), dni));
        assertTrue(turnoRepository.findTurnosSolapadosPorEmpleado(fixture.empleadoId(), fixture.inicio(),
                fixture.inicio().plusMinutes(30)).isEmpty());
    }

    private HttpStatus crearTrasBarrera(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try { if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Barrera expirada"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        try {
            service.crear(new CrearEmpleadoTurnoRequestDto(fixture.servicioId(), fixture.inicio(), fixture.clienteId(), null),
                    fixture.email());
            return HttpStatus.CREATED;
        } catch (ResponseStatusException e) { return HttpStatus.valueOf(e.getStatusCode().value()); }
    }

    private Usuario usuario(String email, Rol rol, int suffix) {
        Usuario usuario = new Usuario(); usuario.setNombre("Prueba"); usuario.setApellido("Empleado");
        usuario.setDni(String.format("%08d", SEQUENCE.get() * 10 + suffix)); usuario.setEmail(email);
        usuario.setContrasena("hash-de-prueba"); usuario.setRol(rol); usuario.setStatus(UsuarioStatus.ACTIVE);
        return usuario;
    }

    private record Fixture(Long negocioId, Long empleadoId, Long servicioId, Long clienteId,
                           String email, LocalDateTime inicio) { }
}
