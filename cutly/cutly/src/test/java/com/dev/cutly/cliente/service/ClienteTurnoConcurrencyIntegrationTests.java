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
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://127.0.0.1:13306/cutly_concurrency_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        "spring.datasource.username=root",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=isolated-concurrency-test-secret-32-bytes-minimum"
})
class ClienteTurnoConcurrencyIntegrationTests {

    private static final AtomicInteger DNI_SEQUENCE = new AtomicInteger(10000000);
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

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

    @MockitoBean
    private SuperAdminInitializer superAdminInitializer;

    private Fixture fixture;
    private LocalDateTime inicio;

    @BeforeEach
    void prepararDatosAislados() {
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
                    clienteUno.getEmail(),
                    clienteDos.getEmail(),
                    negocio.getNegocioId(),
                    empleado.getEmpleadoId(),
                    servicio.getServicioId()
            );
        });

        assertNotNull(fixture);
    }

    @RepeatedTest(5)
    void dosClientesConcurrentesNoPuedenReservarElMismoEmpleadoEIntervalo() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch bloqueoInicialAdquirido = new CountDownLatch(1);
        CountDownLatch liberarBloqueoInicial = new CountDownLatch(1);

        try {
            Future<?> bloqueoInicial = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                empleadoRepository.findWithLockingByEmpleadoId(fixture.empleadoId())
                        .orElseThrow(() -> new AssertionError("No existe el empleado de prueba"));
                bloqueoInicialAdquirido.countDown();
                await(liberarBloqueoInicial);
                return null;
            }));

            assertTrue(bloqueoInicialAdquirido.await(10, TimeUnit.SECONDS),
                    "La transacción de coordinación no obtuvo el bloqueo del empleado");

            Future<ResultadoReserva> solicitudUno = executor.submit(
                    () -> reservar(fixture.clienteUnoEmail(), inicio));
            Future<ResultadoReserva> solicitudDos = executor.submit(
                    () -> reservar(fixture.clienteDosEmail(), inicio));

            assertTrue(esperarDosSolicitudesBloqueadas(),
                    "Las dos solicitudes no llegaron a esperar por la misma fila de empleado");

            liberarBloqueoInicial.countDown();

            List<ResultadoReserva> resultados = List.of(
                    solicitudUno.get(15, TimeUnit.SECONDS),
                    solicitudDos.get(15, TimeUnit.SECONDS)
            );
            bloqueoInicial.get(15, TimeUnit.SECONDS);

            assertEquals(1, resultados.stream().filter(ResultadoReserva::creada).count(),
                    "Solo una reserva incompatible debe guardarse");
            assertEquals(1, resultados.stream()
                            .filter(resultado -> resultado.estado() == HttpStatus.CONFLICT).count(),
                    "La solicitud concurrente perdedora debe responder con conflicto");
            assertEquals(1, turnoRepository.findTurnosSolapadosPorEmpleado(
                    fixture.empleadoId(), inicio, inicio.plusMinutes(45)).size());
        } finally {
            liberarBloqueoInicial.countDown();
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

    @Test
    void servidorDePruebaUsaInnoDbYRepeatableRead() {
        assertEquals("REPEATABLE-READ", jdbcTemplate.queryForObject(
                "select @@global.transaction_isolation", String.class));
        assertEquals("InnoDB", jdbcTemplate.queryForObject(
                "select engine from information_schema.tables "
                        + "where table_schema = database() and table_name = 'turnos'",
                String.class));
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

    private boolean esperarDosSolicitudesBloqueadas() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer solicitudesEsperando = jdbcTemplate.queryForObject(
                    "select count(*) from performance_schema.data_lock_waits w "
                            + "join performance_schema.data_locks l "
                            + "on l.engine = w.engine "
                            + "and l.engine_lock_id = w.requesting_engine_lock_id "
                            + "where l.object_schema = database() "
                            + "and l.object_name = 'empleados' "
                            + "and l.lock_status = 'WAITING'",
                    Integer.class
            );
            if (solicitudesEsperando != null && solicitudesEsperando >= 2) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
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

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("Expiró la espera de sincronización del empleado");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Se interrumpió la sincronización del empleado", exception);
        }
    }

    private record Fixture(
            String clienteUnoEmail,
            String clienteDosEmail,
            Long negocioId,
            Long empleadoId,
            Long servicioId
    ) {
    }

    private record ResultadoReserva(boolean creada, HttpStatus estado) {
    }
}
