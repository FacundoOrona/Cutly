package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface PlanSuscripcionRepository extends Repository<PlanSuscripcion, Long> {

    PlanSuscripcion save(PlanSuscripcion planSuscripcion);

    List<PlanSuscripcion> findAll();

    List<PlanSuscripcion> findByNombreIgnoreCase(String nombre);
}
