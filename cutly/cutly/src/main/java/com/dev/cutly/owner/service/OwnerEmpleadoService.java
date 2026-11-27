package com.dev.cutly.owner.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.empleado.ActualizarEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.CrearEmpleadoRequestDto;
import com.dev.cutly.owner.dto.empleado.OwnerEmpleadoDto;
import com.dev.cutly.owner.dto.empleado.OwnerRemuneracionEmpleadoDto;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
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
import java.math.BigDecimal;

@Service
@Transactional
public class OwnerEmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OwnerNegocioService ownerNegocioService;
    private final PasswordEncoder passwordEncoder;
    private final EmpleadoServiciorRepository empleadoServicioRepository;

    public OwnerEmpleadoService(
            EmpleadoRepository empleadoRepository,
            UsuarioRepository usuarioRepository,
            OwnerNegocioService ownerNegocioService,
            PasswordEncoder passwordEncoder,
            EmpleadoServiciorRepository empleadoServicioRepository
    ) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.ownerNegocioService = ownerNegocioService;
        this.passwordEncoder = passwordEncoder;
        this.empleadoServicioRepository = empleadoServicioRepository;
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

    public OwnerRemuneracionEmpleadoDto obtenerRemuneracion(Long negocioId, Long empleadoId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Empleado empleado = buscarEmpleado(negocioId, empleadoId);
        return remuneracionDto(empleado);
    }

    public OwnerRemuneracionEmpleadoDto actualizarPorcentajeGeneral(Long negocioId, Long empleadoId,
            BigDecimal porcentaje, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        validarPorcentaje(porcentaje);
        Empleado empleado = buscarEmpleadoBloqueado(negocioId, empleadoId);
        empleado.setPorcentajeRemuneracion(porcentaje);
        return remuneracionDto(empleadoRepository.save(empleado));
    }

    public OwnerRemuneracionEmpleadoDto actualizarExcepcion(Long negocioId, Long empleadoId, Long servicioId,
            BigDecimal porcentaje, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        validarPorcentaje(porcentaje);
        Empleado empleado = buscarEmpleadoBloqueado(negocioId, empleadoId);
        EmpleadoServicio asignacion = empleadoServicioRepository
                .findWithLockingByEmpleadoAndServicio(empleadoId, servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El servicio no está asignado al empleado"));
        if (!negocioId.equals(asignacion.getServicio().getNegocio().getNegocioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el servicio solicitado");
        }
        asignacion.setPorcentajeComision(porcentaje);
        empleadoServicioRepository.save(asignacion);
        return remuneracionDto(empleado);
    }

    public OwnerRemuneracionEmpleadoDto eliminarExcepcion(Long negocioId, Long empleadoId, Long servicioId,
            String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        Empleado empleado = buscarEmpleadoBloqueado(negocioId, empleadoId);
        EmpleadoServicio asignacion = empleadoServicioRepository
                .findWithLockingByEmpleadoAndServicio(empleadoId, servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El servicio no está asignado al empleado"));
        if (!negocioId.equals(asignacion.getServicio().getNegocio().getNegocioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el servicio solicitado");
        }
        asignacion.setPorcentajeComision(null);
        empleadoServicioRepository.save(asignacion);
        return remuneracionDto(empleado);
    }

    private Empleado buscarEmpleadoBloqueado(Long negocioId, Long empleadoId) {
        return empleadoRepository.findWithLockingByEmpleadoId(empleadoId)
                .filter(empleado -> empleado.getNegocio().getNegocioId().equals(negocioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el empleado solicitado"));
    }

    private void validarPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100.00")) > 0
                || porcentaje.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El porcentaje debe estar entre 0.00 y 100.00 con hasta dos decimales");
        }
    }

    private OwnerRemuneracionEmpleadoDto remuneracionDto(Empleado empleado) {
        BigDecimal general = empleado.getPorcentajeRemuneracion();
        List<OwnerRemuneracionEmpleadoDto.ServicioComisionDto> servicios = empleadoServicioRepository
                .findByEmpleado_EmpleadoIdOrderByServicio_ServicioIdAsc(empleado.getEmpleadoId())
                .stream().filter(asignacion -> empleado.getNegocio().getNegocioId()
                        .equals(asignacion.getServicio().getNegocio().getNegocioId()))
                .map(asignacion -> {
                    BigDecimal excepcion = asignacion.getPorcentajeComision();
                    return new OwnerRemuneracionEmpleadoDto.ServicioComisionDto(
                            asignacion.getServicio().getServicioId(), asignacion.getServicio().getNombre(), excepcion,
                            excepcion == null ? general : excepcion, excepcion == null);
                }).toList();
        return new OwnerRemuneracionEmpleadoDto(empleado.getEmpleadoId(), general, servicios);
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
