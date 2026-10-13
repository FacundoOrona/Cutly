package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.dto.perfil.*;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.DireccionNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.DireccionRepository;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class EmpleadoPerfilService {
    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final DireccionRepository direccionRepository;
    private final EmpleadoServiciorRepository empleadoServicioRepository;

    public EmpleadoPerfilService(UsuarioRepository usuarioRepository, EmpleadoRepository empleadoRepository,
                                 DireccionRepository direccionRepository, EmpleadoServiciorRepository empleadoServicioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.direccionRepository = direccionRepository;
        this.empleadoServicioRepository = empleadoServicioRepository;
    }

    @Transactional(readOnly = true)
    public EmpleadoPerfilDto obtener(String emailEmpleado) {
        return aPerfil(obtenerEmpleado(emailEmpleado));
    }

    public EmpleadoPerfilDto actualizar(ActualizarEmpleadoPerfilRequestDto request, String emailEmpleado) {
        Empleado empleado = obtenerEmpleado(emailEmpleado);
        Usuario usuario = empleado.getUsuario();
        if (request.email() != null && !request.email().equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmailAndUsuarioIdNot(request.email(), usuario.getUsuarioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        if (request.dni() != null && !request.dni().equals(usuario.getDni())
                && usuarioRepository.existsByDniAndUsuarioIdNot(request.dni(), usuario.getUsuarioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (request.nombre() != null) usuario.setNombre(request.nombre());
        if (request.apellido() != null) usuario.setApellido(request.apellido());
        if (request.dni() != null) usuario.setDni(request.dni());
        if (request.email() != null) usuario.setEmail(request.email());
        usuarioRepository.save(usuario);
        return aPerfil(empleado);
    }

    @Transactional(readOnly = true)
    public EmpleadoNegocioDto obtenerNegocio(String emailEmpleado) {
        Negocio negocio = obtenerEmpleado(emailEmpleado).getNegocio();
        DireccionNegocio direccion = direccionRepository.findByNegocio_NegocioId(negocio.getNegocioId()).orElse(null);
        return new EmpleadoNegocioDto(negocio.getNegocioId(), negocio.getNombre(), negocio.getDescripcion(), negocio.getTelefono(), negocio.getEmail(),
                direccion == null ? null : direccion.getCalle(), direccion == null ? null : direccion.getNumero(),
                direccion == null ? null : direccion.getCiudad(), direccion == null ? null : direccion.getProvincia(),
                direccion == null ? null : direccion.getCodigoPostal());
    }

    @Transactional(readOnly = true)
    public List<EmpleadoServicioDto> obtenerServicios(String emailEmpleado) {
        Empleado empleado = obtenerEmpleado(emailEmpleado);
        return empleadoServicioRepository.findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(empleado.getEmpleadoId())
                .stream().map(EmpleadoServicio::getServicio)
                .map(s -> new EmpleadoServicioDto(s.getServicioId(), s.getNegocio().getNegocioId(), s.getNombre(), s.getDescripcion(), s.getPrecio(), s.getDuracionMinutos(), s.isActivo()))
                .toList();
    }

    private Empleado obtenerEmpleado(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al empleado autenticado"));
        if (usuario.getRol() != Rol.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un empleado");
        }
        return empleadoRepository.findByUsuario_UsuarioId(usuario.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el perfil laboral del empleado"));
    }

    private EmpleadoPerfilDto aPerfil(Empleado empleado) {
        Usuario u = empleado.getUsuario();
        return new EmpleadoPerfilDto(empleado.getEmpleadoId(), u.getUsuarioId(), u.getNombre(), u.getApellido(), u.getDni(), u.getEmail(), u.getRol(), u.getStatus(), empleado.getNegocio().getNegocioId(), empleado.isActivo());
    }
}
