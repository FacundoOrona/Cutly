package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.dto.horario.EmpleadoDisponibilidadDto;
import com.dev.cutly.empleado.dto.horario.EmpleadoHorarioDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpleadoHorarioService {
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HorarioNegocioRepository horarioRepository;
    private final TurnoRepository turnoRepository;

    public EmpleadoHorarioService(UsuarioRepository usuarioRepository, EmpleadoRepository empleadoRepository,
                                  HorarioNegocioRepository horarioRepository, TurnoRepository turnoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.horarioRepository = horarioRepository;
        this.turnoRepository = turnoRepository;
    }

    public List<EmpleadoHorarioDto> listar(String emailEmpleado) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        return horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(empleado.getNegocio().getNegocioId())
                .stream().map(h -> new EmpleadoHorarioDto(h.getDiaSemana(), h.getHoraApertura(), h.getHoraCierre(), h.isCerrado())).toList();
    }

    public List<EmpleadoDisponibilidadDto> disponibilidad(LocalDate fecha, String emailEmpleado) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        List<HorarioNegocio> horarios = horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(empleado.getNegocio().getNegocioId())
                .stream().filter(h -> h.getDiaSemana().name().equals(fecha.getDayOfWeek().name())).toList();
        if (horarios.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un horario configurado para la fecha solicitada");
        List<EmpleadoDisponibilidadDto> resultado = new ArrayList<>();
        for (HorarioNegocio horario : horarios) {
            if (horario.isCerrado()) continue;
            LocalDateTime inicioJornada = fecha.atTime(horario.getHoraApertura());
            LocalDateTime finJornada = fecha.atTime(horario.getHoraCierre());
            List<Turno> turnos = turnoRepository.findTurnosSolapadosPorEmpleado(empleado.getEmpleadoId(), inicioJornada, finJornada)
                    .stream().filter(this::bloquea).sorted(java.util.Comparator.comparing(Turno::getFechaHoraInicio)).toList();
            LocalDateTime inicioDisponible = inicioJornada;
            for (Turno turno : turnos) {
                LocalDateTime inicioBloqueo = turno.getFechaHoraInicio().isAfter(inicioJornada) ? turno.getFechaHoraInicio() : inicioJornada;
                LocalDateTime finBloqueo = turno.getFechaHoraFin().isBefore(finJornada) ? turno.getFechaHoraFin() : finJornada;
                if (inicioDisponible.isBefore(inicioBloqueo)) {
                    resultado.add(new EmpleadoDisponibilidadDto(fecha, inicioDisponible, inicioBloqueo));
                }
                if (finBloqueo.isAfter(inicioDisponible)) inicioDisponible = finBloqueo;
            }
            if (inicioDisponible.isBefore(finJornada)) resultado.add(new EmpleadoDisponibilidadDto(fecha, inicioDisponible, finJornada));
        }
        return resultado;
    }

    private boolean bloquea(Turno turno) {
        return turno.getStatus() != TurnoStatus.CANCELLED && turno.getStatus() != TurnoStatus.REJECTED;
    }

    private Empleado obtenerEmpleadoActivo(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado"));
        if (usuario.getRol() != Rol.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un empleado");
        }
        Empleado empleado = empleadoRepository.findByUsuario_UsuarioId(usuario.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el perfil laboral del empleado"));
        if (!empleado.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El empleado no está activo");
        }
        return empleado;
    }
}
