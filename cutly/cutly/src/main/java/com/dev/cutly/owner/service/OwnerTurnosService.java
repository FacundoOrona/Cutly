package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.notificacion.service.NotificacionTurnoService;
import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.owner.dto.turno.ResumenFinancieroTurnoDto;
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
import com.dev.cutly.pago.entity.*;
import com.dev.cutly.pago.enums.DecisionFinanciera;
import com.dev.cutly.pago.enums.MetodoPago;
import com.dev.cutly.pago.repository.*;
import com.dev.cutly.owner.dto.turno.DecisionFinancieraRequestDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

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
    private final ResultadoFinancieroTurnoRepository resultadoRepository;
    private final DeudaClienteRepository deudaRepository;
    private final PagoRepository pagoRepository;
    private final IdempotenciaFinancieraRepository idempotenciaRepository;
    private final NotificacionTurnoService notificacionTurnoService;

    @Autowired
    public OwnerTurnosService(
            TurnoRepository turnoRepository,
            OwnerNegocioService ownerNegocioService,
            ClienteLocalRepository clienteLocalRepository,
            OwnerClienteLocalService ownerClienteLocalService,
            ServicioRepository servicioRepository,
            EmpleadoRepository empleadoRepository,
            EmpleadoServiciorRepository empleadoServicioRepository,
            UsuarioRepository usuarioRepository,
            TurnoHorarioValidator horarioValidator,
            ResultadoFinancieroTurnoRepository resultadoRepository,
            DeudaClienteRepository deudaRepository,
            PagoRepository pagoRepository,
            IdempotenciaFinancieraRepository idempotenciaRepository,
            NotificacionTurnoService notificacionTurnoService
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
        this.resultadoRepository = Objects.requireNonNull(resultadoRepository, "resultadoRepository es obligatorio");
        this.deudaRepository = deudaRepository;
        this.pagoRepository = pagoRepository;
        this.idempotenciaRepository = idempotenciaRepository;
        this.notificacionTurnoService = notificacionTurnoService;
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
        Turno turnoGuardado = turnoRepository.save(turno);
        notificacionTurnoService.notificarNuevoTurnoAsignado(turnoGuardado);
        return aDto(turnoGuardado);
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
        List<Turno> turnos = turnoRepository.findAll(filtros, Sort.by(
                        Sort.Order.asc("fechaHoraInicio"), Sort.Order.asc("turnoId")
                ));
        if (turnos.isEmpty()) return List.of();
        var turnoIds = turnos.stream().map(Turno::getTurnoId).toList();
        var resumenes = cargarResumenes(turnoIds);
        return turnos.stream().map(turno -> aDto(turno, resumenes.get(turno.getTurnoId()))).toList();
    }

    public OwnerTurnoDto obtenerTurno(Long negocioId, Long turnoId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Turno turno = buscarTurno(negocioId, turnoId);
        return aDto(turno, cargarResumenes(List.of(turno.getTurnoId())).get(turno.getTurnoId()));
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La finalización financiera requiere cuerpo e Idempotency-Key");
    }

    public OwnerTurnoDto completarConDecision(Long negocioId, Long turnoId, DecisionFinancieraRequestDto request, String key, String emailOwner) {
        return finalizarFinanzas(negocioId, turnoId, request, key, emailOwner, true);
    }

    public OwnerTurnoDto regularizarFinanzas(Long negocioId, Long turnoId, DecisionFinancieraRequestDto request, String key, String emailOwner) {
        return finalizarFinanzas(negocioId, turnoId, request, key, emailOwner, false);
    }

    private OwnerTurnoDto finalizarFinanzas(Long negocioId, Long turnoId, DecisionFinancieraRequestDto request, String key, String emailOwner, boolean completarTurno) {
        if (key == null || key.isBlank() || key.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y admite hasta 100 caracteres");
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Turno turno = turnoRepository.findWithLockingByTurnoIdAndNegocioId(turnoId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el turno solicitado"));
        validarDecision(request);
        String hash = hashSolicitud(request);
        var previa = idempotenciaRepository.findByKey(key);
        if (previa.isPresent()) {
            if (!previa.get().getNegocio().getNegocioId().equals(negocioId)
                    || !previa.get().getTurno().getTurnoId().equals(turnoId)
                    || !previa.get().getRequestHash().equals(hash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó con otra operación");
            }
            return aDto(turno);
        }
        if (resultadoRepository.existsByTurno_TurnoId(turnoId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya tiene regularización financiera");
        if (completarTurno && !transicionValida(turno.getStatus(), TurnoStatus.COMPLETED)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El estado actual del turno no permite completarlo");
        if (!completarTurno && turno.getStatus() != TurnoStatus.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede regularizar un turno completado");
        BigDecimal monto = request.decisionFinanciera() == DecisionFinanciera.GRATUIDAD ? BigDecimal.ZERO : request.monto();
        ResultadoFinancieroTurno resultado = new ResultadoFinancieroTurno();
        resultado.setTurno(turno); resultado.setNegocio(negocio); resultado.setCliente(turno.getCliente()); resultado.setClienteLocal(turno.getClienteLocal());
        resultado.setOwner(negocio.getOwner()); resultado.setTipoResultado(request.decisionFinanciera()); resultado.setMontoAcordado(monto);
        resultado.setFechaRegistro(LocalDateTime.now()); resultado.setMotivoGratuidad(request.motivoGratuidad() == null ? null : request.motivoGratuidad().trim());
        resultado = resultadoRepository.save(resultado);
        if (request.decisionFinanciera() == DecisionFinanciera.CUENTA_CORRIENTE) {
            DeudaCliente deuda = new DeudaCliente(); deuda.setResultadoFinanciero(resultado); deuda.setNegocio(negocio); deuda.setCliente(turno.getCliente()); deuda.setClienteLocal(turno.getClienteLocal()); deuda.setMontoOriginal(monto); deuda.setFechaOrigen(LocalDateTime.now()); deuda = deudaRepository.save(deuda);
            if (request.pagoInicial().signum() > 0) {
                Pago pago = new Pago(); pago.setDeuda(deuda); pago.setMonto(request.pagoInicial()); pago.setMetodoPago(request.metodoPago()); pago.setDescripcionOtro(request.descripcionOtro()); pago.setFechaPago(LocalDateTime.now()); pago.setOwnerRegistrante(negocio.getOwner()); pagoRepository.save(pago);
            }
        } else if (request.decisionFinanciera() == DecisionFinanciera.COBRO_DIRECTO) {
            Pago pago = new Pago(); pago.setTurno(turno); pago.setResultadoFinanciero(resultado); pago.setMonto(monto); pago.setMetodoPago(request.metodoPago()); pago.setDescripcionOtro(request.descripcionOtro()); pago.setFechaPago(LocalDateTime.now()); pago.setOwnerRegistrante(negocio.getOwner()); pagoRepository.save(pago);
        }
        IdempotenciaFinanciera idem = new IdempotenciaFinanciera(); idem.setKey(key); idem.setRequestHash(hash); idem.setNegocio(negocio); idem.setTurno(turno); idem.setResultado(resultado);
        try { idempotenciaRepository.saveAndFlush(idem); }
        catch (DataIntegrityViolationException conflict) { throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó en otra solicitud"); }
        if (completarTurno) turno.setStatus(TurnoStatus.COMPLETED);
        return aDto(turnoRepository.save(turno));
    }

    private void validarDecision(DecisionFinancieraRequestDto r) {
        if (r == null || r.decisionFinanciera() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La decisión financiera es obligatoria");
        switch (r.decisionFinanciera()) {
            case COBRO_DIRECTO -> { if (r.monto() == null || r.monto().signum() <= 0 || r.pagoInicial() != null || r.metodoPago() == null || r.motivoGratuidad() != null || !descripcionPagoValida(r)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos para cobro directo"); }
            case CUENTA_CORRIENTE -> { if (r.monto() == null || r.monto().signum() <= 0 || r.pagoInicial() == null || r.pagoInicial().signum() < 0 || r.pagoInicial().compareTo(r.monto()) > 0 || (r.pagoInicial().signum() == 0 && (r.metodoPago() != null || r.descripcionOtro() != null)) || (r.pagoInicial().signum() > 0 && (r.metodoPago() == null || !descripcionPagoValida(r))) || r.motivoGratuidad() != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos para cuenta corriente"); }
            case GRATUIDAD -> { if (r.monto() != null || r.pagoInicial() != null || r.metodoPago() != null || r.descripcionOtro() != null || r.motivoGratuidad() == null || r.motivoGratuidad().trim().length() < 3 || r.motivoGratuidad().trim().length() > 250) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos para gratuidad"); }
        }
    }

    private boolean descripcionPagoValida(DecisionFinancieraRequestDto r) {
        return (r.metodoPago() == MetodoPago.OTRO && r.descripcionOtro() != null && !r.descripcionOtro().isBlank() && r.descripcionOtro().length() <= 100)
                || (r.metodoPago() != MetodoPago.OTRO && r.descripcionOtro() == null);
    }

    private String hashSolicitud(DecisionFinancieraRequestDto r) {
        try { var bytes = MessageDigest.getInstance("SHA-256").digest((r.decisionFinanciera()+"|"+r.monto()+"|"+r.pagoInicial()+"|"+r.metodoPago()+"|"+r.descripcionOtro()+"|"+(r.motivoGratuidad()==null?null:r.motivoGratuidad().trim())).getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(bytes); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
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
        return aDto(turno, cargarResumenes(List.of(turno.getTurnoId())).get(turno.getTurnoId()));
    }

    private OwnerTurnoDto aDto(Turno turno, boolean regularizadoFinancieramente) {
        ResumenFinancieroTurnoDto resumen = cargarResumenes(List.of(turno.getTurnoId())).get(turno.getTurnoId());
        return aDto(turno, regularizadoFinancieramente ? resumen : null);
    }

    private OwnerTurnoDto aDto(Turno turno, ResumenFinancieroTurnoDto resumenFinanciero) {
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
                empleado != null ? empleado.getApellido() : ownerResponsable.getApellido(),
                resumenFinanciero != null,
                resumenFinanciero
        );
    }

    private Map<Long, ResumenFinancieroTurnoDto> cargarResumenes(List<Long> turnoIds) {
        Map<Long, ResumenFinancieroTurnoDto> resumenes = new HashMap<>();
        for (Object[] fila : resultadoRepository.findResumenesBasePorTurnoIds(turnoIds)) {
            Long turnoId = (Long) fila[0];
            DecisionFinanciera decision = (DecisionFinanciera) fila[1];
            BigDecimal monto = (BigDecimal) fila[2];
            if (decision == DecisionFinanciera.GRATUIDAD) {
                resumenes.put(turnoId, new ResumenFinancieroTurnoDto(decision, null, null, null, null, null));
            } else if (decision == DecisionFinanciera.COBRO_DIRECTO) {
                resumenes.put(turnoId, new ResumenFinancieroTurnoDto(decision, monto, null, null, null, monto));
            } else {
                resumenes.put(turnoId, new ResumenFinancieroTurnoDto(decision, monto, null, BigDecimal.ZERO, monto, monto));
            }
        }
        Set<Long> turnosCobroDirecto = new java.util.HashSet<>();
        Set<Long> turnosCuentaCorriente = new java.util.HashSet<>();
        resumenes.forEach((turnoId, resumen) -> {
            if (resumen.decisionFinanciera() == DecisionFinanciera.COBRO_DIRECTO) turnosCobroDirecto.add(turnoId);
            if (resumen.decisionFinanciera() == DecisionFinanciera.CUENTA_CORRIENTE) turnosCuentaCorriente.add(turnoId);
        });
        if (!turnosCobroDirecto.isEmpty()) for (Object[] fila : pagoRepository.findCobrosDirectosPorTurnoIds(turnosCobroDirecto)) {
            Long turnoId = (Long) fila[0];
            ResumenFinancieroTurnoDto previo = resumenes.get(turnoId);
            if (previo != null && previo.decisionFinanciera() == DecisionFinanciera.COBRO_DIRECTO) {
                resumenes.put(turnoId, new ResumenFinancieroTurnoDto(previo.decisionFinanciera(),
                        (BigDecimal) fila[1], (MetodoPago) fila[2], null, null, previo.montoAcordado()));
            }
        }
        if (!turnosCuentaCorriente.isEmpty()) for (Object[] fila : pagoRepository.findDeudasConAbonosPorTurnoIds(turnosCuentaCorriente)) {
            Long turnoId = (Long) fila[0];
            ResumenFinancieroTurnoDto previo = resumenes.get(turnoId);
            if (previo != null && previo.decisionFinanciera() == DecisionFinanciera.CUENTA_CORRIENTE) {
                BigDecimal totalAbonado = (BigDecimal) fila[2];
                resumenes.put(turnoId, new ResumenFinancieroTurnoDto(previo.decisionFinanciera(),
                        previo.montoAcordado(), null, totalAbonado,
                        ((BigDecimal) fila[1]).subtract(totalAbonado), previo.montoAcordado()));
            }
        }
        return resumenes;
    }
}
