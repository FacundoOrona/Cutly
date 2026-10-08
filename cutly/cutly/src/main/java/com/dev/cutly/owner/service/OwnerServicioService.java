package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.servicio.ActualizarServicioRequestDto;
import com.dev.cutly.owner.dto.servicio.CrearServicioRequestDto;
import com.dev.cutly.owner.dto.servicio.OwnerServicioDto;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class OwnerServicioService {

    private final ServicioRepository servicioRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerServicioService(
            ServicioRepository servicioRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.servicioRepository = servicioRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public List<OwnerServicioDto> listarServicios(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return servicioRepository.findByNegocio_NegocioIdOrderByServicioIdAsc(negocioId)
                .stream().map(this::aDto).toList();
    }

    public OwnerServicioDto obtenerServicio(Long negocioId, Long servicioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return aDto(buscarServicio(negocioId, servicioId));
    }

    public OwnerServicioDto crearServicio(
            Long negocioId,
            CrearServicioRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Servicio servicio = new Servicio();
        servicio.setNegocio(negocio);
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion());
        servicio.setPrecio(request.precio());
        servicio.setDuracionMinutos(request.duracionMinutos());
        servicio.setActivo(true);
        return aDto(servicioRepository.save(servicio));
    }

    public OwnerServicioDto actualizarServicio(
            Long negocioId,
            Long servicioId,
            ActualizarServicioRequestDto request,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Servicio servicio = buscarServicio(negocioId, servicioId);
        if (request.nombre() != null) servicio.setNombre(request.nombre().trim());
        if (request.descripcion() != null) servicio.setDescripcion(request.descripcion());
        if (request.precio() != null) servicio.setPrecio(request.precio());
        if (request.duracionMinutos() != null) servicio.setDuracionMinutos(request.duracionMinutos());
        return aDto(servicioRepository.save(servicio));
    }

    public OwnerServicioDto actualizarEstado(
            Long negocioId,
            Long servicioId,
            boolean activo,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Servicio servicio = buscarServicio(negocioId, servicioId);
        if (servicio.isActivo() == activo) return aDto(servicio);
        servicio.setActivo(activo);
        return aDto(servicioRepository.save(servicio));
    }

    private Servicio buscarServicio(Long negocioId, Long servicioId) {
        return servicioRepository.findByServicioIdAndNegocio_NegocioId(servicioId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el servicio solicitado"));
    }

    private OwnerServicioDto aDto(Servicio servicio) {
        return new OwnerServicioDto(
                servicio.getServicioId(),
                servicio.getNegocio().getNegocioId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.isActivo()
        );
    }
}
