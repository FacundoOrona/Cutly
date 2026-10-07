package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.negocio.ActualizarNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.CrearNegocioRequestDto;
import com.dev.cutly.owner.dto.negocio.OwnerNegocioDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OwnerNegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;

    public OwnerNegocioService(NegocioRepository negocioRepository, UsuarioRepository usuarioRepository) {
        this.negocioRepository = negocioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public OwnerNegocioDto crearNegocio(CrearNegocioRequestDto request, String emailOwner) {
        Usuario owner = obtenerOwnerAutenticado(emailOwner);
        Negocio negocio = new Negocio();
        negocio.setNombre(request.nombre());
        negocio.setDescripcion(request.descripcion());
        negocio.setTelefono(request.telefono());
        negocio.setEmail(request.email());
        negocio.setStatus(NegocioStatus.ACTIVE);
        negocio.setOwner(owner);
        return aDto(negocioRepository.save(negocio));
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
