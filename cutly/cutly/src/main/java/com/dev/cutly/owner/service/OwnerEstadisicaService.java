package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.estadistica.OwnerEmpleadoEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerIngresosEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerResumenEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerServicioEstadisticaDto;
import com.dev.cutly.owner.dto.estadistica.OwnerTurnosEstadisticaDto;
import com.dev.cutly.pago.repository.PagoRepository;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OwnerEstadisicaService {

    private final TurnoRepository turnoRepository;
    private final PagoRepository pagoRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerEstadisicaService(
            TurnoRepository turnoRepository,
            PagoRepository pagoRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.turnoRepository = turnoRepository;
        this.pagoRepository = pagoRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public OwnerResumenEstadisticaDto obtenerResumen(
            Long negocioId,
            LocalDate desde,
            LocalDate hasta,
            String emailOwner
    ) {
        Rango rango = validarYCrearRango(desde, hasta);
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Map<TurnoStatus, Long> conteos = contarEstados(negocioId, rango);
        OwnerIngresosEstadisticaDto ingresos = calcularIngresos(negocioId, rango);
        long totalTurnos = conteos.values().stream().mapToLong(Long::longValue).sum();
        return new OwnerResumenEstadisticaDto(
                totalTurnos,
                contar(conteos, TurnoStatus.PENDING),
                contar(conteos, TurnoStatus.CONFIRMED),
                contar(conteos, TurnoStatus.IN_PROGRESS),
                contar(conteos, TurnoStatus.COMPLETED),
                contar(conteos, TurnoStatus.CANCELLED),
                contar(conteos, TurnoStatus.NO_SHOW),
                contar(conteos, TurnoStatus.REJECTED),
                ingresos.ingresosTotales(),
                ingresos.cantidadPagos()
        );
    }

    public OwnerTurnosEstadisticaDto obtenerEstadisticasTurnos(
            Long negocioId,
            LocalDate desde,
            LocalDate hasta,
            String emailOwner
    ) {
        Rango rango = validarYCrearRango(desde, hasta);
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Map<TurnoStatus, Long> conteos = contarEstados(negocioId, rango);
        return new OwnerTurnosEstadisticaDto(
                conteos.values().stream().mapToLong(Long::longValue).sum(),
                contar(conteos, TurnoStatus.PENDING),
                contar(conteos, TurnoStatus.CONFIRMED),
                contar(conteos, TurnoStatus.IN_PROGRESS),
                contar(conteos, TurnoStatus.COMPLETED),
                contar(conteos, TurnoStatus.CANCELLED),
                contar(conteos, TurnoStatus.NO_SHOW),
                contar(conteos, TurnoStatus.REJECTED)
        );
    }

    public OwnerIngresosEstadisticaDto obtenerEstadisticasIngresos(
            Long negocioId,
            LocalDate desde,
            LocalDate hasta,
            String emailOwner
    ) {
        Rango rango = validarYCrearRango(desde, hasta);
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return calcularIngresos(negocioId, rango);
    }

    public List<OwnerEmpleadoEstadisticaDto> obtenerEstadisticasEmpleados(
            Long negocioId,
            LocalDate desde,
            LocalDate hasta,
            String emailOwner
    ) {
        Rango rango = validarYCrearRango(desde, hasta);
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return turnoRepository.estadisticasPorEmpleado(negocioId, rango.desde(), rango.hastaExclusivo());
    }

    public List<OwnerServicioEstadisticaDto> obtenerEstadisticasServicios(
            Long negocioId,
            LocalDate desde,
            LocalDate hasta,
            String emailOwner
    ) {
        Rango rango = validarYCrearRango(desde, hasta);
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return turnoRepository.estadisticasPorServicio(negocioId, rango.desde(), rango.hastaExclusivo());
    }

    private Map<TurnoStatus, Long> contarEstados(Long negocioId, Rango rango) {
        Map<TurnoStatus, Long> conteos = new EnumMap<>(TurnoStatus.class);
        for (Object[] fila : turnoRepository.contarPorEstadoYPeriodo(
                negocioId, rango.desde(), rango.hastaExclusivo()
        )) {
            conteos.put((TurnoStatus) fila[0], (Long) fila[1]);
        }
        return conteos;
    }

    private OwnerIngresosEstadisticaDto calcularIngresos(Long negocioId, Rango rango) {
        BigDecimal total = pagoRepository.sumarMontosPorNegocioYPeriodo(
                negocioId, rango.desde(), rango.hastaExclusivo()
        );
        long cantidad = pagoRepository.contarPorNegocioYPeriodo(
                negocioId, rango.desde(), rango.hastaExclusivo()
        );
        return new OwnerIngresosEstadisticaDto(total == null ? BigDecimal.ZERO : total, cantidad);
    }

    private long contar(Map<TurnoStatus, Long> conteos, TurnoStatus status) {
        return conteos.getOrDefault(status, 0L);
    }

    private Rango validarYCrearRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
        return new Rango(
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay()
        );
    }

    private record Rango(LocalDateTime desde, LocalDateTime hastaExclusivo) {
    }
}
