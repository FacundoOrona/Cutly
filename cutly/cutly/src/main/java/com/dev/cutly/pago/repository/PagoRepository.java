package com.dev.cutly.pago.repository;

import com.dev.cutly.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select sum(p.monto) from Pago p")
    BigDecimal sumarMontos();
}
