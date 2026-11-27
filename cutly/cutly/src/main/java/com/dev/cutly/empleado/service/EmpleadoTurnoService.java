package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.empleado.dto.turno.CrearEmpleadoTurnoRequestDto;
import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.cliente.service.ClienteLocalService;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.empleado.dto.turno.EmpleadoTurnoDto;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.remuneracion.service.RemuneracionService;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
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
    private final NotificacionTurnoService notificacionTurnoService;
    private final ServicioRepository servicioRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;
    private final ClienteLocalRepository clienteLocalRepository;
    private final ClienteLocalService clienteLocalService;
    private final TurnoHorarioValidator horarioValidator;
    private final RemuneracionService remuneracionService;

    public EmpleadoTurnoService(TurnoRepository turnoRepository, EmpleadoRepository empleadoRepository,
                                UsuarioRepository usuarioRepository, NotificacionTurnoService notificacionTurnoService,
                                ServicioRepository servicioRepository, EmpleadoServiciorRepository empleadoServicioRepository,
                                ClienteLocalRepository clienteLocalRepository, ClienteLocalService clienteLocalService,
                                TurnoHorarioValidator horarioValidator, RemuneracionService remuneracionService) {
        this.turnoRepository = turnoRepository;
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionTurnoService = notificacionTurnoService;
        this.servicioRepository = servicioRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
        this.clienteLocalRepository = clienteLocalRepository;
        this.clienteLocalService = clienteLocalService;
        this.horarioValidator = horarioValidator;
        this.remuneracionService = remuneracionService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EmpleadoTurnoDto crear(CrearEmpleadoTurnoRequestDto request, String emailEmpleado) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        var negocio = empleado.getNegocio();
        Long negocioId = negocio.getNegocioId();
        if (negocio.getStatus() != NegocioStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el negocio activo solicitado");
        }
        if (request == null || (request.clienteLocalId() != null) == (request.clienteNuevo() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar exactamente un cliente local existente o nuevo");
        }
        Servicio servicio = servicioRepository.findByServicioIdAndNegocio_NegocioId(request.servicioId(), negocioId)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el servicio activo solicitado"));
        if (servicio.getDuracionMinutos() == null || servicio.getDuracionMinutos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La duración del servicio debe ser positiva");
        }
        ClienteLocal cliente = request.clienteLocalId() == null ? null
                : clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(request.clienteLocalId(), negocioId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No se encontró el cliente local solicitado"));

        Empleado bloqueado = empleadoRepository.findWithLockingByEmpleadoId(empleado.getEmpleadoId())
                .filter(Empleado::isActivo)
                .filter(e -> e.getNegocio().getNegocioId().equals(negocioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "El empleado ya no está disponible"));
        if (empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(
                bloqueado.getEmpleadoId(), servicio.getServicioId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado no está asignado al servicio solicitado");
        }
        var inicio = request.fechaHoraInicio();
        if (inicio == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La fecha y hora de inicio son obligatorias");
        var fin = inicio.plusMinutes(servicio.getDuracionMinutos());
        horarioValidator.validar(negocioId, inicio, fin);
        if (!turnoRepository.findTurnosSolapadosPorEmpleado(bloqueado.getEmpleadoId(), inicio, fin).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El horario solicitado ya no está disponible");
        }
        if (cliente == null) {
            var nuevo = request.clienteNuevo();
            cliente = clienteLocalService.registrar(negocio, nuevo.nombre(), nuevo.apellido(), nuevo.dni(), nuevo.telefono());
        }
        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        turno.setStatus(TurnoStatus.CONFIRMED);
        turno.setPrecio(servicio.getPrecio());
        turno.setClienteLocal(cliente);
        turno.setNegocio(negocio);
        turno.setEmpleado(bloqueado);
        turno.setServicio(servicio);
        Turno guardado = turnoRepository.save(turno);
        notificacionTurnoService.notificarTurnoCreadoPorEmpleado(guardado);
        return aDto(guardado);
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

    public EmpleadoTurnoDto iniciar(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.IN_PROGRESS, true);
    }

    public EmpleadoTurnoDto noShow(Long turnoId, String emailEmpleado) {
        return cambiarEstado(turnoId, emailEmpleado, TurnoStatus.NO_SHOW);
    }

    private EmpleadoTurnoDto cambiarEstado(Long turnoId, String emailEmpleado, TurnoStatus destino) {
        return cambiarEstado(turnoId, emailEmpleado, destino, false);
    }

    private EmpleadoTurnoDto cambiarEstado(Long turnoId, String emailEmpleado, TurnoStatus destino, boolean idempotente) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        Turno turno = destino == TurnoStatus.COMPLETED
                ? turnoRepository.findWithLockingByTurnoIdAndEmpleadoId(turnoId, empleado.getEmpleadoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No se encontró el turno solicitado"))
                : buscarPropio(turnoId, empleado.getEmpleadoId());
        if (idempotente && turno.getStatus() == destino) return aDto(turno);
        if (!transicionValida(turno.getStatus(), destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del turno de " + turno.getStatus() + " a " + destino);
        }
        TurnoStatus anterior = turno.getStatus();
        turno.setStatus(destino);
        notificacionTurnoService.notificarCambioEstado(turno, anterior, destino, empleado.getUsuario());
        Turno guardado = turnoRepository.save(turno);
        if (destino == TurnoStatus.COMPLETED) remuneracionService.asegurarPendiente(guardado);
        return aDto(guardado);
    }

    private boolean transicionValida(TurnoStatus actual, TurnoStatus destino) {
        return switch (destino) {
            case CONFIRMED -> actual == TurnoStatus.PENDING;
            case CANCELLED -> actual == TurnoStatus.PENDING
                    || actual == TurnoStatus.CONFIRMED
                    || actual == TurnoStatus.IN_PROGRESS;
            case COMPLETED -> actual == TurnoStatus.CONFIRMED || actual == TurnoStatus.IN_PROGRESS;
            case NO_SHOW -> actual == TurnoStatus.CONFIRMED;
            case IN_PROGRESS -> actual == TurnoStatus.CONFIRMED;
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
        var clienteLocal = turno.getClienteLocal();
        return new EmpleadoTurnoDto(turno.getTurnoId(), turno.getNegocio().getNegocioId(), turno.getNegocio().getNombre(),
                cliente == null ? null : cliente.getUsuarioId(),
                cliente != null ? cliente.getNombre() : clienteLocal.getNombre(),
                cliente != null ? cliente.getApellido() : clienteLocal.getApellido(),
                turno.getEmpleado().getEmpleadoId(),
                turno.getServicio().getServicioId(), turno.getServicio().getNombre(), turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(), turno.getStatus(), turno.getPrecio(), turno.getObservaciones(),
                clienteLocal == null ? null : clienteLocal.getClienteLocalId(),
                cliente == null ? TipoClienteTurno.LOCAL : TipoClienteTurno.USUARIO);
    }
}
