package com.dev.cutly.empleado.service;

import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.empleado.dto.cliente.EmpleadoClienteLocalDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpleadoClienteLocalService {
    private static final int LIMITE_BUSQUEDA = 50;
    private final ClienteLocalRepository clienteLocalRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmpleadoClienteLocalService(ClienteLocalRepository clienteLocalRepository,
                                      EmpleadoRepository empleadoRepository,
                                      UsuarioRepository usuarioRepository) {
        this.clienteLocalRepository = clienteLocalRepository;
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<EmpleadoClienteLocalDto> buscar(String emailEmpleado, String texto) {
        Empleado empleado = obtenerEmpleadoActivo(emailEmpleado);
        if (empleado.getNegocio().getStatus() != NegocioStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el negocio activo solicitado");
        }
        if (texto == null || texto.codePoints().filter(c -> !Character.isWhitespace(c)).count() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La búsqueda debe contener al menos dos caracteres no blancos");
        }
        Sort orden = Sort.by(Sort.Order.asc("apellido"), Sort.Order.asc("nombre"),
                Sort.Order.asc("clienteLocalId"));
        return clienteLocalRepository.buscarPorNombreOApellido(empleado.getNegocio().getNegocioId(), texto.strip(),
                        PageRequest.of(0, LIMITE_BUSQUEDA, orden)).stream()
                .map(c -> new EmpleadoClienteLocalDto(c.getClienteLocalId(), c.getNombre(), c.getApellido()))
                .toList();
    }

    private Empleado obtenerEmpleadoActivo(String email) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al empleado autenticado");
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al empleado autenticado"));
        if (usuario.getRol() != Rol.EMPLOYEE) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "La cuenta autenticada no es un empleado");
        Empleado empleado = empleadoRepository.findByUsuario_UsuarioId(usuario.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el perfil laboral del empleado"));
        if (!empleado.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "El empleado no está activo");
        return empleado;
    }
}
