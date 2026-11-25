package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.negocio.ActualizarNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.CrearNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.OwnerNegocioDto;
import com.dev.cutly.owner.dto.suscripcion.OwnerSuscripcionDto;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.SnapshotSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.enums.TipoEventoSuscripcion;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.service.HistorialSuscripcionService;
import com.dev.cutly.suscripcion.service.ControlAccesoSuscripcion;
import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
import com.dev.cutly.suscripcion.service.RelojSuscripcion;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class OwnerNegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final PlanSuscripcionService planSuscripcionService;
    private final HistorialSuscripcionService historialSuscripcionService;

    public OwnerNegocioService(
            NegocioRepository negocioRepository,
            UsuarioRepository usuarioRepository,
            SuscripcionRepository suscripcionRepository,
            PlanSuscripcionService planSuscripcionService,
            HistorialSuscripcionService historialSuscripcionService
    ) {
        this.negocioRepository = negocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.planSuscripcionService = planSuscripcionService;
        this.historialSuscripcionService = historialSuscripcionService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OwnerNegocioDto crearNegocio(CrearNegocioRequestDto request, String emailOwner) {
        Usuario ownerAutenticado = obtenerOwnerAutenticado(emailOwner);
        Usuario owner = usuarioRepository.findWithLockingByUsuarioId(ownerAutenticado.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al owner autenticado"));

        long cantidadNegocios = negocioRepository.countByOwner_UsuarioId(owner.getUsuarioId());
        boolean esPrimerNegocio = cantidadNegocios == 0;
        Suscripcion suscripcionExistente = suscripcionRepository
                .findWithLockingByOwner_UsuarioId(owner.getUsuarioId()).orElse(null);
        if (suscripcionExistente == null) {
            if (!esPrimerNegocio || owner.isTrialSuscripcionUtilizado()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El OWNER necesita una suscripción vigente para crear negocios");
            }
        } else {
            Integer maxNegocios = suscripcionExistente.getPlan().getMaxNegocios();
            if (!ControlAccesoSuscripcion.suscripcionHabilitada(suscripcionExistente)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "La suscripción del OWNER no permite crear negocios");
            }
            if (maxNegocios == null || cantidadNegocios >= maxNegocios) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Se alcanzó la cantidad máxima de negocios permitida por el plan");
            }
        }
        Negocio negocio = new Negocio();
        negocio.setNombre(request.nombre());
        negocio.setDescripcion(request.descripcion());
        negocio.setTelefono(request.telefono());
        negocio.setEmail(request.email());
        negocio.setStatus(esPrimerNegocio ? NegocioStatus.ACTIVE : NegocioStatus.INACTIVE);
        negocio.setOwner(owner);
        Negocio negocioGuardado = negocioRepository.save(negocio);

        if (esPrimerNegocio && suscripcionExistente == null) {
            LocalDate fechaInicio = RelojSuscripcion.fechaActual();
            PlanSuscripcion planPrueba = planSuscripcionService.obtenerOCrearPlanPruebaGratuita();

            Suscripcion suscripcion = new Suscripcion();
            suscripcion.setStatus(SuscripcionStatus.TRIAL);
            suscripcion.setFechaInicio(fechaInicio);
            suscripcion.setFechaVencimiento(fechaInicio.plusDays(13));
            suscripcion.setPlan(planPrueba);
            suscripcion.asignarPrecioContratado(planPrueba.getPrecioMensual());
            suscripcion.setOwner(owner);
            owner.setTrialSuscripcionUtilizado(true);
            usuarioRepository.save(owner);
            Suscripcion suscripcionGuardada = suscripcionRepository.save(suscripcion);
            historialSuscripcionService.registrar(
                    suscripcionGuardada,
                    TipoEventoSuscripcion.TRIAL_CREATED,
                    fechaInicio,
                    null,
                    SnapshotSuscripcion.capturar(suscripcionGuardada),
                    null,
                    RelojSuscripcion.IDENTIFICACION_SISTEMA,
                    "Prueba gratuita concedida una sola vez al OWNER al crear su primer negocio",
                    "Creación automática de TRIAL para el OWNER",
                    null
            );
        }

        return aDto(negocioGuardado);
    }

    public List<OwnerNegocioDto> listarNegocios(String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return negocioRepository.findByOwner_UsuarioId(owner.getUsuarioId()).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public OwnerSuscripcionDto consultarSuscripcion(String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        long cantidad = negocioRepository.countByOwner_UsuarioId(owner.getUsuarioId());
        Suscripcion suscripcion = suscripcionRepository.findByOwner_UsuarioId(owner.getUsuarioId()).orElse(null);
        Integer maximo = suscripcion == null ? null : suscripcion.getPlan().getMaxNegocios();
        return new OwnerSuscripcionDto(
                suscripcion == null ? null : suscripcion.getSuscripcionId(),
                suscripcion == null ? null : suscripcion.getStatus(),
                suscripcion == null ? null : suscripcion.getFechaInicio(),
                suscripcion == null ? null : suscripcion.getFechaVencimiento(),
                suscripcion == null ? null : suscripcion.getPrecioContratado(),
                suscripcion == null ? null : suscripcion.getPlan().getPlanId(),
                suscripcion == null ? null : suscripcion.getPlan().getNombre(),
                maximo,
                cantidad,
                maximo == null ? 0 : Math.max(0, maximo - cantidad),
                owner.isTrialSuscripcionUtilizado()
        );
    }

    public OwnerNegocioDto obtenerNegocio(Long id, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return negocioRepository.findByNegocioIdAndOwner_UsuarioId(id, owner.getUsuarioId())
                .map(this::aDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"));
    }

    @Transactional
    public OwnerNegocioDto actualizarNegocio(
            Long id,
            ActualizarNegocioRequestDto request,
            String emailOwner
    ) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Negocio negocio = negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(id, owner.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"));
        if (request.nombre() != null) negocio.setNombre(request.nombre());
        if (request.descripcion() != null) negocio.setDescripcion(request.descripcion());
        if (request.telefono() != null) negocio.setTelefono(request.telefono());
        if (request.email() != null) negocio.setEmail(request.email());
        return aDto(negocioRepository.save(negocio));
    }

    @Transactional
    public OwnerNegocioDto actualizarEstado(Long id, NegocioStatus destino, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Negocio negocio = negocioRepository.findWithLockingByNegocioIdAndOwner_UsuarioId(id, owner.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"));
        if (destino == NegocioStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los negocios solo pueden ser habilitados por SUPER_ADMIN");
        }
        NegocioStatus actual = negocio.getStatus();

        boolean transicionValida = actual == destino
                || (actual == NegocioStatus.INACTIVE && destino == NegocioStatus.ACTIVE)
                || (actual == NegocioStatus.ACTIVE && destino == NegocioStatus.INACTIVE);
        if (!transicionValida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del negocio de " + actual + " a " + destino);
        }
        if (actual == destino) return aDto(negocio);

        negocio.setStatus(destino);
        return aDto(negocioRepository.save(negocio));
    }

    private Usuario obtenerOwnerAutenticado(String emailOwner) {
        if (emailOwner == null || emailOwner.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al owner autenticado");
        }
        Usuario owner = usuarioRepository.findByEmail(emailOwner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al owner autenticado"));
        if (owner.getRol() != Rol.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un owner");
        }
        return owner;
    }

    private Negocio buscarNegocioPropio(Long negocioId, Long ownerId) {
        return negocioRepository.findByNegocioIdAndOwner_UsuarioId(negocioId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"));
    }

    public Negocio obtenerEntidadNegocioPropio(Long negocioId, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return buscarNegocioPropio(negocioId, owner.getUsuarioId());
    }

    private OwnerNegocioDto aDto(Negocio negocio) {
        return new OwnerNegocioDto(
                negocio.getNegocioId(),
                negocio.getNombre(),
                negocio.getDescripcion(),
                negocio.getTelefono(),
                negocio.getEmail(),
                negocio.getStatus()
        );
    }
}
