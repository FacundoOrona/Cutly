package com.dev.cutly.cliente.service;

import com.dev.cutly.auth.config.SuperAdminInitializer;
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
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cutly_concurrency_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "security.jwt.secret=isolated-concurrency-test-secret-32-bytes-minimum",
        "app.bootstrap-super-admin.nombre=Test",
        "app.bootstrap-super-admin.apellido=Admin",
        "app.bootstrap-super-admin.dni=99999999",
        "app.bootstrap-super-admin.email=test-admin@test.local",
        "app.bootstrap-super-admin.password=test-password-only"
})
class ClienteTurnoConcurrencyIntegrationTests {

    private static final String TEST_DATABASE_URL =
            "jdbc:h2:mem:cutly_concurrency_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000";
    private static final AtomicInteger DNI_SEQUENCE = new AtomicInteger(10000000);
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    static {
        boolean hasSafeDatasource = Arrays.stream(
                        ClienteTurnoConcurrencyIntegrationTests.class
                                .getAnnotation(SpringBootTest.class).properties())
                .anyMatch(property -> property.equals("spring.datasource.url=" + TEST_DATABASE_URL));
        if (!hasSafeDatasource) {
            throw new ExceptionInInitializerError(
                    "La prueba de concurrencia exige la URL H2 descartable configurada explícitamente"
            );
        }
    }

    @Autowired
    private ClienteTurnoService clienteTurnoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NegocioRepository negocioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private EmpleadoServiciorRepository empleadoServicioRepository;

    @Autowired
    private HorarioNegocioRepository horarioNegocioRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @MockitoBean
    private SuperAdminInitializer superAdminInitializer;

    private Fixture fixture;
    private LocalDateTime inicio;

