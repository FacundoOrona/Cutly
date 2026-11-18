package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.negocio.ActualizarNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.CrearNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.OwnerNegocioDto;
import com.dev.cutly.suscripcion.entity.PlanSuscripcion;
import com.dev.cutly.suscripcion.entity.Suscripcion;
import com.dev.cutly.suscripcion.enums.SuscripcionStatus;
import com.dev.cutly.suscripcion.repository.SuscripcionRepository;
import com.dev.cutly.suscripcion.service.PlanSuscripcionService;
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

    public OwnerNegocioService(
            NegocioRepository negocioRepository,
            UsuarioRepository usuarioRepository,
            SuscripcionRepository suscripcionRepository,
            PlanSuscripcionService planSuscripcionService
    ) {
        this.negocioRepository = negocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.planSuscripcionService = planSuscripcionService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OwnerNegocioDto crearNegocio(CrearNegocioRequestDto request, String emailOwner) {
        Usuario ownerAutenticado = obtenerOwnerAutenticado(emailOwner);
        Usuario owner = usuarioRepository.findWithLockingByUsuarioId(ownerAutenticado.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al owner autenticado"));

        boolean esPrimerNegocio = negocioRepository.findByOwner_UsuarioId(owner.getUsuarioId()).isEmpty();
        Negocio negocio = new Negocio();
        negocio.setNombre(request.nombre());
        negocio.setDescripcion(request.descripcion());
        negocio.setTelefono(request.telefono());
        negocio.setEmail(request.email());
        negocio.setStatus(esPrimerNegocio ? NegocioStatus.ACTIVE : NegocioStatus.INACTIVE);
        negocio.setOwner(owner);
        Negocio negocioGuardado = negocioRepository.save(negocio);

        if (esPrimerNegocio) {
            LocalDate fechaInicio = LocalDate.now();
            PlanSuscripcion planPrueba = planSuscripcionService.obtenerOCrearPlanPruebaGratuita();

            Suscripcion suscripcion = new Suscripcion();
            suscripcion.setStatus(SuscripcionStatus.TRIAL);
            suscripcion.setFechaInicio(fechaInicio);
            suscripcion.setFechaVencimiento(fechaInicio.plusDays(13));
            suscripcion.setPlan(planPrueba);
            suscripcion.asignarPrecioContratado(planPrueba.getPrecioMensual());
            suscripcion.setNegocio(negocioGuardado);
            suscripcionRepository.save(suscripcion);
        }

        return aDto(negocioGuardado);
    }

    public List<OwnerNegocioDto> listarNegocios(String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return negocioRepository.findByOwner_UsuarioId(owner.getUsuarioId()).stream().map(this::aDto).toList();
    }

    public OwnerNegocioDto obtenerNegocio(Long id, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        return negocioRepository.findByNegocioIdAndOwner_UsuarioId(id, owner.getUsuarioId())
                .map(this::aDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el negocio solicitado"));
    }

    public OwnerNegocioDto actualizarNegocio(
            Long id,
            ActualizarNegocioRequestDto request,
            String emailOwner
    ) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Negocio negocio = buscarNegocioPropio(id, owner.getUsuarioId());
        if (request.nombre() != null) negocio.setNombre(request.nombre());
        if (request.descripcion() != null) negocio.setDescripcion(request.descripcion());
        if (request.telefono() != null) negocio.setTelefono(request.telefono());
        if (request.email() != null) negocio.setEmail(request.email());
        return aDto(negocioRepository.save(negocio));
    }

    public OwnerNegocioDto actualizarEstado(Long id, NegocioStatus destino, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Negocio negocio = buscarNegocioPropio(id, owner.getUsuarioId());
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
