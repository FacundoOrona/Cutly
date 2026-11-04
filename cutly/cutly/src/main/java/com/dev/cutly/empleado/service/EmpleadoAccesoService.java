package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.repository.EmpleadoRepository;
import org.springframework.stereotype.Service;

@Service
public class EmpleadoAccesoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoAccesoService(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public boolean tienePerfilActivo(String email) {
        return email != null && !email.isBlank()
                && empleadoRepository.findByUsuario_Email(email)
                .map(empleado -> empleado.isActivo())
                .orElse(false);
    }
}
