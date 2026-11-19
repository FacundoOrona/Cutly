package com.dev.cutly.suscripcion.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

@Component
public class PlanSuscripcionReadiness {

    private volatile List<String> pendientes = List.of("La inicialización de planes todavía no finalizó");

    public boolean isReady() {
        return pendientes.isEmpty();
    }

    public void markReady() {
        pendientes = List.of();
    }

    public void markBlocked(List<String> condicionesPendientes) {
        pendientes = condicionesPendientes == null || condicionesPendientes.isEmpty()
                ? List.of("La preparación del esquema de planes no pudo verificarse")
                : List.copyOf(condicionesPendientes);
    }

    public String mensajeNoDisponible() {
        StringJoiner joiner = new StringJoiner("; ");
        pendientes.forEach(joiner::add);
        return "Las escrituras administrativas de planes están temporalmente bloqueadas. " + joiner;
    }
}
