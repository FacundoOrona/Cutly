package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EmpleadoTurnoService {
    private final TurnoRepository turnoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpleadoTurnoService(TurnoRepository turnoRepository, EmpleadoRepository empleadoRepository,
                                UsuarioRepository usuarioRepository) {
        this.turnoRepository = turnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoTurnoDto> listar(String emailEmpleado, LocalDate fecha, TurnoStatus status, boolean historial) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        LocalDateTime ahora = LocalDateTime.now();
        Specification<Turno> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("empleado").get("empleadoId"), empleado.getEmpleadoId()));
            if (fecha != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHoraInicio"), fecha.atStartOfDay()));
                predicates.add(cb.lessThan(root.get("fechaHoraInicio"), fecha.plusDays(1).atStartOfDay()));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (historial) {
                predicates.add(cb.or(
                        cb.lessThan(root.get("fechaHoraInicio"), ahora),
                        root.get("status").in(TurnoStatus.COMPLETED, TurnoStatus.CANCELLED, TurnoStatus.NO_SHOW)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return turnoRepository.findAll(filtros, Sort.by(Sort.Order.asc("fechaHoraInicio"), Sort.Order.asc("turnoId")))
                .stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public EmpleadoTurnoDto obtener(Long turnoId, String emailEmpleado) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        return aDto(buscarPropio(turnoId, empleado.getEmpleadoId()));
    }

    public EmpleadoTurnoDto confirmar(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.CONFIRMED);
    }

    public EmpleadoTurnoDto cancelar(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.CANCELLED);
    }

    public EmpleadoTurnoDto completar(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.COMPLETED);
    }

    public EmpleadoTurnoDto noShow(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.NO_SHOW);
    }

    private EmpleadoTurnoDto cambiarEstado(Long turnoId, String emailEmpleado, TurnoStatus destino) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        Turno turno = buscarPropio(turnoId, empleado.getEmpleadoId());
        if (!transicionValida(turno.getStatus(), destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del turno de " + turno.getStatus() + " a " + destino);
        }
        turno.setStatus(destino);
        return aDto(turnoRepository.save(turno));
    }

    private boolean transicionValida(TurnoStatus actual, TurnoStatus destino) {
        return switch (destino) {
            case CONFIRMED -> actual == TurnoStatus.PENDING;
            case CANCELLED -> actual == TurnoStatus.PENDING
                    || actual == TurnoStatus.CONFIRMED
                    || actual == TurnoStatus.IN_PROGRESS;
            case COMPLETED -> actual == TurnoStatus.CONFIRMED || actual == TurnoStatus.IN_PROGRESS;
            case NO_SHOW -> actual == TurnoStatus.CONFIRMED;
            default -> false;
        };
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

    private Turno buscarPropio(Long turnoId, Long empleadoId) {
        return turnoRepository.findByTurnoIdAndEmpleado_EmpleadoId(turnoId, empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el turno asignado"));
    }

    private EmpleadoTurnoDto aDto(Turno turno) {
        Usuario cliente = turno.getCliente();
        return new EmpleadoTurnoDto(turno.getTurnoId(), turno.getNegocio().getNegocioId(), turno.getNegocio().getNombre(),
                cliente.getUsuarioId(), cliente.getNombre(), cliente.getApellido(), turno.getEmpleado().getEmpleadoId(),
                turno.getServicio().getServicioId(), turno.getServicio().getNombre(), turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(), turno.getStatus(), turno.getPrecio(), turno.getObservaciones());
    }
}
