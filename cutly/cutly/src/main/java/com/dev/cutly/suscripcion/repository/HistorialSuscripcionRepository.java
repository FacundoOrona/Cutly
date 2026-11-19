package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.HistorialSuscripcion;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface HistorialSuscripcionRepository extends Repository<HistorialSuscripcion, Long> {

    HistorialSuscripcion save(HistorialSuscripcion historialSuscripcion);

    List<HistorialSuscripcion> findBySuscripcion_SuscripcionIdOrderByFechaEventoAscHistorialSuscripcionIdAsc(Long suscripcionId);
}
