package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.negocio.ClienteEmpleadoDto;
import com.dev.cutly.cliente.dto.negocio.ClienteNegocioDto;
import com.dev.cutly.cliente.dto.negocio.ClienteReviewDto;
import com.dev.cutly.cliente.dto.negocio.ClienteServicioDto;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.DireccionNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.DireccionRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.review.repository.ReviewRepository;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClienteNegocioService {

    private final NegocioRepository negocioRepository;
    private final DireccionRepository direccionRepository;
    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ReviewRepository reviewRepository;
    private final ControlAccesoSuscripcion controlAccesoSuscripcion;
    private final TurnoRepository turnoRepository;

    public ClienteNegocioService(
            NegocioRepository negocioRepository,
            DireccionRepository direccionRepository,
            ServicioRepository servicioRepository,
            EmpleadoRepository empleadoRepository,
            ReviewRepository reviewRepository,
            ControlAccesoSuscripcion controlAccesoSuscripcion,
            TurnoRepository turnoRepository
    ) {
        this.negocioRepository = negocioRepository;
        this.direccionRepository = direccionRepository;
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
        this.reviewRepository = reviewRepository;
        this.controlAccesoSuscripcion = controlAccesoSuscripcion;
        this.turnoRepository = turnoRepository;
    }

    public Page<ClienteNegocioDto> listar(String nombre, Pageable pageable) {
        return negocioRepository
                .findDisponiblesParaClientes(
                        NegocioStatus.ACTIVE,
                        nombre == null ? "" : nombre.trim(),
                        RelojSuscripcion.fechaActual(),
                        pageable
                )
                .map(this::aDto);
    }

    public List<ClienteNegocioDto> top10() {
        LocalDateTime hasta = RelojSuscripcion.fechaHoraActual();
        LocalDateTime desde = hasta.minusDays(30);
        List<Object[]> ranking = turnoRepository.rankingNegociosClientes(
                desde, hasta, RelojSuscripcion.fechaActual(), PageRequest.of(0, 10));
        List<Negocio> elegibles = ranking.stream().map(row -> (Negocio) row[0]).toList();
        if (elegibles.isEmpty()) return List.of();
        Map<Long, DireccionNegocio> direcciones = direccionRepository
                .findAllByNegocio_NegocioIdIn(elegibles.stream().map(Negocio::getNegocioId).toList())
                .stream().collect(Collectors.toMap(d -> d.getNegocio().getNegocioId(), Function.identity()));
        return elegibles.stream().map(n -> aDto(n, direcciones.get(n.getNegocioId()))).toList();
    }
    public ClienteNegocioDto obtener(Long id) {
        return aDto(buscar(id));
    }

    public List<ClienteServicioDto> servicios(Long id) {
        buscar(id);

        return servicioRepository
                .findByNegocio_NegocioIdAndActivoTrueOrderByServicioIdAsc(id)
                .stream()
                .map(s -> new ClienteServicioDto(
                        s.getServicioId(),
                        id,
                        s.getNombre(),
                        s.getDescripcion(),
                        s.getPrecio(),
                        s.getDuracionMinutos()
                ))
                .toList();
    }

    public List<ClienteEmpleadoDto> empleados(Long id) {
        buscar(id);

        return empleadoRepository
                .findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(id)
                .stream()
                .map(e -> new ClienteEmpleadoDto(
                        e.getEmpleadoId(),
                        id,
                        e.getUsuario().getNombre(),
                        e.getUsuario().getApellido()
                ))
                .toList();
    }

    public List<ClienteReviewDto> reviews(Long id) {
        buscar(id);

        return reviewRepository
                .findByNegocio_NegocioIdOrderByReviewIdDesc(id)
                .stream()
                .map(r -> new ClienteReviewDto(
                        r.getReviewId(),
                        r.getPuntuacion(),
                        r.getComentario(),
                        r.getCliente().getUsuarioId(),
                        r.getCliente().getNombre(),
                        id
                ))
                .toList();
    }

    private Negocio buscar(Long id) {
        Negocio negocio = negocioRepository
                .findByNegocioIdAndStatus(id, NegocioStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"
                ));
        if (!controlAccesoSuscripcion.clientePuedeConsultarNegocio(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No se encontró el negocio solicitado");
        }
        return negocio;
    }

    private ClienteNegocioDto aDto(Negocio n) {
        DireccionNegocio d = direccionRepository.findByNegocio_NegocioId(n.getNegocioId()).orElse(null);
        return aDto(n, d);
    }

    private ClienteNegocioDto aDto(Negocio n, DireccionNegocio d) {
        return new ClienteNegocioDto(
                n.getNegocioId(), n.getNombre(), n.getDescripcion(), n.getTelefono(), n.getEmail(), n.getStatus(),
                d == null ? null : d.getCalle(), d == null ? null : d.getNumero(),
                d == null ? null : d.getCiudad(), d == null ? null : d.getProvincia(),
                d == null ? null : d.getCodigoPostal()
        );
    }
}
