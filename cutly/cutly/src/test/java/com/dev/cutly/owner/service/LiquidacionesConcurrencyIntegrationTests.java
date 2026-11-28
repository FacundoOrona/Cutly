package com.dev.cutly.owner.service;

import com.dev.cutly.auth.config.SuperAdminInitializer;
import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.liquidacion.repository.AplicacionLiquidacionRepository;
import com.dev.cutly.liquidacion.repository.LiquidacionEmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.liquidacion.CrearLiquidacionEmpleadoRequestDto;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import com.dev.cutly.remuneracion.repository.RemuneracionEmpleadoRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cutly_liquidaciones_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "spring.flyway.enabled=false",
        "security.jwt.secret=liquidaciones-concurrency-test-secret-32-bytes",
        "app.bootstrap-super-admin.nombre=Test", "app.bootstrap-super-admin.apellido=Admin",
        "app.bootstrap-super-admin.dni=99999995", "app.bootstrap-super-admin.email=liquidaciones-admin@test.local",
        "app.bootstrap-super-admin.password=test-password-only"
})
class LiquidacionesConcurrencyIntegrationTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    @MockitoBean SuperAdminInitializer superAdminInitializer;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired NegocioRepository negocioRepository;
    @Autowired EmpleadoRepository empleadoRepository;
    @Autowired ClienteLocalRepository clienteRepository;
    @Autowired ServicioRepository servicioRepository;
    @Autowired TurnoRepository turnoRepository;
    @Autowired ResultadoFinancieroTurnoRepository resultadoRepository;
    @Autowired RemuneracionEmpleadoRepository remuneracionRepository;
    @Autowired LiquidacionEmpleadoRepository liquidacionRepository;
    @Autowired AplicacionLiquidacionRepository aplicacionRepository;
    @Autowired OwnerLiquidacionesService service;
    @Autowired PlatformTransactionManager transactionManager;
    private Usuario owner;
    private Negocio negocio;
    private RemuneracionEmpleado remuneracion;
    private String email;

    @BeforeEach void setup() {
        int n = SEQUENCE.incrementAndGet();
        email = "liquidacion-owner-" + n + "@test.local";
        owner = usuario("Owner", "Test", 82000000 + n, email, Rol.OWNER);
        owner = usuarioRepository.saveAndFlush(owner);
        negocio = new Negocio(); negocio.setNombre("Liquidaciones " + n); negocio.setStatus(NegocioStatus.ACTIVE); negocio.setOwner(owner);
        negocio = negocioRepository.saveAndFlush(negocio);
        Usuario empleadoUsuario = usuario("Empleado", "Test", 83000000 + n, "empleado-" + n + "@test.local", Rol.EMPLOYEE);
        empleadoUsuario = usuarioRepository.saveAndFlush(empleadoUsuario);
        Empleado empleado = new Empleado(); empleado.setUsuario(empleadoUsuario); empleado.setNegocio(negocio); empleado.setActivo(false);
        empleado = empleadoRepository.saveAndFlush(empleado);
        ClienteLocal cliente = new ClienteLocal(); cliente.setNegocio(negocio); cliente.setNombre("Cliente"); cliente.setApellido("Local");
        cliente.setDniPresentado("1234567"); cliente.setDniNormalizado("1234567"); cliente.setTelefono("5555");
        cliente = clienteRepository.saveAndFlush(cliente);
        Servicio servicio = new Servicio(); servicio.setNegocio(negocio); servicio.setNombre("Corte"); servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDuracionMinutos(30); servicio.setActivo(true); servicio = servicioRepository.saveAndFlush(servicio);
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        Turno turno = new Turno(); turno.setFechaHoraInicio(inicio); turno.setFechaHoraFin(inicio.plusMinutes(30));
        turno.setStatus(TurnoStatus.COMPLETED); turno.setPrecio(new BigDecimal("200.00")); turno.setClienteLocal(cliente);
        turno.setEmpleado(empleado); turno.setNegocio(negocio); turno.setServicio(servicio); turno = turnoRepository.saveAndFlush(turno);
        ResultadoFinancieroTurno resultado = new ResultadoFinancieroTurno(); resultado.setTurno(turno); resultado.setNegocio(negocio);
        resultado.setClienteLocal(cliente); resultado.setOwner(owner); resultado.setTipoResultado(DecisionFinanciera.COBRO_DIRECTO);
        resultado.setMontoAcordado(new BigDecimal("100.00")); resultado.setFechaRegistro(LocalDateTime.now());
        resultado = resultadoRepository.saveAndFlush(resultado);
        remuneracion = RemuneracionEmpleado.pendiente(turno, LocalDateTime.now());
        remuneracion.confirmar(resultado, new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("100.00"), LocalDateTime.now());
        remuneracion = remuneracionRepository.saveAndFlush(remuneracion);
    }

    @Test void dosCreacionesSimultaneasNoSuperanElSaldo() throws Exception {
        var outcomes = concurrentTwo(() -> crear("balance-a", "60.00"), () -> crear("balance-b", "60.00"));
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("OK"::equals).count());
        assertEquals(1, java.util.Arrays.stream(outcomes).filter("CONFLICT"::equals).count());
        assertEquals(new BigDecimal("60.00"), aplicacionRepository.totalVigente(remuneracion.getRemuneracionId()));
    }

    @Test void mismaClaveConcurrentePersisteUnaLiquidacionYDevuelveLaExistente() throws Exception {
        var outcomes = concurrentTwo(() -> crear("same-key", "40.00"), () -> crear("same-key", "40.00"));
        assertArrayEquals(new String[]{"OK", "OK"}, outcomes);
        assertEquals(1, liquidacionRepository.countByNegocio_NegocioId(negocio.getNegocioId()));
        assertEquals(new BigDecimal("40.00"), aplicacionRepository.totalVigente(remuneracion.getRemuneracionId()));
    }

    @Test void anulacionConcurrenteConNuevaLiquidacionConservaHistorialYSaldos() throws Exception {
        var inicial = service.crear(negocio.getNegocioId(), request("original", "40.00"), "original", email);
        var outcomes = concurrentTwo(
                () -> { service.anular(negocio.getNegocioId(), inicial.liquidacionId(), "Carga incorrecta", email); return "OK"; },
                () -> crear("after-void", "60.00"));
        assertEquals(2, liquidacionRepository.countByNegocio_NegocioId(negocio.getNegocioId()));
        assertEquals(2, aplicacionRepository.countByLiquidacion_Negocio_NegocioId(negocio.getNegocioId()));
        BigDecimal vigente = aplicacionRepository.totalVigente(remuneracion.getRemuneracionId());
        assertTrue(vigente.compareTo(new BigDecimal("100.00")) <= 0);
        assertTrue(java.util.Arrays.stream(outcomes).allMatch("OK"::equals));
    }

    @Test void fallaSiTotalPersistidoNoCoincideConAplicaciones() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(RuntimeException.class, () -> tx.execute(status -> {
            var liquidacion = new com.dev.cutly.liquidacion.entity.LiquidacionEmpleado(negocio, remuneracion.getEmpleado(), owner,
                    LocalDate.now(), MetodoPago.EFECTIVO, null, null, new BigDecimal("39.00"), "mismatch", "hash");
            liquidacion.agregar(new com.dev.cutly.liquidacion.entity.AplicacionLiquidacion(liquidacion, remuneracion, new BigDecimal("40.00")));
            liquidacionRepository.saveAndFlush(liquidacion);
            return null;
        }));
        assertEquals(0, liquidacionRepository.countByNegocio_NegocioId(negocio.getNegocioId()));
        assertEquals(0, aplicacionRepository.countByLiquidacion_Negocio_NegocioId(negocio.getNegocioId()));
    }

    @Test void rollbackEvitaLiquidacionParcialSiFallaUnaAplicacion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(RuntimeException.class, () -> tx.execute(status -> {
            var liquidacion = new com.dev.cutly.liquidacion.entity.LiquidacionEmpleado(negocio, remuneracion.getEmpleado(), owner,
                    LocalDate.now(), MetodoPago.EFECTIVO, null, null, new BigDecimal("-1.00"), "child-failure", "hash");
            liquidacion.agregar(new com.dev.cutly.liquidacion.entity.AplicacionLiquidacion(liquidacion, remuneracion, new BigDecimal("-1.00")));
            liquidacionRepository.saveAndFlush(liquidacion);
            return null;
        }));
        assertEquals(0, liquidacionRepository.countByNegocio_NegocioId(negocio.getNegocioId()));
        assertEquals(0, aplicacionRepository.countByLiquidacion_Negocio_NegocioId(negocio.getNegocioId()));
    }

    @Test void rollbackEvitaAplicacionesSiFallaLaLiquidacion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(RuntimeException.class, () -> tx.execute(status -> {
            var liquidacion = new com.dev.cutly.liquidacion.entity.LiquidacionEmpleado(negocio, remuneracion.getEmpleado(), owner,
                    LocalDate.now(), MetodoPago.EFECTIVO, null, null, new BigDecimal("40.00"), "k".repeat(101), "hash");
            liquidacion.agregar(new com.dev.cutly.liquidacion.entity.AplicacionLiquidacion(liquidacion, remuneracion, new BigDecimal("40.00")));
            liquidacionRepository.saveAndFlush(liquidacion);
            return null;
        }));
        assertEquals(0, liquidacionRepository.countByNegocio_NegocioId(negocio.getNegocioId()));
        assertEquals(0, aplicacionRepository.countByLiquidacion_Negocio_NegocioId(negocio.getNegocioId()));
    }

    @Test void resumenYHistorialConservanLosTotales() {
        service.crear(negocio.getNegocioId(), request("summary", "40.00"), "summary", email);
        @SuppressWarnings("unchecked") Map<String,Object> row = (Map<String,Object>) service.resumen(negocio.getNegocioId(), email).get(0);
        assertEquals(new BigDecimal("100.00"), row.get("totalConfirmado"));
        assertEquals(new BigDecimal("40.00"), row.get("totalPagado"));
        assertEquals(new BigDecimal("60.00"), row.get("totalPendiente"));
        var page = service.historial(negocio.getNegocioId(), null, 0, 20, email);
        assertEquals(1, page.getTotalElements());
        assertEquals(new BigDecimal("40.00"), page.getContent().get(0).importeTotal());
    }

    @Test void resumenSumaAplicacionesVigentesYExcluyeLiquidacionAnuladaDeEmpleadoInactivo() {
        var primera = service.crear(negocio.getNegocioId(), request("summary-first", "20.00"), "summary-first", email);
        service.crear(negocio.getNegocioId(), request("summary-second", "30.00"), "summary-second", email);

        @SuppressWarnings("unchecked") Map<String,Object> before = (Map<String,Object>) service.resumen(negocio.getNegocioId(), email).get(0);
        assertEquals(new BigDecimal("50.00"), before.get("totalPagado"));
        assertEquals(new BigDecimal("50.00"), before.get("totalPendiente"));
        assertEquals(1, before.get("parcialmentePagadas"));

        service.anular(negocio.getNegocioId(), primera.liquidacionId(), "Corrección de prueba", email);

        @SuppressWarnings("unchecked") Map<String,Object> after = (Map<String,Object>) service.resumen(negocio.getNegocioId(), email).get(0);
        assertEquals(new BigDecimal("30.00"), after.get("totalPagado"));
        assertEquals(new BigDecimal("70.00"), after.get("totalPendiente"));
        assertEquals(1, after.get("parcialmentePagadas"));
    }

    @Test void ownerAjenoYIdsDeOtroNegocioNoAccedenALosDatos() {
        Usuario otroOwner = usuario("Otro", "Owner", 84000001, "otro-owner@test.local", Rol.OWNER);
        otroOwner = usuarioRepository.saveAndFlush(otroOwner);
        Negocio otroNegocio = new Negocio(); otroNegocio.setNombre("Otro negocio"); otroNegocio.setStatus(NegocioStatus.ACTIVE); otroNegocio.setOwner(otroOwner);
        negocioRepository.saveAndFlush(otroNegocio);
        String otroEmail = otroOwner.getEmail();
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.historial(negocio.getNegocioId(), null, 0, 20, otroEmail)).getStatusCode().value());
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.detalleRemuneracion(negocio.getNegocioId() + 1000, remuneracion.getRemuneracionId(), email)).getStatusCode().value());
    }

    @Test void rollbackDeAnulacionConservaEstadoVigente() {
        var creada = service.crear(negocio.getNegocioId(), request("void-rollback", "40.00"), "void-rollback", email);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            var liquidacion = liquidacionRepository.findByLiquidacionIdAndNegocio_NegocioId(creada.liquidacionId(), negocio.getNegocioId()).orElseThrow();
            liquidacion.anular("Falla simulada", owner);
            liquidacionRepository.flush();
            throw new IllegalStateException("rollback");
        }));
        assertEquals(com.dev.cutly.liquidacion.enums.EstadoLiquidacion.VIGENTE,
                liquidacionRepository.findByLiquidacionIdAndNegocio_NegocioId(creada.liquidacionId(), negocio.getNegocioId()).orElseThrow().getEstado());
    }

    private String crear(String key, String amount) {
        service.crear(negocio.getNegocioId(), request(key, amount), key, email);
        return "OK";
    }
    private CrearLiquidacionEmpleadoRequestDto request(String key, String amount) {
        return new CrearLiquidacionEmpleadoRequestDto(LocalDate.now(), MetodoPago.EFECTIVO, null, key,
                Map.of(remuneracion.getRemuneracionId(), new BigDecimal(amount)));
    }
    private String[] concurrentTwo(CheckedOperation first, CheckedOperation second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> a = executor.submit(() -> run(first, ready, start));
            Future<String> b = executor.submit(() -> run(second, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            return new String[]{a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)};
        } finally { executor.shutdownNow(); }
    }
    private String run(CheckedOperation operation, CountDownLatch ready, CountDownLatch start) {
        try { ready.countDown(); start.await(); operation.run(); return "OK"; }
        catch (org.springframework.web.server.ResponseStatusException e) { return "CONFLICT"; }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private Usuario usuario(String nombre, String apellido, int dni, String correo, Rol rol) {
        Usuario u = new Usuario(); u.setNombre(nombre); u.setApellido(apellido); u.setDni(Integer.toString(dni)); u.setEmail(correo);
        u.setContrasena("test-hash"); u.setRol(rol); u.setStatus(UsuarioStatus.ACTIVE); return u;
    }
    @FunctionalInterface private interface CheckedOperation { Object run() throws Exception; }
}
