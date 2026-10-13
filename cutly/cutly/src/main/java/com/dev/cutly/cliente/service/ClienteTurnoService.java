package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.service.ClienteTurnoDto;
import com.dev.cutly.cliente.dto.service.CrearTurnoRequestDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
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
public class ClienteTurnoService {
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;
    private final HorarioNegocioRepository horarioRepository;

    public ClienteTurnoService(TurnoRepository turnoRepository, UsuarioRepository usuarioRepository,
                               NegocioRepository negocioRepository, ServicioRepository servicioRepository,
                               EmpleadoRepository empleadoRepository, EmpleadoServiciorRepository empleadoServicioRepository,
                               HorarioNegocioRepository horarioRepository) {
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
        this.horarioRepository = horarioRepository;
    }

    public ClienteTurnoDto crear(CrearTurnoRequestDto request, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        Negocio negocio = negocioRepository.findByNegocioIdAndStatus(request.negocioId(), NegocioStatus.ACTIVE)
                .orElseThrow(() -> noEncontrado("No se encontró el negocio activo solicitado"));
        Servicio servicio = servicioRepository.findByServicioIdAndNegocio_NegocioId(request.servicioId(), negocio.getNegocioId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> noEncontrado("No se encontró el servicio activo solicitado"));
        LocalDateTime inicio = request.fechaHoraInicio();
        LocalDateTime fin = inicio.plusMinutes(servicio.getDuracionMinutos());
        validarHorario(negocio.getNegocioId(), inicio, fin);

        Empleado empleado = seleccionarEmpleado(negocio.getNegocioId(), servicio.getServicioId(), request.empleadoId(), inicio, fin);
        if (haySolapamientoEmpleado(empleado.getEmpleadoId(), inicio, fin)
                || haySolapamientoCliente(cliente.getUsuarioId(), inicio, fin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El horario solicitado ya no está disponible");
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        turno.setStatus(TurnoStatus.PENDING);
        turno.setPrecio(servicio.getPrecio());
        turno.setCliente(cliente);
        turno.setNegocio(negocio);
        turno.setEmpleado(empleado);
        turno.setServicio(servicio);
        turno.setObservaciones(request.observaciones());
        return aDto(turnoRepository.save(turno));
    }

    public List<ClienteTurnoDto> listar(String emailCliente, LocalDate fecha, TurnoStatus status, boolean historial) {
        Usuario cliente = obtenerCliente(emailCliente);
        LocalDateTime ahora = LocalDateTime.now();
        Specification<Turno> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("cliente").get("usuarioId"), cliente.getUsuarioId()));
            if (fecha != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaHoraInicio"), fecha.atStartOfDay()));
                predicates.add(cb.lessThan(root.get("fechaHoraInicio"), fecha.plusDays(1).atStartOfDay()));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (historial) {
                Predicate pasado = cb.lessThan(root.get("fechaHoraInicio"), ahora);
                Predicate estadoHistorico = root.get("status").in(TurnoStatus.COMPLETED, TurnoStatus.CANCELLED);
                predicates.add(cb.or(pasado, estadoHistorico));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return turnoRepository.findAll(filtros, Sort.by(Sort.Order.desc("fechaHoraInicio"), Sort.Order.desc("turnoId")))
                .stream().map(this::aDto).toList();
    }

    public ClienteTurnoDto obtener(Long turnoId, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        return aDto(buscarPropio(turnoId, cliente.getUsuarioId()));
    }

    public ClienteTurnoDto cancelar(Long turnoId, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        Turno turno = buscarPropio(turnoId, cliente.getUsuarioId());
        if (turno.getStatus() != TurnoStatus.PENDING && turno.getStatus() != TurnoStatus.CONFIRMED
                && turno.getStatus() != TurnoStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno no puede cancelarse en su estado actual");
        }
        turno.setStatus(TurnoStatus.CANCELLED);
        return aDto(turnoRepository.save(turno));
    }

    private Empleado seleccionarEmpleado(Long negocioId, Long servicioId, Long empleadoId, LocalDateTime inicio, LocalDateTime fin) {
        List<Empleado> candidatos = empleadoId == null
                ? empleadoRepository.findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(negocioId)
                : List.of(empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(empleadoId, negocioId)
                .filter(Empleado::isActivo).orElseThrow(() -> noEncontrado("No se encontró el empleado activo solicitado")));
        return candidatos.stream()
                .filter(e -> empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(e.getEmpleadoId(), servicioId).isPresent())
                .filter(e -> !haySolapamientoEmpleado(e.getEmpleadoId(), inicio, fin))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay empleados disponibles para el horario solicitado"));
    }

    private boolean haySolapamientoEmpleado(Long empleadoId, LocalDateTime inicio, LocalDateTime fin) {
        return turnoRepository.findTurnosSolapadosPorEmpleado(empleadoId, inicio, fin).stream().anyMatch(this::bloquea);
    }

    private boolean haySolapamientoCliente(Long clienteId, LocalDateTime inicio, LocalDateTime fin) {
        return turnoRepository.findTurnosClienteSolapados(clienteId, inicio, fin).stream().anyMatch(this::bloquea);
    }

    private boolean bloquea(Turno turno) {
        return turno.getStatus() != TurnoStatus.CANCELLED && turno.getStatus() != TurnoStatus.REJECTED;
    }

    private void validarHorario(Long negocioId, LocalDateTime inicio, LocalDateTime fin) {
        if (inicio.isBefore(LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha del turno debe ser futura");
        HorarioNegocio horario = horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(negocioId).stream()
                .filter(h -> h.getDiaSemana() == DiaSemana.valueOf(inicio.getDayOfWeek().name())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay horario laboral para la fecha solicitada"));
        if (horario.isCerrado() || inicio.toLocalTime().isBefore(horario.getHoraApertura()) || fin.toLocalTime().isAfter(horario.getHoraCierre()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno está fuera del horario laboral del negocio");
    }

    private Usuario obtenerCliente(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado");
        }
        Usuario cliente = usuarioRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado"));
        if (cliente.getRol() != Rol.CLIENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un cliente");
        return cliente;
    }

    private Turno buscarPropio(Long turnoId, Long clienteId) {
        return turnoRepository.findByTurnoIdAndCliente_UsuarioId(turnoId, clienteId)
                .orElseThrow(() -> noEncontrado("No se encontró el turno solicitado"));
    }

    private ResponseStatusException noEncontrado(String mensaje) { return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje); }

    private ClienteTurnoDto aDto(Turno t) {
        return new ClienteTurnoDto(t.getTurnoId(), t.getNegocio().getNegocioId(), t.getNegocio().getNombre(), t.getCliente().getUsuarioId(), t.getEmpleado().getEmpleadoId(), t.getEmpleado().getUsuario().getNombre(), t.getEmpleado().getUsuario().getApellido(), t.getServicio().getServicioId(), t.getServicio().getNombre(), t.getFechaHoraInicio(), t.getFechaHoraFin(), t.getStatus(), t.getPrecio(), t.getObservaciones());
    }
}
