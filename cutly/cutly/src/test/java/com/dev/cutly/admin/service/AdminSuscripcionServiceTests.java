package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.suscripcion.AsignarSuscripcionRequestDto;
import com.dev.cutly.admin.dto.suscripcion.RegularizarSuscripcionRequestDto;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.suscripcion.enums.TipoPlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.service.HistorialSuscripcionService;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSuscripcionServiceTests {
    @Mock SuscripcionRepository suscripcionRepository;
    @Mock HistorialSuscripcionService historialService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock NegocioRepository negocioRepository;
    @Mock PlanSuscripcionRepository planRepository;
    AdminSuscripcionService service;

    @BeforeEach
    void setUp() {
        service = new AdminSuscripcionService(suscripcionRepository, historialService, usuarioRepository,
                negocioRepository, planRepository);
    }

    @Test
    void asignaAlOwnerUnaSuscripcionPendingConElPrecioYCapacidadDelPlan() {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Usuario admin = user(12L, Rol.SUPER_ADMIN, "admin@example.com");
        PlanSuscripcion plan = plan();
        when(usuarioRepository.findWithLockingByUsuarioId(11L)).thenReturn(Optional.of(owner));
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(suscripcionRepository.existsByOwner_UsuarioId(11L)).thenReturn(false);
        when(planRepository.findWithLockingByPlanId(4L)).thenReturn(Optional.of(plan));
        when(negocioRepository.countByOwner_UsuarioId(11L)).thenReturn(0L);
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(invocation -> {
            Suscripcion saved = invocation.getArgument(0);
            saved.setSuscripcionId(25L);
            return saved;
        });

        var result = service.asignar(11L, new AsignarSuscripcionRequestDto(4L, "Pago pendiente"),
                "admin@example.com", "127.0.0.1");

        assertEquals(SuscripcionStatus.PENDING, result.status());
        assertEquals(11L, result.ownerId());
        assertEquals(3, result.maxNegocios());
        assertEquals(new BigDecimal("1200.00"), result.precioContratado());
        verify(suscripcionRepository).save(argThat(s -> s.getOwner() == owner
                && s.getStatus() == SuscripcionStatus.PENDING
                && s.getFechaInicio().equals(RelojSuscripcion.fechaActual())
                && s.getFechaVencimiento().equals(RelojSuscripcion.fechaActual().plusDays(30))));
    }

    @Test
    void activacionYRenovacionInicianHoySinAcumularDiasYConservanPrecioContratado() {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Usuario admin = user(12L, Rol.SUPER_ADMIN, "admin@example.com");
        PlanSuscripcion plan = plan();
        plan.actualizarDatosComerciales("Plan tres", "", new BigDecimal("1800.00"), 3);
        Suscripcion suscripcion = suscripcion(owner, plan, SuscripcionStatus.PENDING,
                RelojSuscripcion.fechaActual().minusDays(5), RelojSuscripcion.fechaActual().plusDays(25));
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(suscripcionRepository.findWithLockingBySuscripcionId(25L)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.actualizarEstado(25L, SuscripcionStatus.ACTIVE, "admin@example.com", "127.0.0.1", null);
        assertEquals(RelojSuscripcion.fechaActual(), suscripcion.getFechaInicio());
        assertEquals(RelojSuscripcion.fechaActual().plusDays(30), suscripcion.getFechaVencimiento());
        assertEquals(new BigDecimal("1200.00"), suscripcion.getPrecioContratado());

        suscripcion.setFechaVencimiento(RelojSuscripcion.fechaActual().plusDays(28));
        service.actualizarEstado(25L, SuscripcionStatus.ACTIVE, "admin@example.com", "127.0.0.1", null);
        assertEquals(RelojSuscripcion.fechaActual(), suscripcion.getFechaInicio());
        assertEquals(RelojSuscripcion.fechaActual().plusDays(30), suscripcion.getFechaVencimiento());
        ArgumentCaptor<TipoEventoSuscripcion> event = ArgumentCaptor.forClass(TipoEventoSuscripcion.class);
        verify(historialService, times(2)).registrar(eq(suscripcion), event.capture(), any(), any(), any(),
                eq(admin), isNull(), any(), anyString(), anyString());
        assertEquals(TipoEventoSuscripcion.ACTIVATED, event.getAllValues().get(0));
        assertEquals(TipoEventoSuscripcion.RENEWED, event.getAllValues().get(1));
    }

    @Test
    void trialNoPuedeActivarseManualmente() {
        assertTrialStateOperationRejected(SuscripcionStatus.ACTIVE);
    }

    @Test
    void trialNoPuedeSuspenderse() {
        assertTrialStateOperationRejected(SuscripcionStatus.SUSPENDED);
    }

    @Test
    void trialNoPuedeCancelarse() {
        assertTrialStateOperationRejected(SuscripcionStatus.CANCELLED);
    }

    @Test
    void regularizarTrialAComercialConservaFechasAsignaPrecioYSuspendeConHistorial() {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Usuario admin = user(12L, Rol.SUPER_ADMIN, "admin@example.com");
        PlanSuscripcion trial = PlanSuscripcion.crearPlanTrial();
        PlanSuscripcion commercial = plan();
        Suscripcion suscripcion = suscripcion(owner, trial, SuscripcionStatus.TRIAL,
                RelojSuscripcion.fechaActual().minusDays(5), RelojSuscripcion.fechaActual().plusDays(8));
        when(suscripcionRepository.findById(25L)).thenReturn(Optional.of(suscripcion));
        when(negocioRepository.countByOwner_UsuarioId(11L)).thenReturn(0L);
        String version = service.obtenerSuscripcion(25L).version();
        when(suscripcionRepository.findWithLockingBySuscripcionId(25L)).thenReturn(Optional.of(suscripcion));
        when(planRepository.findWithLockingByPlanId(4L)).thenReturn(Optional.of(commercial));
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.regularizar(25L,
                new RegularizarSuscripcionRequestDto(4L, null, null, null, null, version, null),
                "admin@example.com", "127.0.0.1");

        assertEquals(SuscripcionStatus.SUSPENDED, result.status());
        assertEquals(RelojSuscripcion.fechaActual().minusDays(5), result.fechaInicio());
        assertEquals(RelojSuscripcion.fechaActual().plusDays(8), result.fechaVencimiento());
        assertEquals(new BigDecimal("1200.00"), result.precioContratado());
        assertEquals(TipoPlanSuscripcion.COMERCIAL, suscripcion.getPlan().getTipo());
        verify(historialService).registrar(eq(suscripcion), eq(TipoEventoSuscripcion.SUSPENDED), isNull(),
                any(), any(), eq(admin), isNull(), isNull(), anyString(), anyString());
    }

    @Test
    void regularizacionNoPuedeDejarLaSuscripcionEnTrial() {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Suscripcion suscripcion = suscripcion(owner, PlanSuscripcion.crearPlanTrial(), SuscripcionStatus.TRIAL,
                RelojSuscripcion.fechaActual().minusDays(5), RelojSuscripcion.fechaActual().plusDays(8));
        when(suscripcionRepository.findById(25L)).thenReturn(Optional.of(suscripcion));
        when(negocioRepository.countByOwner_UsuarioId(11L)).thenReturn(0L);
        String version = service.obtenerSuscripcion(25L).version();
        when(suscripcionRepository.findWithLockingBySuscripcionId(25L)).thenReturn(Optional.of(suscripcion));

        assertThrows(ResponseStatusException.class, () -> service.regularizar(25L,
                new RegularizarSuscripcionRequestDto(null, suscripcion.getFechaInicio(), null, null, null, version, null),
                "admin@example.com", "127.0.0.1"));
        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(historialService);
    }

    @Test
    void suscripcionComercialPuedeSuspenderseYCancelarse() {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Usuario admin = user(12L, Rol.SUPER_ADMIN, "admin@example.com");
        Suscripcion suscripcion = suscripcion(owner, plan(), SuscripcionStatus.ACTIVE,
                RelojSuscripcion.fechaActual(), RelojSuscripcion.fechaActual().plusDays(30));
        when(usuarioRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(suscripcionRepository.findWithLockingBySuscripcionId(25L)).thenReturn(Optional.of(suscripcion));
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.actualizarEstado(25L, SuscripcionStatus.SUSPENDED, "admin@example.com", "127.0.0.1", null);
        assertEquals(SuscripcionStatus.SUSPENDED, suscripcion.getStatus());
        service.actualizarEstado(25L, SuscripcionStatus.CANCELLED, "admin@example.com", "127.0.0.1", null);
        assertEquals(SuscripcionStatus.CANCELLED, suscripcion.getStatus());
        verify(suscripcionRepository, times(2)).save(suscripcion);
        verify(historialService, times(2)).registrar(eq(suscripcion), any(), any(), any(), any(), eq(admin),
                isNull(), anyString(), anyString(), anyString());
    }

    private void assertTrialStateOperationRejected(SuscripcionStatus target) {
        Usuario owner = user(11L, Rol.OWNER, "owner@example.com");
        Suscripcion suscripcion = suscripcion(owner, PlanSuscripcion.crearPlanTrial(), SuscripcionStatus.TRIAL,
                RelojSuscripcion.fechaActual().minusDays(5), RelojSuscripcion.fechaActual().plusDays(8));
        when(suscripcionRepository.findWithLockingBySuscripcionId(25L)).thenReturn(Optional.of(suscripcion));

        assertThrows(ResponseStatusException.class, () -> service.actualizarEstado(
                25L, target, "admin@example.com", "127.0.0.1", null));
        verify(suscripcionRepository, never()).save(any());
        verifyNoInteractions(historialService);
    }

    private PlanSuscripcion plan() {
        PlanSuscripcion plan = PlanSuscripcion.crearPlanComercial("Plan tres", "", new BigDecimal("1200.00"), 3);
        plan.cambiarActivo(true);
        return plan;
    }

    private Suscripcion suscripcion(Usuario owner, PlanSuscripcion plan, SuscripcionStatus status,
                                     LocalDate inicio, LocalDate vencimiento) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setSuscripcionId(25L);
        suscripcion.setOwner(owner);
        suscripcion.setPlan(plan);
        suscripcion.setStatus(status);
        suscripcion.setFechaInicio(inicio);
        suscripcion.setFechaVencimiento(vencimiento);
        suscripcion.asignarPrecioContratado(status == SuscripcionStatus.TRIAL
                ? BigDecimal.ZERO : new BigDecimal("1200.00"));
        return suscripcion;
    }

    private Usuario user(Long id, Rol rol, String email) {
        Usuario user = new Usuario();
        user.setUsuarioId(id);
        user.setRol(rol);
        user.setEmail(email);
        user.setNombre("Test");
        user.setApellido("User");
        return user;
    }
}
