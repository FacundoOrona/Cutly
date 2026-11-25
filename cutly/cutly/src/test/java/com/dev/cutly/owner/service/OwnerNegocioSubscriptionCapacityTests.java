package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.negocio.CrearNegocioRequestDto;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.service.HistorialSuscripcionService;
import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerNegocioSubscriptionCapacityTests {
    @Mock NegocioRepository negocioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SuscripcionRepository suscripcionRepository;
    @Mock PlanSuscripcionService planService;
    @Mock HistorialSuscripcionService historyService;
    OwnerNegocioService service;
    Usuario owner;

    @BeforeEach
    void setUp() {
        service = new OwnerNegocioService(negocioRepository, usuarioRepository, suscripcionRepository,
                planService, historyService);
        owner = new Usuario();
        owner.setUsuarioId(31L);
        owner.setEmail("owner@example.com");
        owner.setRol(Rol.OWNER);
        when(usuarioRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(usuarioRepository.findWithLockingByUsuarioId(31L)).thenReturn(Optional.of(owner));
    }

    @Test
    void primerNegocioGeneraTrialDeCatorceDiasUnaSolaVez() {
        when(negocioRepository.countByOwner_UsuarioId(31L)).thenReturn(0L);
        when(suscripcionRepository.findWithLockingByOwner_UsuarioId(31L)).thenReturn(Optional.empty());
        PlanSuscripcion trial = PlanSuscripcion.crearPlanTrial();
        when(planService.obtenerOCrearPlanPruebaGratuita()).thenReturn(trial);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(invocation -> {
            Negocio negocio = invocation.getArgument(0);
            negocio.setNegocioId(41L);
            return negocio;
        });
        when(suscripcionRepository.save(any(Suscripcion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.crearNegocio(new CrearNegocioRequestDto("Local", null, null, null), "owner@example.com");

        assertTrue(owner.isTrialSuscripcionUtilizado());
        assertEquals(1, trial.getMaxNegocios());
        verify(usuarioRepository).save(owner);
        verify(suscripcionRepository).save(argThat(sub -> sub.getOwner() == owner
                && sub.getStatus() == SuscripcionStatus.TRIAL
                && sub.getFechaVencimiento().equals(sub.getFechaInicio().plusDays(13))));
    }

    @Test
    void ownerConTrialNoPuedeCrearUnSegundoNegocio() {
        when(negocioRepository.countByOwner_UsuarioId(31L)).thenReturn(1L);
        when(suscripcionRepository.findWithLockingByOwner_UsuarioId(31L))
                .thenReturn(Optional.of(subscription(SuscripcionStatus.TRIAL, 1)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crearNegocio(new CrearNegocioRequestDto("Otro local", null, null, null), "owner@example.com"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void negocioPosteriorQuedaInactivoAunqueElPlanTengaCapacidadDisponible() {
        when(negocioRepository.countByOwner_UsuarioId(31L)).thenReturn(1L);
        when(suscripcionRepository.findWithLockingByOwner_UsuarioId(31L))
                .thenReturn(Optional.of(subscription(SuscripcionStatus.ACTIVE, 3)));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.crearNegocio(new CrearNegocioRequestDto("Otro local", null, null, null), "owner@example.com");

        verify(negocioRepository).save(argThat(negocio -> negocio.getStatus()
                == com.dev.cutly.negocio.enums.NegocioStatus.INACTIVE));
    }

    @Test
    void planesDeUnoDosYTresRechazanElNegocioQueExcedeSuCapacidadBajoBloqueoDelOwner() {
        AtomicInteger capacidad = new AtomicInteger();
        when(negocioRepository.countByOwner_UsuarioId(31L)).thenAnswer(invocation -> (long) capacidad.get());
        when(suscripcionRepository.findWithLockingByOwner_UsuarioId(31L))
                .thenAnswer(invocation -> Optional.of(subscription(SuscripcionStatus.ACTIVE, capacidad.get())));
        for (int max = 1; max <= 3; max++) {
            capacidad.set(max);
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.crearNegocio(new CrearNegocioRequestDto("Otro local", null, null, null), "owner@example.com"));
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
        verify(usuarioRepository, times(3)).findWithLockingByUsuarioId(31L);
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    private Suscripcion subscription(SuscripcionStatus status, int maxNegocios) {
        PlanSuscripcion plan = status == SuscripcionStatus.TRIAL
                ? PlanSuscripcion.crearPlanTrial()
                : PlanSuscripcion.crearPlanComercial("Plan", null, new BigDecimal("100.00"), maxNegocios);
        Suscripcion subscription = new Suscripcion();
        subscription.setOwner(owner);
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setFechaInicio(LocalDate.now().minusDays(1));
        subscription.setFechaVencimiento(LocalDate.now().plusDays(20));
        subscription.asignarPrecioContratado(status == SuscripcionStatus.TRIAL
                ? BigDecimal.ZERO : new BigDecimal("100.00"));
        return subscription;
    }
}
