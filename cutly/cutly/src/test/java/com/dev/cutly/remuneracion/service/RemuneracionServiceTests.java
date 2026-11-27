package com.dev.cutly.remuneracion.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.turno.CorregirPrecioHistoricoRequestDto;
import com.dev.cutly.owner.dto.turno.ResolverComisionRemuneracionRequestDto;
import com.dev.cutly.pago.entity.ResultadoFinancieroTurno;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.remuneracion.entity.RemuneracionEmpleado;
import com.dev.cutly.remuneracion.enums.EstadoRemuneracion;
import com.dev.cutly.remuneracion.repository.RemuneracionEmpleadoRepository;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemuneracionServiceTests {
    @Mock RemuneracionEmpleadoRepository remuneracionRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock EmpleadoServiciorRepository empleadoServicioRepository;

    private RemuneracionService service;
    private Turno turno;
    private Empleado empleado;
    private EmpleadoServicio asignacion;
    private ResultadoFinancieroTurno resultado;

    @BeforeEach
    void setUp() {
        service = new RemuneracionService(remuneracionRepository, empleadoRepository, empleadoServicioRepository);
        Usuario owner = new Usuario(); owner.setUsuarioId(1L);
        Negocio negocio = new Negocio(); negocio.setNegocioId(2L); negocio.setOwner(owner);
        Usuario usuarioEmpleado = new Usuario(); usuarioEmpleado.setUsuarioId(3L);
        empleado = new Empleado(); empleado.setEmpleadoId(4L); empleado.setUsuario(usuarioEmpleado);
        empleado.setNegocio(negocio); empleado.setPorcentajeRemuneracion(new BigDecimal("12.50"));
        Servicio servicio = new Servicio(); servicio.setServicioId(5L); servicio.setNegocio(negocio);
        turno = new Turno(); turno.setTurnoId(6L); turno.setNegocio(negocio); turno.setEmpleado(empleado);
        turno.setServicio(servicio); turno.setPrecio(new BigDecimal("111.11")); turno.setStatus(TurnoStatus.COMPLETED);
        asignacion = new EmpleadoServicio(); asignacion.setEmpleado(empleado); asignacion.setServicio(servicio);
        resultado = new ResultadoFinancieroTurno(); resultado.setTipoResultado(DecisionFinanciera.COBRO_DIRECTO);
        lenient().when(remuneracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(empleadoRepository.findWithLockingByEmpleadoId(4L)).thenReturn(Optional.of(empleado));
        lenient().when(empleadoServicioRepository.findWithLockingByEmpleadoAndServicio(4L, 5L))
                .thenReturn(Optional.of(asignacion));
    }

    @Test
    void usaComisionGeneralYRedondeaHalfUpConBigDecimal() {
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado remuneracion = capturada();
        assertEquals(EstadoRemuneracion.CONFIRMADA, remuneracion.getEstado());
        assertEquals(new BigDecimal("111.11"), remuneracion.getPrecioAplicado());
        assertEquals(new BigDecimal("12.50"), remuneracion.getPorcentajeAplicado());
        assertEquals(new BigDecimal("13.89"), remuneracion.getMontoCalculado());
        InOrder lockOrder = inOrder(empleadoRepository, empleadoServicioRepository);
        lockOrder.verify(empleadoRepository).findWithLockingByEmpleadoId(4L);
        lockOrder.verify(empleadoServicioRepository).findWithLockingByEmpleadoAndServicio(4L, 5L);
    }

    @Test
    void laExcepcionReemplazaAlPorcentajeGeneral() {
        empleado.setPorcentajeRemuneracion(new BigDecimal("80.00"));
        asignacion.setPorcentajeComision(new BigDecimal("25.25"));
        turno.setPrecio(new BigDecimal("1000.00"));
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado remuneracion = capturada();
        assertEquals(new BigDecimal("25.25"), remuneracion.getPorcentajeAplicado());
        assertEquals(new BigDecimal("252.50"), remuneracion.getMontoCalculado());
    }

    @Test
    void elCeroPorCientoConfirmaUnImporteCero() {
        empleado.setPorcentajeRemuneracion(BigDecimal.ZERO);
        turno.setPrecio(new BigDecimal("100.00"));
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        assertEquals(EstadoRemuneracion.CONFIRMADA, capturada().getEstado());
        assertEquals(new BigDecimal("0.00"), capturada().getMontoCalculado());
    }

    @Test
    void elPrecioHistoricoNuloQuedaPendienteDeRevisionYNoConsultaPrecioDeServicio() {
        turno.setPrecio(null);
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado remuneracion = capturada();
        assertEquals(EstadoRemuneracion.PENDIENTE_REVISION_MANUAL, remuneracion.getEstado());
        assertNull(remuneracion.getPrecioAplicado());
        assertNull(remuneracion.getPorcentajeAplicado());
        assertNull(remuneracion.getMontoCalculado());
    }

    @Test
    void gratitudCierraSinRemuneracionNiSnapshotPagable() {
        resultado.setTipoResultado(DecisionFinanciera.GRATUIDAD);
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado remuneracion = capturada();
        assertEquals(EstadoRemuneracion.SIN_REMUNERACION, remuneracion.getEstado());
        assertNull(remuneracion.getMontoCalculado());
        verifyNoInteractions(empleadoRepository, empleadoServicioRepository);
    }

    @Test
    void unTurnoSinEmpleadoNoCreaRemuneracion() {
        turno.setEmpleado(null);

        service.resolverDecision(turno, resultado, true);

        verifyNoInteractions(remuneracionRepository, empleadoRepository, empleadoServicioRepository);
    }

    @Test
    void unaComisionExcepcionalSePreservaAlDesasignarSoloEnTurnosCompletadosPendientes() {
        asignacion.setPorcentajeComision(new BigDecimal("30.00"));
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.conservarExcepcionesAlDesasignar(List.of(turno), asignacion);

        assertEquals(new BigDecimal("30.00"), capturada().getPorcentajeExcepcionPreservado());
    }

    @Test
    void excepcionEliminadaMientrasSigueAsignadoVuelveAlPorcentajeGeneral() {
        asignacion.setPorcentajeComision(null);
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        assertEquals(new BigDecimal("12.50"), capturada().getPorcentajeAplicado());
    }

    @Test
    void turnoAnteriorDesasignadoSinSnapshotQuedaParaRevisionYNoUsaComisionInventada() {
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());
        when(empleadoServicioRepository.findWithLockingByEmpleadoAndServicio(4L, 5L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado remuneracion = capturada();
        assertEquals(EstadoRemuneracion.PENDIENTE_REVISION_MANUAL, remuneracion.getEstado());
        assertTrue(remuneracion.isRequiereRevisionComision());
        assertNull(remuneracion.getPorcentajeAplicado());
    }

    @Test
    void correccionManualConfirmaConSnapshotYNoModificaTurnoPrecio() {
        turno.setPrecio(null);
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.pendienteRevision(resultado, "Precio nulo", false);
        Usuario owner = turno.getNegocio().getOwner();
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.of(pendiente));

        service.corregirPrecioHistorico(turno, resultado,
                new CorregirPrecioHistoricoRequestDto(new BigDecimal("177.77"), "Comprobante histórico"), owner);

        assertNull(turno.getPrecio());
        assertEquals(new BigDecimal("177.77"), pendiente.getPrecioAplicado());
        assertEquals(new BigDecimal("12.50"), pendiente.getPorcentajeAplicado());
        assertEquals(new BigDecimal("22.22"), pendiente.getMontoCalculado());
        assertTrue(pendiente.isPrecioCorregidoManualmente());
        assertEquals(owner, pendiente.getOwnerCorrector());
        assertEquals(EstadoRemuneracion.CONFIRMADA, pendiente.getEstado());
    }

    @Test
    void precioCorregidoConComisionNoResolubleQuedaPendienteYOwnerLaResuelveAuditadamente() {
        turno.setPrecio(null);
        when(empleadoServicioRepository.findWithLockingByEmpleadoAndServicio(4L, 5L))
                .thenReturn(Optional.empty());
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.pendienteRevision(resultado, "Sin snapshot histórico de comisión", true);
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.of(pendiente));
        Usuario owner = turno.getNegocio().getOwner();

        service.corregirPrecioHistorico(turno, resultado,
                new CorregirPrecioHistoricoRequestDto(new BigDecimal("177.77"), "Comprobante histórico"), owner);

        assertEquals(EstadoRemuneracion.PENDIENTE_REVISION_MANUAL, pendiente.getEstado());
        assertEquals(new BigDecimal("177.77"), pendiente.getPrecioAplicado());
        assertNull(pendiente.getPorcentajeAplicado());

        service.resolverComisionManualmente(turno, resultado,
                new ResolverComisionRemuneracionRequestDto(new BigDecimal("20.00"), "Acuerdo documentado"), owner);

        assertEquals(EstadoRemuneracion.CONFIRMADA, pendiente.getEstado());
        assertEquals(new BigDecimal("177.77"), pendiente.getPrecioAplicado());
        assertEquals(new BigDecimal("20.00"), pendiente.getPorcentajeAplicado());
        assertEquals(new BigDecimal("35.55"), pendiente.getMontoCalculado());
        assertEquals(owner, pendiente.getOwnerResolutorComision());
        assertEquals("Acuerdo documentado", pendiente.getMotivoResolucionComision());
        assertNotNull(pendiente.getFechaResolucionComision());
        assertNull(turno.getPrecio());
        BigDecimal montoConfirmado = pendiente.getMontoCalculado();
        assertThrows(ResponseStatusException.class, () -> service.resolverComisionManualmente(turno, resultado,
                new ResolverComisionRemuneracionRequestDto(new BigDecimal("25.00"), "Nuevo acuerdo"), owner));
        assertEquals(new BigDecimal("20.00"), pendiente.getPorcentajeAplicado());
        assertEquals(montoConfirmado, pendiente.getMontoCalculado());
    }

    @Test
    void laComisionGeneralSeCongelaAlRegularizarCuandoElPrecioHistoricoEsNulo() {
        turno.setPrecio(null);
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());

        service.resolverDecision(turno, resultado, false);

        RemuneracionEmpleado pendiente = capturada();
        assertEquals(new BigDecimal("12.50"), pendiente.getPorcentajeComisionPendiente());
        empleado.setPorcentajeRemuneracion(new BigDecimal("40.00"));
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.of(pendiente));

        service.corregirPrecioHistorico(turno, resultado,
                new CorregirPrecioHistoricoRequestDto(new BigDecimal("100.00"), "Comprobante histórico"),
                turno.getNegocio().getOwner());

        assertEquals(new BigDecimal("12.50"), pendiente.getPorcentajeAplicado());
        assertEquals(new BigDecimal("12.50"), pendiente.getMontoCalculado());
    }

    @Test
    void correccionConfirmadaIdempotenteNoRecalculaYDatosDiferentesProducenConflicto() {
        turno.setPrecio(null);
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.pendienteRevision(resultado, "Precio nulo", false);
        Usuario owner = turno.getNegocio().getOwner();
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.of(pendiente));
        var correccion = new CorregirPrecioHistoricoRequestDto(new BigDecimal("177.77"), "Comprobante histórico");
        service.corregirPrecioHistorico(turno, resultado, correccion, owner);
        BigDecimal confirmado = pendiente.getMontoCalculado();

        service.corregirPrecioHistorico(turno, resultado, correccion, owner);

        assertEquals(confirmado, pendiente.getMontoCalculado());
        assertThrows(ResponseStatusException.class, () -> service.corregirPrecioHistorico(turno, resultado,
                new CorregirPrecioHistoricoRequestDto(new BigDecimal("180.00"), "Otro comprobante"), owner));
    }

    @Test
    void rechazaImportesManualesQueExcedenLaColumnaDecimal() {
        turno.setPrecio(null);
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.pendienteRevision(resultado, "Precio nulo", false);
        assertThrows(ResponseStatusException.class, () -> service.corregirPrecioHistorico(turno, resultado,
                new CorregirPrecioHistoricoRequestDto(new BigDecimal("100000000.00"), "Comprobante histórico"),
                turno.getNegocio().getOwner()));
        assertNull(pendiente.getPrecioAplicado());
    }

    @Test
    void rechazaPorcentajeManualFueraDelRangoPersistibleSinCambiarLaPendiente() {
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.pendienteRevision(resultado, "Sin snapshot histórico de comisión", true);
        pendiente.corregirPrecio(new BigDecimal("100.00"), turno.getNegocio().getOwner(),
                "Precio documentado", java.time.LocalDateTime.now());
        assertThrows(ResponseStatusException.class, () -> service.resolverComisionManualmente(turno, resultado,
                new ResolverComisionRemuneracionRequestDto(new BigDecimal("100.01"), "Acuerdo documentado"),
                turno.getNegocio().getOwner()));
        assertEquals(EstadoRemuneracion.PENDIENTE_REVISION_MANUAL, pendiente.getEstado());
        assertNull(pendiente.getPorcentajeAplicado());
    }

    @Test
    void excepcionPreservadaNoPuedeSerReemplazadaPorResolucionManual() {
        RemuneracionEmpleado pendiente = RemuneracionEmpleado.pendiente(turno, java.time.LocalDateTime.now());
        pendiente.preservarExcepcion(new BigDecimal("30.00"));
        pendiente.pendienteRevision(resultado, "Sin snapshot histórico de comisión", true);
        pendiente.corregirPrecio(new BigDecimal("100.00"), turno.getNegocio().getOwner(),
                "Precio documentado", java.time.LocalDateTime.now());
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.of(pendiente));

        assertThrows(ResponseStatusException.class, () -> service.resolverComisionManualmente(turno, resultado,
                new ResolverComisionRemuneracionRequestDto(new BigDecimal("20.00"), "Acuerdo documentado"),
                turno.getNegocio().getOwner()));
        assertEquals(new BigDecimal("30.00"), pendiente.getPorcentajeExcepcionPreservado());
    }

    @Test
    void remuneracionConfirmadaNoAdmiteMutacion() {
        when(remuneracionRepository.findByTurno_TurnoId(6L)).thenReturn(Optional.empty());
        service.resolverDecision(turno, resultado, false);
        RemuneracionEmpleado remuneracion = capturada();

        assertThrows(IllegalStateException.class, () -> remuneracion.cerrarSinRemuneracion(resultado));
    }

    private RemuneracionEmpleado capturada() {
        var captor = org.mockito.ArgumentCaptor.forClass(RemuneracionEmpleado.class);
        verify(remuneracionRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
