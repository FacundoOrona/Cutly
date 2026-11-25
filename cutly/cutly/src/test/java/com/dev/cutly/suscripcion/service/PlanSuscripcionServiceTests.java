package com.dev.cutly.suscripcion.service;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.repository.PlanSuscripcionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanSuscripcionServiceTests {

    @Test
    void inicializacionNoCorrigeAtributosDeUnTrialExistente() {
        PlanSuscripcion trialExistente = PlanSuscripcion.crearPlanTrial();
        ReflectionTestUtils.setField(trialExistente, "maxNegocios", null);
        PlanSuscripcionRepository repository = mock(PlanSuscripcionRepository.class);
        when(repository.findAll()).thenReturn(List.of(trialExistente));

        assertThrows(IllegalStateException.class,
                () -> new PlanSuscripcionService(repository).obtenerOCrearPlanPruebaGratuita());

        org.junit.jupiter.api.Assertions.assertNull(trialExistente.getMaxNegocios());
        verify(repository).findAll();
        verify(repository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
