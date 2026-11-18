package com.dev.cutly.suscripcion.repository;

import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {

    List<PlanSuscripcion> findByNombreIgnoreCase(String nombre);
}
