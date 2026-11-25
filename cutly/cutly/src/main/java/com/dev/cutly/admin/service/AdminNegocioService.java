package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.negocio.AdminNegocioDto;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminNegocioService {

    private final NegocioRepository negocioRepository;
    public AdminNegocioService(NegocioRepository negocioRepository) {
        this.negocioRepository = negocioRepository;
    }

    public List<AdminNegocioDto> listarNegocios() {
        return aDtos(negocioRepository.findAll());
    }

    public List<AdminNegocioDto> listarNegociosDeOwner(Long ownerId) {
        return aDtos(negocioRepository.findByOwner_UsuarioId(ownerId));
    }

    public AdminNegocioDto obtenerNegocio(Long id) {
        return negocioRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
    }

    @Transactional
    public AdminNegocioDto actualizarEstado(Long id, NegocioStatus status) {
        Negocio negocio = negocioRepository.findWithLockingByNegocioId(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
        if (!transicionValida(negocio.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el estado del negocio de " + negocio.getStatus() + " a " + status);
        }
        if (negocio.getStatus() == status) {
            return aDto(negocio);
        }
        negocio.setStatus(status);
        return aDto(negocioRepository.save(negocio));
    }

    private boolean transicionValida(NegocioStatus actual, NegocioStatus destino) {
        return switch (actual) {
            case ACTIVE -> destino == NegocioStatus.SUSPENDED || destino == NegocioStatus.INACTIVE;
            case SUSPENDED, INACTIVE -> destino == NegocioStatus.ACTIVE
                    || (actual == NegocioStatus.SUSPENDED && destino == NegocioStatus.INACTIVE);
        };
    }

    private AdminNegocioDto aDto(Negocio negocio) {
        return aDtos(List.of(negocio)).get(0);
    }

    private List<AdminNegocioDto> aDtos(List<Negocio> negocios) {
        return negocios.stream().map(negocio -> new AdminNegocioDto(
                negocio.getNegocioId(), negocio.getNombre(), negocio.getDescripcion(), negocio.getTelefono(),
                negocio.getEmail(), negocio.getStatus(), negocio.getOwner().getNombre(),
                negocio.getOwner().getApellido(), negocio.getOwner().getEmail())).toList();
    }
}
