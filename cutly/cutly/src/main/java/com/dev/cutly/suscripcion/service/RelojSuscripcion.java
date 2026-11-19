package com.dev.cutly.suscripcion.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Reloj funcional para operaciones del dominio de suscripciones. */
public final class RelojSuscripcion {

    public static final String IDENTIFICACION_SISTEMA = "SISTEMA";
    public static final ZoneId ZONA_FUNCIONAL = ZoneId.of("America/Argentina/Buenos_Aires");

    private RelojSuscripcion() {
    }

    public static LocalDate fechaActual() {
        return LocalDate.now(ZONA_FUNCIONAL);
    }

    public static LocalDateTime fechaHoraActual() {
        return LocalDateTime.now(ZONA_FUNCIONAL);
    }
}
