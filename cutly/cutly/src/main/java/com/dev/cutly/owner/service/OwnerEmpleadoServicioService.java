package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.owner.dto.servicio.OwnerServicioDto;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.remuneracion.service.RemuneracionService;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.pago.repository.ResultadoFinancieroTurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class OwnerEmpleadoServicioService {

    private final EmpleadoServiciorRepository empleadoServicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ServicioRepository servicioRepository;
    private final OwnerNegocioService ownerNegocioService;
    private final TurnoRepository turnoRepository;
    private final RemuneracionService remuneracionService;
    private final ResultadoFinancieroTurnoRepository resultadoRepository;

    public OwnerEmpleadoServicioService(
            EmpleadoServiciorRepository empleadoServicioRepository,
            EmpleadoRepository empleadoRepository,
            ServicioRepository servicioRepository,
            OwnerNegocioService ownerNegocioService,
            TurnoRepository turnoRepository,
            RemuneracionService remuneracionService,
            ResultadoFinancieroTurnoRepository resultadoRepository
    ) {
        this.empleadoServicioRepository = empleadoServicioRepository;
        this.empleadoRepository = empleadoRepository;
        this.servicioRepository = servicioRepository;
        this.ownerNegocioService = ownerNegocioService;
        this.turnoRepository = turnoRepository;
        this.remuneracionService = remuneracionService;
        this.resultadoRepository = resultadoRepository;
    }

    public List<OwnerServicioDto> listarServiciosAsignados(
            Long negocioId,
            Long empleadoId,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        buscarEmpleado(negocioId, empleadoId);
        return empleadoServicioRepository.findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(empleadoId)
                .stream()
                .map(EmpleadoServicio::getServicio)
                .map(this::aDto)
                .toList();
    }

    public OwnerServicioDto asignarServicio(
            Long negocioId,
            Long empleadoId,
            Long servicioId,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Empleado empleado = buscarEmpleado(negocioId, empleadoId);
        Servicio servicio = buscarServicio(negocioId, servicioId);

        if (empleadoServicioRepository
                .findByEmpleado_EmpleadoIdAndServicio_ServicioId(empleadoId, servicioId)
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio ya está asignado al empleado");
        }

        EmpleadoServicio asignacion = new EmpleadoServicio();
        asignacion.setEmpleado(empleado);
        asignacion.setServicio(servicio);
        empleadoServicioRepository.save(asignacion);
        return aDto(servicio);
    }

    public void quitarServicio(
            Long negocioId,
            Long empleadoId,
            Long servicioId,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        buscarEmpleado(negocioId, empleadoId);
        buscarServicio(negocioId, servicioId);
        List<com.dev.cutly.turno.entity.Turno> turnosBloqueados = turnoRepository
                .findActiveOrCompletedForEmpleadoServicioWithLocking(negocioId, empleadoId, servicioId);
        EmpleadoServicio asignacion = empleadoServicioRepository
                .findWithLockingByEmpleadoAndServicio(empleadoId, servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El servicio no está asignado al empleado"));
        List<com.dev.cutly.turno.entity.Turno> turnosPendientes = turnosBloqueados.stream()
                .filter(turno -> turno.getStatus() == com.dev.cutly.turno.enums.TurnoStatus.COMPLETED)
                .filter(turno -> !resultadoRepository.existsByTurno_TurnoId(turno.getTurnoId()))
                .toList();
        remuneracionService.conservarExcepcionesAlDesasignar(turnosPendientes, asignacion);
        empleadoServicioRepository.delete(asignacion);
    }

    private Empleado buscarEmpleado(Long negocioId, Long empleadoId) {
        return empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(empleadoId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el empleado solicitado"));
    }

    private Servicio buscarServicio(Long negocioId, Long servicioId) {
        return servicioRepository.findByServicioIdAndNegocio_NegocioId(servicioId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el servicio solicitado"));
    }

    private OwnerServicioDto aDto(Servicio servicio) {
        return new OwnerServicioDto(
                servicio.getServicioId(),
                servicio.getNegocio().getNegocioId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.isActivo()
        );
    }
}
