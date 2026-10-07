package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.empleado.ActualizarEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.CrearEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.OwnerEmpleadoDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class OwnerEmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OwnerNegocioService ownerNegocioService;
    private final PasswordEncoder passwordEncoder;

    public OwnerEmpleadoService(
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository,
            OwnerNegocioService ownerNegocioService,
            PasswordEncoder passwordEncoder
    ) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.ownerNegocioService = ownerNegocioService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<OwnerEmpleadoDto> listarEmpleados(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return empleadoRepository.findByNegocio_NegocioIdOrderByEmpleadoIdAsc(negocioId)
                .stream().map(this::aDto).toList();
    }

    public OwnerEmpleadoDto obtenerEmpleado(Long negocioId, Long empleadoId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return aDto(buscarEmpleado(negocioId, empleadoId));
    }

    public OwnerEmpleadoDto agregarEmpleado(
            Long negocioId,
            CrearEmpleadoRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setDni(request.dni());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(Rol.EMPLOYEE);
        usuario.setStatus(UsuarioStatus.ACTIVE);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuarioGuardado);
        empleado.setNegocio(negocio);
        empleado.setActivo(true);
        return aDto(empleadoRepository.save(empleado));
    }

    public OwnerEmpleadoDto actualizarEmpleado(
            Long negocioId,
            Long empleadoId,
            ActualizarEmpleadoRequestDto request,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Empleado empleado = buscarEmpleado(negocioId, empleadoId);
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
        if (request.email() != null) usuario.setEmail(request.email());
        if (request.dni() != null) usuario.setDni(request.dni());
        return aDto(empleadoRepository.save(empleado));
    }

    public OwnerEmpleadoDto actualizarEstado(
            Long negocioId,
            Long empleadoId,
            boolean activo,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Empleado empleado = buscarEmpleado(negocioId, empleadoId);
        if (empleado.isActivo() == activo) return aDto(empleado);
        empleado.setActivo(activo);
        return aDto(empleadoRepository.save(empleado));
    }

    private Empleado buscarEmpleado(Long negocioId, Long empleadoId) {
        return empleadoRepository.findByEmpleadoIdAndNegocio_NegocioId(empleadoId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el empleado solicitado"));
    }

    private OwnerEmpleadoDto aDto(Empleado empleado) {
        Usuario usuario = empleado.getUsuario();
        return new OwnerEmpleadoDto(
                empleado.getEmpleadoId(),
                usuario.getUsuarioId(),
                empleado.getNegocio().getNegocioId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getDni(),
                empleado.isActivo()
        );
    }
}
