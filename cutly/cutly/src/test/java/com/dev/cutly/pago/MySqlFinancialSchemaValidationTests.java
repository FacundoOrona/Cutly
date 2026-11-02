package com.dev.cutly.pago;

import com.dev.cutly.pago.repository.DeudaClienteRepository;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import com.dev.cutly.pago.repository.IdempotenciaFinancieraRepository;
import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.pago.entity.DeudaCliente;
import com.dev.cutly.pago.entity.IdempotenciaFinanciera;
import com.dev.cutly.pago.entity.Pago;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfSystemProperty(named = "cutly.mysql.validation", matches = "true")
class MySqlFinancialSchemaValidationTests {
    @DynamicPropertySource
    static void mysqlTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySqlFinancialSchemaValidationTests::testDatabaseUrl);
    }

    private static String testDatabaseUrl() {
        String url = System.getProperty("cutly.mysql.validation.url");
        if (url == null || !url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):3306/cutly_test(?:\\?.*)?")) {
            throw new IllegalStateException("La validación MySQL requiere una URL localhost:3306/cutly_test");
        }
        return url;
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PagoRepository pagoRepository;
    @Autowired DeudaClienteRepository deudaRepository;
    @Autowired ResultadoFinancieroTurnoRepository resultadoRepository;
    @Autowired TurnoRepository turnoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired NegocioRepository negocioRepository;
    @Autowired ServicioRepository servicioRepository;
    @Autowired ClienteLocalRepository clienteLocalRepository;
    @Autowired IdempotenciaFinancieraRepository idempotenciaRepository;

    @Test
    void mysqlCreaRestriccionesRelacionesYValidaConsultasFinancieras() {
        for (String table : new String[]{"audit_logs", "clientes_locales", "deudas_cliente", "direcciones_negocio",
                "empleados", "empleados_servicios", "favoritos", "horarios_negocio", "idempotencia_financiera",
                "negocios", "notificaciones", "pagos", "planes_suscripcion", "resultados_financieros_turno",
                "reviews", "servicios", "suscripciones", "tokens_invalidos", "turnos", "usuarios"}) {
            assertTrue(tableExists(table), "Falta tabla mapeada: " + table);
        }

        assertTrue(constraintCount("pagos", "CHECK") >= 1);
        assertTrue(constraintCount("deudas_cliente", "CHECK") >= 2);
        assertTrue(constraintCount("resultados_financieros_turno", "CHECK") >= 3);
        assertTrue(constraintCount("pagos", "UNIQUE") >= 2);
        assertTrue(constraintCount("deudas_cliente", "UNIQUE") >= 1);
        assertTrue(constraintCount("resultados_financieros_turno", "UNIQUE") >= 1);
        assertTrue(constraintCount("idempotencia_financiera", "UNIQUE") >= 3);

        assertTrue(foreignKeyCount("pagos") >= 4);
        assertTrue(foreignKeyCount("deudas_cliente") >= 4);
        assertTrue(foreignKeyCount("resultados_financieros_turno") >= 5);
        assertTrue(foreignKeyCount("idempotencia_financiera") >= 3);

        assertEquals(0, pagoRepository.sumarCobrosDirectos(-1L, null, null).compareTo(BigDecimal.ZERO));
        assertEquals(0, pagoRepository.sumarAbonosPorNegocioYPeriodo(-1L, null, null).compareTo(BigDecimal.ZERO));
        assertEquals(0, deudaRepository.sumarSaldoPendiente(-1L, null, null).compareTo(BigDecimal.ZERO));
        assertEquals(0L, deudaRepository.contarPendientes(-1L, null, null));
        assertEquals(0L, deudaRepository.contarParciales(-1L, null, null));
        assertEquals(0L, deudaRepository.contarPagadas(-1L, null, null));
        assertEquals(0L, resultadoRepository.contarTurnosCobrados(-1L, null, null));
        assertEquals(0L, resultadoRepository.contarTurnosGratuitos(-1L, null, null));
        assertEquals(0L, turnoRepository.contarCompletadosSinRegularizacion(-1L, null, null));

        verifyDirectPaymentAndDebtAbonosOnMySql();
    }

    private void verifyDirectPaymentAndDebtAbonosOnMySql() {
        Usuario owner = new Usuario(); owner.setNombre("MySQL"); owner.setApellido("SchemaTest"); owner.setDni("81234567");
        owner.setEmail("mysql-schema-test@cutly.test"); owner.setContrasena("test-only-hash"); owner.setRol(Rol.OWNER); owner.setStatus(UsuarioStatus.ACTIVE);
        owner = usuarioRepository.saveAndFlush(owner);
        Negocio negocio = new Negocio(); negocio.setNombre("MySQL schema test"); negocio.setStatus(NegocioStatus.ACTIVE); negocio.setOwner(owner);
        negocio = negocioRepository.saveAndFlush(negocio);
        Servicio servicio = new Servicio(); servicio.setNombre("Test service"); servicio.setPrecio(new BigDecimal("1000.00"));
        servicio.setDuracionMinutos(30); servicio.setActivo(true); servicio.setNegocio(negocio);
        servicio = servicioRepository.saveAndFlush(servicio);
        ClienteLocal local = new ClienteLocal(); local.setNegocio(negocio); local.setNombre("Test"); local.setApellido("Client");
        local.setDniPresentado("12345678"); local.setDniNormalizado("12345678"); local.setTelefono("5555");
        local = clienteLocalRepository.saveAndFlush(local);

        LocalDateTime start = LocalDateTime.now().plusDays(10);
        Turno directTurn = saveTurn(negocio, local, servicio, owner, start);
        ResultadoFinancieroTurno direct = new ResultadoFinancieroTurno();
        direct.setTurno(directTurn); direct.setNegocio(negocio); direct.setClienteLocal(local); direct.setOwner(owner);
        direct.setTipoResultado(DecisionFinanciera.COBRO_DIRECTO); direct.setMontoAcordado(new BigDecimal("75.00")); direct.setFechaRegistro(LocalDateTime.now());
        direct = resultadoRepository.saveAndFlush(direct);
        Pago payment = new Pago(); payment.setTurno(directTurn); payment.setResultadoFinanciero(direct); payment.setOwnerRegistrante(owner);
        payment.setMonto(new BigDecimal("75.00")); payment.setMetodoPago(MetodoPago.EFECTIVO); payment.setFechaPago(LocalDateTime.now());
        pagoRepository.saveAndFlush(payment);

        Turno debtTurn = saveTurn(negocio, local, servicio, owner, start.plusHours(1));
        ResultadoFinancieroTurno account = new ResultadoFinancieroTurno();
        account.setTurno(debtTurn); account.setNegocio(negocio); account.setClienteLocal(local); account.setOwner(owner);
        account.setTipoResultado(DecisionFinanciera.CUENTA_CORRIENTE); account.setMontoAcordado(new BigDecimal("1000.00")); account.setFechaRegistro(LocalDateTime.now());
        account = resultadoRepository.saveAndFlush(account);
        DeudaCliente debt = new DeudaCliente(); debt.setResultadoFinanciero(account); debt.setNegocio(negocio); debt.setClienteLocal(local);
        debt.setMontoOriginal(new BigDecimal("1000.00")); debt.setFechaOrigen(LocalDateTime.now());
        debt = deudaRepository.saveAndFlush(debt);
        savePayment(debt, owner, "10.00");
        savePayment(debt, owner, "15.00");

        Turno freeTurn = saveTurn(negocio, local, servicio, owner, start.plusHours(2));
        ResultadoFinancieroTurno free = new ResultadoFinancieroTurno(); free.setTurno(freeTurn); free.setNegocio(negocio);
        free.setClienteLocal(local); free.setOwner(owner); free.setTipoResultado(DecisionFinanciera.GRATUIDAD);
        free.setMontoAcordado(BigDecimal.ZERO); free.setMotivoGratuidad("Promoción"); free.setFechaRegistro(LocalDateTime.now());
        resultadoRepository.saveAndFlush(free);

        assertEquals(0, pagoRepository.sumarAbonos(debt.getDeudaId()).compareTo(new BigDecimal("25.00")));
        assertEquals(0, pagoRepository.sumarCobrosDirectos(negocio.getNegocioId(), null, null).compareTo(new BigDecimal("75.00")));
        assertEquals(0, pagoRepository.sumarAbonosPorNegocioYPeriodo(negocio.getNegocioId(), null, null).compareTo(new BigDecimal("25.00")));
        assertEquals(1L, pagoRepository.contarCobrosDirectos(negocio.getNegocioId(), null, null));
        assertEquals(2L, pagoRepository.contarAbonosPorNegocioYPeriodo(negocio.getNegocioId(), null, null));
        assertEquals(0, deudaRepository.sumarSaldoPendiente(negocio.getNegocioId(), null, null).compareTo(new BigDecimal("975.00")));
        assertEquals(0L, deudaRepository.contarPendientes(negocio.getNegocioId(), null, null));
        assertEquals(1L, deudaRepository.contarParciales(negocio.getNegocioId(), null, null));
        assertEquals(0L, deudaRepository.contarPagadas(negocio.getNegocioId(), null, null));
        assertEquals(1L, resultadoRepository.contarTurnosCobrados(negocio.getNegocioId(), null, null));
        assertEquals(1L, resultadoRepository.contarTurnosGratuitos(negocio.getNegocioId(), null, null));
        assertEquals(0L, turnoRepository.contarCompletadosSinRegularizacion(negocio.getNegocioId(), null, null));

        IdempotenciaFinanciera idem = new IdempotenciaFinanciera(); idem.setKey("mysql-schema-key");
        idem.setRequestHash("0123456789012345678901234567890123456789012345678901234567890123");
        idem.setNegocio(negocio); idem.setTurno(directTurn); idem.setResultado(direct);
        idempotenciaRepository.saveAndFlush(idem);
    }

    private Turno saveTurn(Negocio negocio, ClienteLocal local, Servicio servicio, Usuario owner, LocalDateTime start) {
        Turno turn = new Turno(); turn.setFechaHoraInicio(start); turn.setFechaHoraFin(start.plusMinutes(30));
        turn.setStatus(TurnoStatus.COMPLETED); turn.setPrecio(new BigDecimal("1000.00")); turn.setClienteLocal(local);
        turn.setNegocio(negocio); turn.setOwnerResponsable(owner); turn.setServicio(servicio);
        return turnoRepository.saveAndFlush(turn);
    }

    private void savePayment(DeudaCliente debt, Usuario owner, String amount) {
        Pago payment = new Pago(); payment.setDeuda(debt); payment.setOwnerRegistrante(owner);
        payment.setMonto(new BigDecimal(amount)); payment.setMetodoPago(MetodoPago.TRANSFERENCIA_QR); payment.setFechaPago(LocalDateTime.now());
        pagoRepository.saveAndFlush(payment);
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select count(*) > 0 from information_schema.tables where table_schema=database() and table_name=?",
                Boolean.class, tableName));
    }

    private int constraintCount(String tableName, String type) {
        return jdbc.queryForObject("select count(*) from information_schema.table_constraints where constraint_schema=database() and table_name=? and constraint_type=?", Integer.class, tableName, type);
    }

    private int foreignKeyCount(String tableName) {
        return jdbc.queryForObject("select count(*) from information_schema.key_column_usage where table_schema=database() and table_name=? and referenced_table_name is not null", Integer.class, tableName);
    }
}
