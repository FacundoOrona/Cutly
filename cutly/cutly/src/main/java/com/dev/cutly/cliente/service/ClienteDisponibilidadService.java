package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.disponibilidad.ClienteDisponibilidadDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ClienteDisponibilidadService {

    private final NegocioRepository negocioRepository;
    private final HorarioNegocioRepository horarioRepository;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;
    private final TurnoRepository turnoRepository;

    public ClienteDisponibilidadService(
            NegocioRepository negocioRepository,
            HorarioNegocioRepository horarioRepository,
            ServicioRepository servicioRepository,
            EmpleadoRepository empleadoRepository,
            EmpleadoServiciorRepository empleadoServicioRepository,
            TurnoRepository turnoRepository
    ) {
        this.negocioRepository = negocioRepository;
        this.horarioRepository = horarioRepository;
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<ClienteDisponibilidadDto> listar(
            Long negocioId, LocalDate fecha, Long servicioId, Long empleadoId
    ) {
        validarNegocio(negocioId);
        Servicio servicio = buscarServicio(negocioId, servicioId);
        List<Empleado> empleados = empleadosDisponibles(negocioId, servicioId, empleadoId);
        return generarDisponibilidad(negocioId, fecha, servicio, empleados);
    }

    public List<ClienteDisponibilidadDto> listarPorEmpleado(
            Long negocioId, Long empleadoId, LocalDate fecha, Long servicioId
    ) {
        validarNegocio(negocioId);
        Servicio servicio = buscarServicio(negocioId, servicioId);
        List<Empleado> empleados = empleadosDisponibles(negocioId, servicioId, empleadoId);
        return generarDisponibilidad(negocioId, fecha, servicio, empleados);
    }

    private List<Empleado> empleadosDisponibles(Long negocioId, Long servicioId, Long empleadoId) {
        List<Empleado> empleados = empleadoId == null
                ? empleadoRepository.findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(negocioId)
                : List.of(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(empleadoId, negocioId)
                .filter(Empleado::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el empleado activo solicitado")));
        List<Empleado> resultado = empleados.stream()
                .filter(e -> empleadoServicioRepository
                        .findByEmpleado_EmpleadoIdAndServicio_ServicioId(e.getEmpleadoId(), servicioId)
                        .isPresent())
                .toList();
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay empleados activos asignados al servicio solicitado");
        }
        return resultado;
    }

    private List<ClienteDisponibilidadDto> generarDisponibilidad(
            Long negocioId, LocalDate fecha, Servicio servicio, List<Empleado> empleados
    ) {
        List<HorarioNegocio> horarios = horarioRepository
                .findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(negocioId).stream()
                .filter(h -> h.getDiaSemana().name().equals(fecha.getDayOfWeek().name()))
                .toList();
        if (horarios.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No existe un horario configurado para la fecha solicitada");
        List<ClienteDisponibilidadDto> resultado = new java.util.ArrayList<>();
        for (HorarioNegocio horario : horarios) {
            if (horario.isCerrado()) continue;
            LocalDateTime desde = fecha.atTime(horario.getHoraApertura());
            LocalDateTime hasta = fecha.atTime(horario.getHoraCierre());
            List<Turno> turnos = turnoRepository.findTurnosSolapados(negocioId, desde, hasta)
                    .stream().filter(this::bloqueaHorario).toList();
            LocalTime inicio = horario.getHoraApertura();
            while (!inicio.plusMinutes(servicio.getDuracionMinutos()).isAfter(horario.getHoraCierre())) {
                LocalDateTime slotInicio = fecha.atTime(inicio);
                LocalDateTime slotFin = slotInicio.plusMinutes(servicio.getDuracionMinutos());
                Empleado empleado = empleados.stream()
                        .filter(e -> turnos.stream().noneMatch(t -> mismoResponsable(t, e)
                                && t.getFechaHoraInicio().isBefore(slotFin) && t.getFechaHoraFin().isAfter(slotInicio)))
                        .findFirst().orElse(null);
                if (empleado != null) {
                    resultado.add(new ClienteDisponibilidadDto(slotInicio, slotFin, servicio.getServicioId(), empleado.getEmpleadoId()));
                }
                inicio = inicio.plusMinutes(servicio.getDuracionMinutos());
            }
        }
        return resultado;
    }

    private boolean mismoResponsable(Turno turno, Empleado empleado) {
        if (turno.getEmpleado() != null) {
            return turno.getEmpleado().getEmpleadoId().equals(empleado.getEmpleadoId());
        }
        Usuario ownerResponsable = turno.getOwnerResponsable();
        return ownerResponsable != null
                && empleado.getUsuario() != null
                && Objects.equals(ownerResponsable.getUsuarioId(), empleado.getUsuario().getUsuarioId());
    }

    private boolean bloqueaHorario(Turno turno) {
        return turno.getStatus() != TurnoStatus.CANCELLED && turno.getStatus() != TurnoStatus.REJECTED;
    }

    private void validarNegocio(Long negocioId) {
        negocioRepository.findByNegocioIdAndStatus(negocioId, NegocioStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio activo solicitado"));
    }

    private Servicio buscarServicio(Long negocioId, Long servicioId) {
        Servicio servicio = servicioRepository.findByServicioIdAndNegocio_NegocioId(servicioId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el servicio solicitado"));
        if (!servicio.isActivo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El servicio solicitado no está activo");
        }
        return servicio;
    }
}
