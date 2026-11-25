package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.suscripcion.AsignarSuscripcionRequestDto;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        suscripcion.asignarPrecioContratado(new BigDecimal("1200.00"));
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
