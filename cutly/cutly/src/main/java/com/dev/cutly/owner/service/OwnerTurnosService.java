package com.dev.cutly.owner.service;

import com.dev.cutly.owner.dto.turno.OwnerTurnoDto;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class OwnerTurnosService {

    private final TurnoRepository turnoRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerTurnosService(TurnoRepository turnoRepository, OwnerNegocioService ownerNegocioService) {
        this.turnoRepository = turnoRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public List<OwnerTurnoDto> listarTurnos(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return turnoRepository.findByNegocio_NegocioIdOrderByFechaHoraInicioAscTurnoIdAsc(negocioId)
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
        Usuario empleado = turno.getEmpleado().getUsuario();
        return new OwnerTurnoDto(
                turno.getTurnoId(),
                turno.getNegocio().getNegocioId(),
                cliente.getUsuarioId(),
                cliente.getNombre(),
                cliente.getApellido(),
                turno.getEmpleado().getEmpleadoId(),
                empleado.getNombre(),
                empleado.getApellido(),
                turno.getServicio().getServicioId(),
                turno.getServicio().getNombre(),
                turno.getFechaHoraInicio(),
                turno.getFechaHoraFin(),
                turno.getStatus(),
                turno.getPrecio(),
                turno.getObservaciones()
        );
    }
}
