package com.dev.cutly.pago;

import com.dev.cutly.auth.config.SuperAdminInitializer;
import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.pago.CrearAbonoRequestDto;
import com.dev.cutly.owner.dto.turno.DecisionFinancieraRequestDto;
import com.dev.cutly.owner.service.OwnerDeudasService;
import com.dev.cutly.owner.service.OwnerEstadisicaService;
import com.dev.cutly.owner.service.OwnerTurnosService;
import com.dev.cutly.pago.entity.DeudaCliente;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.repository.DeudaClienteRepository;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cutly_financial_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "security.jwt.secret=financial-concurrency-test-secret-32-bytes",
        "app.bootstrap-super-admin.nombre=Test",
        "app.bootstrap-super-admin.apellido=Admin",
        "app.bootstrap-super-admin.dni=99999996",
        "app.bootstrap-super-admin.email=financial-test-admin@test.local",
        "app.bootstrap-super-admin.password=test-password-only"
})
class FinancialConcurrencyIntegrationTests {
    private static final String DB_URL = "jdbc:h2:mem:cutly_financial_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    @MockitoBean SuperAdminInitializer superAdminInitializer;
    @Autowired Environment environment;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired NegocioRepository negocioRepository;
    @Autowired ServicioRepository servicioRepository;
    @Autowired ClienteLocalRepository clienteLocalRepository;
    @Autowired TurnoRepository turnoRepository;
    @Autowired ResultadoFinancieroTurnoRepository resultadoRepository;
    @Autowired DeudaClienteRepository deudaRepository;
    @Autowired PagoRepository pagoRepository;
    @Autowired OwnerTurnosService ownerTurnosService;
    @Autowired OwnerDeudasService ownerDeudasService;
    @Autowired OwnerEstadisicaService ownerEstadisicaService;
    @Autowired PlatformTransactionManager transactionManager;
    private Usuario owner;
    private Negocio negocio;
    private Turno turno;
    private String email;

    @BeforeEach void setup() {
        assertEquals(DB_URL, environment.getRequiredProperty("spring.datasource.url"), "Este test solo usa la base H2 descartable declarada");
        int n = SEQUENCE.incrementAndGet();
        email = "finance-owner-" + n + "@test.local";
        owner = new Usuario(); owner.setNombre("Owner"); owner.setApellido("Test"); owner.setDni(String.format("%08d", 81000000 + n));
        owner.setEmail(email); owner.setContrasena("test-hash"); owner.setRol(Rol.OWNER); owner.setStatus(UsuarioStatus.ACTIVE);
        owner = usuarioRepository.saveAndFlush(owner);
        negocio = new Negocio(); negocio.setNombre("Finance test " + n); negocio.setStatus(NegocioStatus.ACTIVE); negocio.setOwner(owner);
        negocio = negocioRepository.saveAndFlush(negocio);
        Servicio servicio = new Servicio(); servicio.setNombre("Servicio"); servicio.setPrecio(new BigDecimal("1000.00")); servicio.setDuracionMinutos(30); servicio.setActivo(true); servicio.setNegocio(negocio);
        servicio = servicioRepository.saveAndFlush(servicio);
        ClienteLocal cliente = new ClienteLocal(); cliente.setNegocio(negocio); cliente.setNombre("Ana"); cliente.setApellido("Test");
        cliente.setDniPresentado("12345678"); cliente.setDniNormalizado("12345678"); cliente.setTelefono("5555");
        cliente = clienteLocalRepository.saveAndFlush(cliente);
        LocalDateTime inicio = LocalDate.now().plusDays(60).atTime(10, 0);
        turno = new Turno(); turno.setFechaHoraInicio(inicio); turno.setFechaHoraFin(inicio.plusMinutes(30));
        turno.setStatus(TurnoStatus.CONFIRMED); turno.setPrecio(new BigDecimal("1000.00")); turno.setClienteLocal(cliente);
        turno.setNegocio(negocio); turno.setOwnerResponsable(owner); turno.setServicio(servicio);
        turno = turnoRepository.saveAndFlush(turno);
    }

