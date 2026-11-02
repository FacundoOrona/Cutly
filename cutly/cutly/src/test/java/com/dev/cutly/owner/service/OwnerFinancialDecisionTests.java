package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.owner.dto.turno.DecisionFinancieraRequestDto;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.pago.entity.*;
import com.dev.cutly.pago.enums.*;
import com.dev.cutly.pago.repository.*;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerFinancialDecisionTests {
    @Mock TurnoRepository turnoRepository;
    @Mock OwnerNegocioService ownerNegocioService;
    @Mock ClienteLocalRepository clienteLocalRepository;
    @Mock OwnerClienteLocalService ownerClienteLocalService;
    @Mock ServicioRepository servicioRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock EmpleadoServiciorRepository empleadoServicioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock TurnoHorarioValidator horarioValidator;
    @Mock ResultadoFinancieroTurnoRepository resultadoRepository;
    @Mock DeudaClienteRepository deudaRepository;
    @Mock PagoRepository pagoRepository;
    @Mock IdempotenciaFinancieraRepository idempotenciaRepository;
    OwnerTurnosService service;
    Negocio negocio;
    Turno turno;

    @BeforeEach void setup() {
        service = new OwnerTurnosService(turnoRepository, ownerNegocioService, clienteLocalRepository, ownerClienteLocalService,
                servicioRepository, empleadoRepository, empleadoServicioRepository, usuarioRepository, horarioValidator,
                resultadoRepository, deudaRepository, pagoRepository, idempotenciaRepository);
        Usuario owner = new Usuario(); owner.setUsuarioId(7L); owner.setNombre("Owner"); owner.setApellido("Test");
        negocio = new Negocio(); negocio.setNegocioId(9L); negocio.setOwner(owner); negocio.setNombre("Negocio"); negocio.setStatus(NegocioStatus.ACTIVE);
        ClienteLocal cliente = new ClienteLocal(); cliente.setClienteLocalId(11L); cliente.setNombre("Cliente"); cliente.setApellido("Local");
        Servicio servicio = new Servicio(); servicio.setServicioId(13L); servicio.setNombre("Corte");
        turno = new Turno(); turno.setTurnoId(17L); turno.setNegocio(negocio); turno.setClienteLocal(cliente); turno.setServicio(servicio); turno.setOwnerResponsable(owner); turno.setStatus(TurnoStatus.CONFIRMED);
        lenient().when(ownerNegocioService.obtenerEntidadNegocioPropio(9L, "owner@test.local")).thenReturn(negocio);
        lenient().when(turnoRepository.findWithLockingByTurnoIdAndNegocioId(17L, 9L)).thenReturn(Optional.of(turno));
        lenient().when(resultadoRepository.existsByTurno_TurnoId(17L)).thenReturn(false);
        lenient().when(resultadoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(deudaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(turnoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(idempotenciaRepository.findByKey("key-1")).thenReturn(Optional.empty());
    }

    @Test void cobraDirectamenteYCompletaElTurnoAtomico() {
        service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("1200"), null, MetodoPago.EFECTIVO, null, null), "key-1", "owner@test.local");
        verify(pagoRepository).save(argThat(p -> p.getMonto().compareTo(new BigDecimal("1200")) == 0 && p.getTurno() == turno && p.getResultadoFinanciero() != null));
        verify(idempotenciaRepository).saveAndFlush(any(IdempotenciaFinanciera.class));
        assertEquals(TurnoStatus.COMPLETED, turno.getStatus());
    }

    @Test void otroConDescripcionValidaEsAceptadoAlRegularizar() {
        turno.setStatus(TurnoStatus.COMPLETED);
        service.regularizarFinanzas(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO,
                new BigDecimal("1200"), null, MetodoPago.OTRO, "Transferencia externa", null), "key-1", "owner@test.local");
        verify(resultadoRepository).save(any(ResultadoFinancieroTurno.class));
        verify(pagoRepository).save(argThat(p -> "Transferencia externa".equals(p.getDescripcionOtro())));
    }

    @Test void otroSinDescripcionSeRechazaAntesDeGuardar() {
        assertOtroDescripcionInvalida(null);
    }

    @Test void otroConDescripcionVaciaSeRechazaAntesDeGuardar() {
        assertOtroDescripcionInvalida("");
    }

    @Test void otroConDescripcionDeSoloEspaciosSeRechazaAntesDeGuardar() {
        assertOtroDescripcionInvalida("   ");
    }

    @Test void otroConDescripcionMayorA100CaracteresSeRechazaAntesDeGuardar() {
        assertOtroDescripcionInvalida("x".repeat(101));
    }

    @Test void otroMetodoConDescripcionSeRechazaAntesDeGuardar() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L,
                new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("1200"),
                        null, MetodoPago.EFECTIVO, "No corresponde", null), "key-1", "owner@test.local"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(resultadoRepository, pagoRepository);
    }

    private void assertOtroDescripcionInvalida(String descripcion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L,
                new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("1200"),
                        null, MetodoPago.OTRO, descripcion, null), "key-1", "owner@test.local"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(resultadoRepository, pagoRepository);
    }

    @Test void creaDeudaSinRegistrarIngreso() {
        service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.CUENTA_CORRIENTE, new BigDecimal("1200"), BigDecimal.ZERO, null, null, null), "key-1", "owner@test.local");
        verify(deudaRepository).save(argThat(d -> d.getMontoOriginal().compareTo(new BigDecimal("1200")) == 0));
        verifyNoInteractions(pagoRepository);
    }

    @Test void gratuidadGuardaMotivoSinPagoNiDeuda() {
        service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.GRATUIDAD, null, null, null, null, "Promoción"), "key-1", "owner@test.local");
        verify(resultadoRepository).save(argThat(r -> r.getMontoAcordado().signum() == 0 && "Promoción".equals(r.getMotivoGratuidad())));
        verifyNoInteractions(pagoRepository, deudaRepository);
    }

    @Test void gratuidadSinMotivoSeRechazaAntesDeGuardar() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.GRATUIDAD, null, null, null, null, "  "), "key-1", "owner@test.local"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(resultadoRepository, never()).save(any());
    }

    @Test void claveIgualConCuerpoDistintoSeRechaza() throws Exception {
        String body = "COBRO_DIRECTO|100|null|EFECTIVO|null|null";
        String hash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        IdempotenciaFinanciera existing = new IdempotenciaFinanciera(); existing.setNegocio(negocio); existing.setTurno(turno); existing.setRequestHash(hash);
        when(idempotenciaRepository.findByKey("key-1")).thenReturn(Optional.of(existing));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("200"), null, MetodoPago.EFECTIVO, null, null), "key-1", "owner@test.local"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(resultadoRepository, never()).save(any());
    }

    @Test void claveIgualConMismoCuerpoDevuelveResultadoSinDuplicar() throws Exception {
        String body = "COBRO_DIRECTO|1200|null|EFECTIVO|null|null";
        String hash = java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        IdempotenciaFinanciera existing = new IdempotenciaFinanciera(); existing.setNegocio(negocio); existing.setTurno(turno); existing.setRequestHash(hash);
        when(idempotenciaRepository.findByKey("key-1")).thenReturn(Optional.of(existing));
        service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("1200"), null, MetodoPago.EFECTIVO, null, null), "key-1", "owner@test.local");
        verify(resultadoRepository, never()).save(any());
        verifyNoInteractions(pagoRepository, deudaRepository);
    }

    @Test void cuentaCorrienteRegistraPagoInicialComoAbonoYConservaDeudaTotal() {
        var request = new DecisionFinancieraRequestDto(DecisionFinanciera.CUENTA_CORRIENTE,
                new BigDecimal("20000"), new BigDecimal("7000"), MetodoPago.EFECTIVO, null, null);
        service.completarConDecision(9L, 17L, request, "key-1", "owner@test.local");
        verify(deudaRepository).save(argThat(d -> d.getMontoOriginal().compareTo(new BigDecimal("20000")) == 0));
        verify(pagoRepository).save(argThat(p -> p.getDeuda() != null
                && p.getMonto().compareTo(new BigDecimal("7000")) == 0
                && p.getMetodoPago() == MetodoPago.EFECTIVO));
    }

    @Test void cuentaCorrientePagoCompletoMantieneDeudaYAbonoCompleto() {
        service.completarConDecision(9L, 17L, new DecisionFinancieraRequestDto(DecisionFinanciera.CUENTA_CORRIENTE,
                new BigDecimal("20000"), new BigDecimal("20000"), MetodoPago.DEBITO, null, null), "key-1", "owner@test.local");
        verify(deudaRepository).save(argThat(d -> d.getMontoOriginal().compareTo(new BigDecimal("20000")) == 0));
        verify(pagoRepository).save(argThat(p -> p.getMonto().compareTo(new BigDecimal("20000")) == 0));
    }

    @Test void pagoInicialMayorAlTotalSeRechaza() {
        assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L,
                new DecisionFinancieraRequestDto(DecisionFinanciera.CUENTA_CORRIENTE, new BigDecimal("100"),
                        new BigDecimal("101"), MetodoPago.EFECTIVO, null, null), "key-1", "owner@test.local"));
        verifyNoInteractions(resultadoRepository, deudaRepository, pagoRepository);
    }

    @Test void ownerAjenoNoPuedeFinalizarElTurnoDelNegocio() {
        when(ownerNegocioService.obtenerEntidadNegocioPropio(9L, "intruso@test.local"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> service.completarConDecision(9L, 17L,
                new DecisionFinancieraRequestDto(DecisionFinanciera.COBRO_DIRECTO, new BigDecimal("1200"), null, MetodoPago.EFECTIVO, null, null),
                "otra-key", "intruso@test.local"));
        verify(turnoRepository, never()).findWithLockingByTurnoIdAndNegocioId(any(), any());
    }
}