    @BeforeEach
    void verificarBaseDescartableYPrepararDatos() {
        String url = environment.getRequiredProperty("spring.datasource.url");
        assertTrue(url.equals(TEST_DATABASE_URL),
                "La prueba solo puede ejecutarse contra la base H2 descartable configurada explícitamente");

        String suffix = Integer.toString(FIXTURE_SEQUENCE.incrementAndGet());
        inicio = LocalDate.now().plusDays(30).atTime(10, 0);

        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            Usuario owner = usuario("owner" + suffix + "@test.local", Rol.OWNER);
            usuarioRepository.save(owner);

            Usuario clienteUno = usuario("cliente1_" + suffix + "@test.local", Rol.CLIENT);
            Usuario clienteDos = usuario("cliente2_" + suffix + "@test.local", Rol.CLIENT);
            usuarioRepository.saveAll(List.of(clienteUno, clienteDos));

            Usuario empleadoUsuario = usuario("empleado" + suffix + "@test.local", Rol.EMPLOYEE);
            usuarioRepository.save(empleadoUsuario);

            Negocio negocio = new Negocio();
            negocio.setNombre("Negocio de prueba " + suffix);
            negocio.setStatus(NegocioStatus.ACTIVE);
            negocio.setOwner(owner);
            negocioRepository.save(negocio);

            Empleado empleado = new Empleado();
            empleado.setUsuario(empleadoUsuario);
            empleado.setNegocio(negocio);
            empleado.setActivo(true);
            empleadoRepository.save(empleado);

            Servicio servicio = new Servicio();
            servicio.setNombre("Servicio de prueba");
            servicio.setPrecio(new BigDecimal("25.00"));
            servicio.setDuracionMinutos(45);
            servicio.setActivo(true);
            servicio.setNegocio(negocio);
            servicioRepository.save(servicio);

            EmpleadoServicio asignacion = new EmpleadoServicio();
            asignacion.setEmpleado(empleado);
            asignacion.setServicio(servicio);
            empleadoServicioRepository.save(asignacion);

            HorarioNegocio horario = new HorarioNegocio();
            horario.setNegocio(negocio);
            horario.setDiaSemana(DiaSemana.valueOf(inicio.getDayOfWeek().name()));
            horario.setHoraApertura(LocalTime.of(9, 0));
            horario.setHoraCierre(LocalTime.of(18, 0));
            horario.setCerrado(false);
            horarioNegocioRepository.save(horario);

            return new Fixture(
                    owner.getUsuarioId(),
                    clienteUno.getUsuarioId(),
                    clienteUno.getEmail(),
                    clienteDos.getUsuarioId(),
                    clienteDos.getEmail(),
                    empleadoUsuario.getUsuarioId(),
                    negocio.getNegocioId(),
                    empleado.getEmpleadoId(),
                    servicio.getServicioId()
            );
        });

        assertNotNull(fixture);
    }

    @AfterEach
    void limpiarDatosDelFixture() {
        if (fixture == null) {
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<Turno> turnos = turnoRepository.findAll().stream()
                    .filter(turno -> turno.getNegocio().getNegocioId().equals(fixture.negocioId()))
                    .toList();
            turnoRepository.deleteAll(turnos);
            empleadoServicioRepository.deleteAll(empleadoServicioRepository
                    .findByEmpleado_EmpleadoIdAndServicio_ServicioId(fixture.empleadoId(), fixture.servicioId())
                    .stream().toList());
            horarioNegocioRepository.deleteAll(horarioNegocioRepository
                    .findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(fixture.negocioId()));
            servicioRepository.deleteById(fixture.servicioId());
            empleadoRepository.deleteById(fixture.empleadoId());
            negocioRepository.deleteById(fixture.negocioId());
            usuarioRepository.deleteAllById(List.of(
                    fixture.ownerUsuarioId(), fixture.clienteUnoUsuarioId(),
                    fixture.clienteDosUsuarioId(), fixture.empleadoUsuarioId()
            ));
        });
        fixture = null;
    }

    @RepeatedTest(5)
    void dosClientesConcurrentesNoPuedenReservarElMismoEmpleadoEIntervalo() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch solicitudesPreparadas = new CountDownLatch(2);
        CountDownLatch iniciarSolicitudes = new CountDownLatch(1);
        Future<ResultadoReserva> solicitudUno = null;
        Future<ResultadoReserva> solicitudDos = null;

        try {
            solicitudUno = executor.submit(() -> reservarTrasBarrera(
                    fixture.clienteUnoEmail(), inicio, solicitudesPreparadas, iniciarSolicitudes));
            solicitudDos = executor.submit(() -> reservarTrasBarrera(
                    fixture.clienteDosEmail(), inicio, solicitudesPreparadas, iniciarSolicitudes));

            assertTrue(solicitudesPreparadas.await(10, TimeUnit.SECONDS),
                    "Las dos solicitudes no alcanzaron el punto de inicio concurrente");
            iniciarSolicitudes.countDown();

            List<ResultadoReserva> resultados = List.of(
                    solicitudUno.get(20, TimeUnit.SECONDS),
                    solicitudDos.get(20, TimeUnit.SECONDS)
            );

            assertEquals(1, resultados.stream().filter(ResultadoReserva::creada).count(),
                    "Solo una reserva incompatible debe guardarse");
            assertEquals(1, resultados.stream()
                            .filter(resultado -> resultado.estado() == HttpStatus.CONFLICT).count(),
                    "La solicitud concurrente perdedora debe responder con conflicto");
            assertEquals(1, turnoRepository.findTurnosSolapadosPorEmpleado(
                    fixture.empleadoId(), inicio, inicio.plusMinutes(45)).size());
        } finally {
            iniciarSolicitudes.countDown();
            cancelarSiPendiente(solicitudUno);
            cancelarSiPendiente(solicitudDos);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Los hilos de reserva no terminaron");
        }
    }

    @Test
    void permiteTurnosContiguosParaElMismoEmpleado() {
        clienteTurnoService.crear(request(inicio), fixture.clienteUnoEmail());
        clienteTurnoService.crear(request(inicio.plusMinutes(45)), fixture.clienteDosEmail());

        assertEquals(2, turnoRepository.findByNegocio_NegocioIdOrderByFechaHoraInicioAscTurnoIdAsc(
                fixture.negocioId()).size());
    }

    private ResultadoReserva reservarTrasBarrera(
            String email,
            LocalDateTime fechaHoraInicio,
            CountDownLatch preparadas,
            CountDownLatch iniciar
    ) {
        preparadas.countDown();
        await(iniciar);
        return reservar(email, fechaHoraInicio);
    }

    private ResultadoReserva reservar(String email, LocalDateTime fechaHoraInicio) {
        try {
            clienteTurnoService.crear(request(fechaHoraInicio), email);
            return new ResultadoReserva(true, HttpStatus.CREATED);
        } catch (ResponseStatusException exception) {
            return new ResultadoReserva(false, HttpStatus.valueOf(exception.getStatusCode().value()));
        }
    }

    private CrearTurnoRequestDto request(LocalDateTime fechaHoraInicio) {
        return new CrearTurnoRequestDto(
                fixture.negocioId(),
                fixture.servicioId(),
                fixture.empleadoId(),
                fechaHoraInicio,
                null
        );
    }

    private Usuario usuario(String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setApellido("Concurrencia");
        usuario.setDni(String.format("%08d", DNI_SEQUENCE.incrementAndGet()));
        usuario.setEmail(email);
        usuario.setContrasena("hash-de-prueba");
        usuario.setRol(rol);
        usuario.setStatus(UsuarioStatus.ACTIVE);
        return usuario;
    }

    private static void cancelarSiPendiente(Future<?> tarea) {
        if (tarea != null && !tarea.isDone()) {
            tarea.cancel(true);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("Expiró la espera de sincronización concurrente");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Se interrumpió la sincronización concurrente", exception);
        }
    }

    private record Fixture(
            Long ownerUsuarioId,
            Long clienteUnoUsuarioId,
            String clienteUnoEmail,
            Long clienteDosUsuarioId,
            String clienteDosEmail,
            Long empleadoUsuarioId,
            Long negocioId,
            Long empleadoId,
            Long servicioId
    ) {
    }

    private record ResultadoReserva(boolean creada, HttpStatus estado) {
    }
}