    @Test void dosFinalizacionesMismoIdempotencyKeyMismoCuerpoCreanUnaSolaOperacion() throws Exception {
        var request = new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("900.00"), MetodoPago.EFECTIVO, null, null);
        var outcomes = concurrent(() -> ownerTurnosService.completarConDecision(negocio.getNegocioId(), turno.getTurnoId(), request, "same-key-" + turno.getTurnoId(), email));
        assertEquals(2, outcomes.length);
        assertEquals(1, resultadoRepository.findByTurno_TurnoId(turno.getTurnoId()).stream().count());
        assertEquals(1, pagoRepository.findByTurno_Negocio_NegocioIdOrderByFechaPagoDescPagoIdDesc(negocio.getNegocioId()).stream().filter(p -> turno.getTurnoId().equals(p.getTurno().getTurnoId())).count());
    }

    @Test void finalizacionesSimultaneasConClavesDistintasNoDuplicanResultado() throws Exception {
        var outcomes = concurrentTwo(() -> ownerTurnosService.completarConDecision(negocio.getNegocioId(), turno.getTurnoId(),
                        new DecisionFinancieraRequestDto(DecisionFinanciera.GRATUIDAD, null, null, null, "Promoción"), "different-a-" + turno.getTurnoId(), email),
                () -> ownerTurnosService.completarConDecision(negocio.getNegocioId(), turno.getTurnoId(),
                        new DecisionFinancieraRequestDto(DecisionFinanciera.GRATUIDAD, null, null, null, "Promoción"), "different-b-" + turno.getTurnoId(), email));
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("OK"::equals).count());
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("CONFLICT"::equals).count());
        assertTrue(resultadoRepository.existsByTurno_TurnoId(turno.getTurnoId()));
    }

    @Test void abonosSimultaneosNoSuperanElSaldo() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long deudaId = tx.execute(status -> crearDeuda());
        var request = new CrearAbonoRequestDto(new BigDecimal("600.00"), MetodoPago.EFECTIVO, null);
        var outcomes = concurrentTwo(() -> ownerDeudasService.abonar(negocio.getNegocioId(), deudaId, request, email),
                () -> ownerDeudasService.abonar(negocio.getNegocioId(), deudaId, request, email));
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("OK"::equals).count());
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("CONFLICT"::equals).count());
        assertEquals(new BigDecimal("600.00"), pagoRepository.sumarAbonos(deudaId));
    }

    @Test void estadisticasCalculanIngresosSaldoYRegularizacionSinDuplicar() {
        ownerTurnosService.completarConDecision(negocio.getNegocioId(), turno.getTurnoId(),
                new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("900.00"), MetodoPago.EFECTIVO, null, null),
                "stats-direct-" + turno.getTurnoId(), email);
        Turno turnoDeuda = crearTurnoAdicional(TurnoStatus.CONFIRMED);
        ownerTurnosService.completarConDecision(negocio.getNegocioId(), turnoDeuda.getTurnoId(),
                new DecisionFinancieraRequestDto(DecisionFinanciera.CUENTA_CORRIENTE, new BigDecimal("1000.00"), null, null, null),
                "stats-debt-" + turnoDeuda.getTurnoId(), email);
        DeudaCliente deuda = deudaRepository.findByNegocio_NegocioIdOrderByFechaOrigenDesc(negocio.getNegocioId()).get(0);
        ownerDeudasService.abonar(negocio.getNegocioId(), deuda.getDeudaId(), new CrearAbonoRequestDto(new BigDecimal("250.00"), MetodoPago.DEBITO, null), email);
        Turno turnoGratis = crearTurnoAdicional(TurnoStatus.CONFIRMED);
        ownerTurnosService.completarConDecision(negocio.getNegocioId(), turnoGratis.getTurnoId(),
                new DecisionFinancieraRequestDto(DecisionFinanciera.GRATUIDAD, null, null, null, "Promoción"),
                "stats-free-" + turnoGratis.getTurnoId(), email);
        crearTurnoAdicional(TurnoStatus.COMPLETED);

        var stats = ownerEstadisicaService.obtenerEstadisticasIngresos(negocio.getNegocioId(), null, null, email);
        assertEquals(new BigDecimal("900.00"), stats.ingresosCobrosDirectos());
        assertEquals(new BigDecimal("250.00"), stats.ingresosAbonos());
        assertEquals(new BigDecimal("1150.00"), stats.ingresosTotales());
        assertEquals(750L, stats.deudaPendienteTotal().longValue());
        assertEquals(1L, stats.deudasParciales());
        assertEquals(1L, stats.turnosCobrados());
        assertEquals(1L, stats.turnosGratuitos());
        assertEquals(1L, stats.turnosPendientesRegularizacion());
    }

    private Long crearDeuda() {
        ResultadoFinancieroTurno resultado = new ResultadoFinancieroTurno(); resultado.setTurno(turno); resultado.setNegocio(negocio);
        resultado.setClienteLocal(turno.getClienteLocal()); resultado.setOwner(owner); resultado.setTipoResultado(DecisionFinanciera.CUENTA_CORRIENTE);
        resultado.setMontoAcordado(new BigDecimal("1000.00")); resultado.setFechaRegistro(LocalDateTime.now());
        resultado = resultadoRepository.saveAndFlush(resultado);
        DeudaCliente deuda = new DeudaCliente(); deuda.setResultadoFinanciero(resultado); deuda.setNegocio(negocio);
        deuda.setClienteLocal(turno.getClienteLocal()); deuda.setMontoOriginal(new BigDecimal("1000.00")); deuda.setFechaOrigen(LocalDateTime.now());
        return deudaRepository.saveAndFlush(deuda).getDeudaId();
    }

    private Turno crearTurnoAdicional(TurnoStatus status) {
        Turno adicional = new Turno();
        LocalDateTime inicio = turno.getFechaHoraInicio().plusHours(SEQUENCE.get());
        adicional.setFechaHoraInicio(inicio); adicional.setFechaHoraFin(inicio.plusMinutes(30));
        adicional.setStatus(status); adicional.setPrecio(turno.getPrecio()); adicional.setClienteLocal(turno.getClienteLocal());
        adicional.setNegocio(negocio); adicional.setOwnerResponsable(owner); adicional.setServicio(turno.getServicio());
        return turnoRepository.saveAndFlush(adicional);
    }

    private Object[] concurrent(CheckedOperation operation) throws Exception {
        return concurrentTwo(operation, operation);
    }

    private Object[] concurrentTwo(CheckedOperation first, CheckedOperation second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> a = pool.submit(() -> invoke(first, ready, start));
            Future<String> b = pool.submit(() -> invoke(second, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return new Object[]{a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)};
        } finally { pool.shutdownNow(); }
    }

    private String invoke(CheckedOperation operation, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
        try { operation.run(); return "OK"; }
        catch (ResponseStatusException conflict) { return conflict.getStatusCode().is4xxClientError() ? "CONFLICT" : "ERROR"; }
    }

    @FunctionalInterface private interface CheckedOperation { Object run() throws Exception; }
}
