package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.DireccionNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.DireccionRepository;
import com.dev.cutly.owner.dto.direccion.ActualizarDireccionRequestDto;
import com.dev.cutly.owner.dto.direccion.CrearDireccionRequestDto;
import com.dev.cutly.owner.dto.direccion.OwnerDireccionDto;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OwnerDireccionService {

    private final DireccionRepository direccionRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerDireccionService(
            DireccionRepository direccionRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.direccionRepository = direccionRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public OwnerDireccionDto crearDireccion(
            Long negocioId,
            CrearDireccionRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        if (direccionRepository.existsByNegocio_NegocioId(negocioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El negocio ya tiene una dirección registrada");
        }
        DireccionNegocio direccion = new DireccionNegocio();
        direccion.setCalle(request.calle());
        direccion.setNumero(request.numero());
        direccion.setCiudad(request.ciudad());
        direccion.setProvincia(request.provincia());
        direccion.setCodigoPostal(request.codigoPostal());
        direccion.setNegocio(negocio);
        return aDto(direccionRepository.save(direccion));
    }

    public OwnerDireccionDto obtenerDireccion(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return direccionRepository.findByNegocio_NegocioId(negocioId)
                .map(this::aDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El negocio todavía no tiene una dirección registrada"));
    }

    public OwnerDireccionDto actualizarDireccion(
            Long negocioId,
            ActualizarDireccionRequestDto request,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        DireccionNegocio direccion = direccionRepository.findByNegocio_NegocioId(negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El negocio todavía no tiene una dirección registrada"));
        if (request.calle() != null) direccion.setCalle(request.calle());
        if (request.numero() != null) direccion.setNumero(request.numero());
        if (request.ciudad() != null) direccion.setCiudad(request.ciudad());
        if (request.provincia() != null) direccion.setProvincia(request.provincia());
        if (request.codigoPostal() != null) direccion.setCodigoPostal(request.codigoPostal());
        return aDto(direccionRepository.save(direccion));
    }

    private OwnerDireccionDto aDto(DireccionNegocio direccion) {
        return new OwnerDireccionDto(
                direccion.getDireccionId(),
                direccion.getNegocio().getNegocioId(),
                direccion.getCalle(),
                direccion.getNumero(),
                direccion.getCiudad(),
                direccion.getProvincia(),
                direccion.getCodigoPostal()
        );
    }
}
