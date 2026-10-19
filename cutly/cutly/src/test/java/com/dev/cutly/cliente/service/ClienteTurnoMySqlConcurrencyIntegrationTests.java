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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in MySQL integration test. Run with:
 * mvnw "-Dcutly.mysql.integration=true" "-Dtest=ClienteTurnoMySqlConcurrencyIntegrationTests" test
 */
@Testcontainers
@EnabledIfSystemProperty(named = "cutly.mysql.integration", matches = "true")
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "security.jwt.secret=mysql-concurrency-test-secret-32-bytes-minimum"
})
@Import(ClienteTurnoMySqlConcurrencyIntegrationTests.LockstepConfiguration.class)
class ClienteTurnoMySqlConcurrencyIntegrationTests {

    private static final String DATABASE_NAME = "cutly_concurrency_test";
    private static final String DATABASE_USERNAME = "cutly_test_user";
    private static final String DATABASE_PASSWORD = "ephemeral_mysql_test_password";
    private static final AtomicInteger DNI_SEQUENCE = new AtomicInteger(20000000);
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();
    private static final AtomicReference<CountDownLatch> CLIENT_OVERLAP_BARRIER = new AtomicReference<>();

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(
            DockerImageName.parse("mysql:8.4"))
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD)
            .withCommand("--transaction-isolation=REPEATABLE-READ", "--innodb-lock-wait-timeout=5");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        properties.add("app.bootstrap-super-admin.nombre", () -> "Test");
        properties.add("app.bootstrap-super-admin.apellido", () -> "Admin");
        properties.add("app.bootstrap-super-admin.dni", () -> "99999997");
        properties.add("app.bootstrap-super-admin.email", () -> "mysql-test-admin@test.local");
        properties.add("app.bootstrap-super-admin.password", () -> "test-only-password");
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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @MockitoBean
    private SuperAdminInitializer superAdminInitializer;

    private Fixture fixture;
    private LocalDateTime inicio;

    @BeforeEach
    void verificarDatasourceMySqlDesechableYPrepararFixture() {
        assertTrue(MYSQL.isRunning(), "El contenedor MySQL exclusivo de esta prueba debe estar activo");
        assertEquals(MYSQL.getJdbcUrl(), environment.getRequiredProperty("spring.datasource.url"));
        assertEquals(DATABASE_USERNAME, environment.getRequiredProperty("spring.datasource.username"));
        assertEquals(DATABASE_NAME, jdbcTemplate.queryForObject("select database()", String.class));
        assertEquals("REPEATABLE-READ", jdbcTemplate.queryForObject(
                "select @@global.transaction_isolation", String.class));
        assertEquals("InnoDB", jdbcTemplate.queryForObject(
                "select engine from information_schema.tables "
                        + "where table_schema = database() and table_name = 'turnos'",
                String.class));

        String suffix = Integer.toString(FIXTURE_SEQUENCE.incrementAndGet());
        inicio = LocalDate.now().plusDays(30).atTime(10, 0);

        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            Usuario owner = usuario("mysql-owner" + suffix + "@test.local", Rol.OWNER);
            usuarioRepository.save(owner);

            Usuario clienteUno = usuario("mysql-client1_" + suffix + "@test.local", Rol.CLIENT);
            Usuario clienteDos = usuario("mysql-client2_" + suffix + "@test.local", Rol.CLIENT);
            usuarioRepository.saveAll(List.of(clienteUno, clienteDos));

            Usuario empleadoUsuario = usuario("mysql-employee" + suffix + "@test.local", Rol.EMPLOYEE);
            usuarioRepository.save(empleadoUsuario);

            Negocio negocio = new Negocio();
            negocio.setNombre("Negocio MySQL de prueba " + suffix);
            negocio.setStatus(NegocioStatus.ACTIVE);
            negocio.setOwner(owner);
            negocioRepository.save(negocio);

            Empleado empleado = new Empleado();
            empleado.setUsuario(empleadoUsuario);
            empleado.setNegocio(negocio);
            empleado.setActivo(true);
            empleadoRepository.save(empleado);

            Servicio servicio = new Servicio();
            servicio.setNombre("Servicio MySQL de prueba");
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
    void limpiarFixture() {
        CLIENT_OVERLAP_BARRIER.set(null);
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
    void dosClientesConLecturaInicialConcurrenteNoPuedenReservarElMismoEmpleadoEIntervalo() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ambasConsultasIniciales = new CountDownLatch(2);
        CLIENT_OVERLAP_BARRIER.set(ambasConsultasIniciales);
        Future<ResultadoReserva> solicitudUno = null;
        Future<ResultadoReserva> solicitudDos = null;

        try {
            solicitudUno = executor.submit(() -> reservar(fixture.clienteUnoEmail(), inicio));
            solicitudDos = executor.submit(() -> reservar(fixture.clienteDosEmail(), inicio));

            List<ResultadoReserva> resultados = List.of(
                    solicitudUno.get(30, TimeUnit.SECONDS),
                    solicitudDos.get(30, TimeUnit.SECONDS)
            );

            assertEquals(1, resultados.stream().filter(ResultadoReserva::creada).count(),
                    "Solo una reserva incompatible debe guardarse");
            assertEquals(1, resultados.stream()
                            .filter(resultado -> resultado.estado() == HttpStatus.CONFLICT).count(),
                    "La solicitud que pierde el bloqueo debe responder 409 Conflict");
            assertEquals(1, turnoRepository.findTurnosSolapadosPorEmpleado(
                    fixture.empleadoId(), inicio, inicio.plusMinutes(45)).size());
        } finally {
            ambasConsultasIniciales.countDown();
            cancelarSiPendiente(solicitudUno);
            cancelarSiPendiente(solicitudDos);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS),
                    "Las transacciones/hilos de reserva deben finalizar antes de limpiar el fixture");
            CLIENT_OVERLAP_BARRIER.compareAndSet(ambasConsultasIniciales, null);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class LockstepConfiguration {

        @Bean
        ClientOverlapReadBarrier clientOverlapReadBarrier() {
            return new ClientOverlapReadBarrier();
        }
    }

    @Aspect
    static class ClientOverlapReadBarrier {

        @Around("execution(* com.dev.cutly.turno.repository.TurnoRepository.findTurnosClienteSolapados(..))")
        Object esperarAmbasLecturasIniciales(ProceedingJoinPoint invocation) throws Throwable {
            Object result = invocation.proceed();
            CountDownLatch barrier = CLIENT_OVERLAP_BARRIER.get();
            if (barrier != null) {
                if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                    throw new AssertionError("La consulta inicial debe ejecutarse dentro de una transacción real");
                }
                if (!(result instanceof List<?> turnos) || !turnos.isEmpty()) {
                    throw new AssertionError("Las dos consultas iniciales deben observar cero turnos solapados");
                }
                // Ambos lectores terminan la validación inicial antes de que cualquiera pida el lock del empleado.
                barrier.countDown();
                if (!barrier.await(15, TimeUnit.SECONDS)) {
                    throw new AssertionError("Las dos transacciones no llegaron a la barrera de lectura inicial");
                }
            }
            return result;
        }
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
                fixture.negocioId(), fixture.servicioId(), fixture.empleadoId(), fechaHoraInicio, null);
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

    private static void cancelarSiPendiente(Future<?> task) {
        if (task != null && !task.isDone()) {
            task.cancel(true);
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
