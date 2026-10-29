package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.owner.dto.turno.CrearOwnerTurnoManualRequestDto;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.enums.TipoClienteTurno;
import com.dev.cutly.turno.enums.TipoResponsableTurno;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class OwnerTurnosService {

    private final TurnoRepository turnoRepository;
    private final OwnerNegocioService ownerNegocioService;
    private final ClienteLocalRepository clienteLocalRepository;
    private final OwnerClienteLocalService ownerClienteLocalService;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurnoHorarioValidator horarioValidator;

    public OwnerTurnosService(
            TurnoRepository turnoRepository,
            OwnerNegocioService ownerNegocioService,
            ClienteLocalRepository clienteLocalRepository,
            OwnerClienteLocalService ownerClienteLocalService,
            ServicioRepository servicioRepository,
            EmpleadoRepository empleadoRepository,
            EmpleadoServiciorRepository empleadoServicioRepository,
            UsuarioRepository usuarioRepository,
            TurnoHorarioValidator horarioValidator
    ) {
        this.turnoRepository = turnoRepository;
        this.ownerNegocioService = ownerNegocioService;
        this.clienteLocalRepository = clienteLocalRepository;
        this.ownerClienteLocalService = ownerClienteLocalService;
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioValidator = horarioValidator;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OwnerTurnoDto crearManual(
            Long negocioId,
            CrearOwnerTurnoManualRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        if (negocio.getStatus() != NegocioStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el negocio activo solicitado");
        }
        if ((request.clienteLocalId() != null) == (request.clienteNuevo() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar exactamente un cliente local existente o nuevo");
        }

        Servicio servicio = servicioRepository
                .findByServicioIdAndNegocio_NegocioId(request.servicioId(), negocioId)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el servicio activo solicitado"));
        if (request.fechaHoraInicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha y hora de inicio son obligatorias");
        }
        if (servicio.getDuracionMinutos() == null || servicio.getDuracionMinutos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La duración del servicio debe ser positiva");
        }

        ClienteLocal clienteLocalExistente = request.clienteLocalId() == null ? null
                : clienteLocalRepository.findByClienteLocalIdAndNegocio_NegocioId(
                                request.clienteLocalId(), negocioId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No se encontró el cliente local solicitado"));

        Empleado empleado = null;
        Usuario ownerResponsable = null;
        if (request.empleadoId() == null) {
            Long ownerId = negocio.getOwner().getUsuarioId();
            ownerResponsable = usuarioRepository.findWithLockingByUsuarioId(ownerId)
                    .filter(usuario -> usuario.getRol() == Rol.OWNER)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "No se pudo bloquear al owner autenticado"));
        } else {
            Empleado candidato = empleadoRepository
                    .findByEmpleadoIdAndNegocio_NegocioId(request.empleadoId(), negocioId)
                    .filter(Empleado::isActivo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No se encontró el empleado activo solicitado"));
            validarAsignacionServicio(candidato, servicio);
            empleado = empleadoRepository.findWithLockingByEmpleadoId(candidato.getEmpleadoId())
                    .filter(Empleado::isActivo)
                    .filter(bloqueado -> bloqueado.getNegocio().getNegocioId().equals(negocioId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "El empleado ya no está disponible"));
            validarAsignacionServicio(empleado, servicio);
        }

        var inicio = request.fechaHoraInicio();
        var fin = inicio.plusMinutes(servicio.getDuracionMinutos());
        horarioValidator.validar(negocioId, inicio, fin);
        boolean ocupado = empleado == null
                ? !turnoRepository.findTurnosSolapadosPorOwner(
                        ownerResponsable.getUsuarioId(), inicio, fin).isEmpty()
                : !turnoRepository.findTurnosSolapadosPorEmpleado(
                        empleado.getEmpleadoId(), inicio, fin).isEmpty();
        if (ocupado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El horario solicitado ya no está disponible");
        }

        ClienteLocal clienteLocal = clienteLocalExistente;
        if (clienteLocal == null) {
            var clienteDto = ownerClienteLocalService.registrar(
                    negocioId, request.clienteNuevo(), emailOwner
            );
            clienteLocal = clienteLocalRepository.getReferenceById(clienteDto.clienteLocalId());
        }

        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(fin);
        turno.setStatus(TurnoStatus.CONFIRMED);
        turno.setPrecio(servicio.getPrecio());
        turno.setClienteLocal(clienteLocal);
        turno.setNegocio(negocio);
        turno.setEmpleado(empleado);
        turno.setOwnerResponsable(ownerResponsable);
        turno.setServicio(servicio);
        turno.setObservaciones(request.observaciones());
        return aDto(turnoRepository.save(turno));
    }

    private void validarAsignacionServicio(Empleado empleado, Servicio servicio) {
        if (empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(
                empleado.getEmpleadoId(), servicio.getServicioId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado no está asignado al servicio solicitado");
        }
    }

    public List<OwnerTurnoDto> listarTurnos(
            Long negocioId,
            LocalDate fecha,
            Long empleadoId,
            TurnoStatus status,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Specification<Turno> filtros = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("negocio").get("negocioId"), negocioId));
            if (fecha != null) {
                LocalDateTime desde = fecha.atStartOfDay();
                LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaHoraInicio"), desde));
                predicates.add(criteriaBuilder.lessThan(root.get("fechaHoraInicio"), hasta));
            }
            if (empleadoId != null) {
                predicates.add(criteriaBuilder.equal(root.get("empleado").get("empleadoId"), empleadoId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return turnoRepository.findAll(filtros, Sort.by(
                        Sort.Order.asc("fechaHoraInicio"), Sort.Order.asc("turnoId")
                ))
                .stream().map(this::aDto).toList();
    }

    public OwnerTurnoDto obtenerTurno(Long negocioId, Long turnoId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return aDto(buscarTurno(negocioId, turnoId));
    }

    public OwnerTurnoDto confirmar(Long negocioId, Long turnoId, String emailOwner) {
        return cambiarEstado(negocioId, turnoId, TurnoStatus.CONFIRMED, emailOwner);
    }

    public OwnerTurnoDto rechazar(Long negocioId, Long turnoId, String emailOwner) {
        return cambiarEstado(negocioId, turnoId, TurnoStatus.REJECTED, emailOwner);
    }

    public OwnerTurnoDto cancelar(Long negocioId, Long turnoId, String emailOwner) {
        return cambiarEstado(negocioId, turnoId, TurnoStatus.CANCELLED, emailOwner);
    }

    public OwnerTurnoDto completar(Long negocioId, Long turnoId, String emailOwner) {
        return cambiarEstado(negocioId, turnoId, TurnoStatus.COMPLETED, emailOwner);
    }

    public OwnerTurnoDto marcarNoShow(Long negocioId, Long turnoId, String emailOwner) {
        return cambiarEstado(negocioId, turnoId, TurnoStatus.NO_SHOW, emailOwner);
    }

    private OwnerTurnoDto cambiarEstado(
            Long negocioId,
            Long turnoId,
            TurnoStatus destino,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Turno turno = buscarTurno(negocioId, turnoId);
        TurnoStatus actual = turno.getStatus();
        if (actual == destino) return aDto(turno);
        if (!transicionValida(actual, destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del turno de " + actual + " a " + destino);
        }
        turno.setStatus(destino);
        return aDto(turnoRepository.save(turno));
    }

    private boolean transicionValida(TurnoStatus actual, TurnoStatus destino) {
        return switch (destino) {
            case CONFIRMED, REJECTED -> actual == TurnoStatus.PENDING;
            case CANCELLED -> actual == TurnoStatus.PENDING
                    || actual == TurnoStatus.CONFIRMED
                    || actual == TurnoStatus.IN_PROGRESS;
            case COMPLETED -> actual == TurnoStatus.CONFIRMED || actual == TurnoStatus.IN_PROGRESS;
            case NO_SHOW -> actual == TurnoStatus.CONFIRMED;
            default -> false;
        };
    }

    private Turno buscarTurno(Long negocioId, Long turnoId) {
        return turnoRepository.findByTurnoIdAndNegocio_NegocioId(turnoId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el turno solicitado"));
    }

    private OwnerTurnoDto aDto(Turno turno) {
        Usuario cliente = turno.getCliente();
        var clienteLocal = turno.getClienteLocal();
        Usuario empleado = turno.getEmpleado() == null ? null : turno.getEmpleado().getUsuario();
        Usuario ownerResponsable = turno.getOwnerResponsable();
        TipoClienteTurno tipoCliente = cliente != null ? TipoClienteTurno.USUARIO : TipoClienteTurno.LOCAL;
        TipoResponsableTurno tipoResponsable = turno.getEmpleado() != null
                ? TipoResponsableTurno.EMPLEADO
                : TipoResponsableTurno.OWNER;
        return new OwnerTurnoDto(
                turno.getTurnoId(),
                turno.getNegocio().getNegocioId(),
                cliente == null ? null : cliente.getUsuarioId(),
                cliente != null ? cliente.getNombre() : clienteLocal.getNombre(),
                cliente != null ? cliente.getApellido() : clienteLocal.getApellido(),
                turno.getEmpleado() == null ? null : turno.getEmpleado().getEmpleadoId(),
                empleado == null ? null : empleado.getNombre(),
                empleado == null ? null : empleado.getApellido(),
                turno.getServicio().getServicioId(),
                turno.getServicio().getNombre(),
                turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(),
                turno.getStatus(),
                turno.getPrecio(),
                turno.getObservaciones(),
                clienteLocal == null ? null : clienteLocal.getClienteLocalId(),
                tipoCliente,
                ownerResponsable == null ? null : ownerResponsable.getUsuarioId(),
                tipoResponsable,
                empleado != null ? empleado.getNombre() : ownerResponsable.getNombre(),
                empleado != null ? empleado.getApellido() : ownerResponsable.getApellido()
        );
    }
}
