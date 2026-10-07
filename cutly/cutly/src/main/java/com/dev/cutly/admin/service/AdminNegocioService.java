package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.negocio.AdminNegocioDto;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.NegocioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminNegocioService {

    private final NegocioRepository negocioRepository;

    public AdminNegocioService(NegocioRepository negocioRepository) {
        this.negocioRepository = negocioRepository;
    }

    public List<AdminNegocioDto> listarNegocios() {
        return negocioRepository.findAll().stream().map(this::aDto).toList();
    }

    public AdminNegocioDto obtenerNegocio(Long id) {
        return negocioRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
    }

    public AdminNegocioDto actualizarEstado(Long id, NegocioStatus status) {
        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el negocio con id " + id));
        negocio.setStatus(status);
        return aDto(negocioRepository.save(negocio));
    }

    private AdminNegocioDto aDto(Negocio negocio) {
        return new AdminNegocioDto(
                negocio.getNegocioId(),
                negocio.getNombre(),
                negocio.getDescripcion(),
                negocio.getTelefono(),
                negocio.getEmail(),
                negocio.getStatus()
        );
    }
}
