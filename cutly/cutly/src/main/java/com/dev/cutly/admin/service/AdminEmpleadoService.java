package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.empleado.AdminEmpleadoDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminEmpleadoService {

    private final UsuarioRepository usuarioRepository;

    public AdminEmpleadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<AdminEmpleadoDto> listarEmpleados() {
        return usuarioRepository.findByRol(Rol.EMPLOYEE).stream().map(this::aDto).toList();
    }

    public AdminEmpleadoDto obtenerEmpleado(Long id) {
        return aDto(buscarEmpleado(id));
    }

    public AdminEmpleadoDto actualizarEstado(Long id, UsuarioStatus status) {
        Usuario empleado = buscarEmpleado(id);
        empleado.setStatus(status);
        return aDto(usuarioRepository.save(empleado));
    }

    private Usuario buscarEmpleado(Long id) {
        Usuario empleado = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el empleado con id " + id));
        if (empleado.getRol() != Rol.EMPLOYEE) {
            throw new NoSuchElementException("No se encontró el empleado con id " + id);
        }
        return empleado;
    }

    private AdminEmpleadoDto aDto(Usuario empleado) {
        return new AdminEmpleadoDto(
                empleado.getUsuarioId(),
                empleado.getNombre(),
                empleado.getApellido(),
                empleado.getDni(),
                empleado.getEmail(),
                empleado.getStatus()
        );
    }
}
