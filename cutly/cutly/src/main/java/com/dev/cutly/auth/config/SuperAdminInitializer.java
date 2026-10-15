package com.dev.cutly.auth.config;

import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nombre;
    private final String apellido;
    private final String dni;
    private final String email;
    private final String contrasena;

    public SuperAdminInitializer(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-super-admin.nombre:}") String nombre,
            @Value("${app.bootstrap-super-admin.apellido:}") String apellido,
            @Value("${app.bootstrap-super-admin.dni:}") String dni,
            @Value("${app.bootstrap-super-admin.email:}") String email,
            @Value("${app.bootstrap-super-admin.password:}") String contrasena
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.email = email;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (estaEnBlanco(nombre) || estaEnBlanco(apellido) || estaEnBlanco(dni)
                || estaEnBlanco(email) || estaEnBlanco(contrasena)) {
            throw new IllegalStateException(
                    "Debe configurar SUPER_ADMIN_NOMBRE, SUPER_ADMIN_APELLIDO, "
                            + "SUPER_ADMIN_DNI, SUPER_ADMIN_EMAIL y SUPER_ADMIN_PASSWORD"
            );
        }

        usuarioRepository.findByEmail(email).ifPresentOrElse(usuario -> {
            if (usuario.getRol() != Rol.SUPER_ADMIN) {
                throw new IllegalStateException(
                        "El email configurado para el super admin ya pertenece a otro rol"
                );
            }
            if (estaEnBlanco(usuario.getNombre()) || estaEnBlanco(usuario.getApellido())
                    || estaEnBlanco(usuario.getDni()) || estaEnBlanco(usuario.getEmail())
                    || estaEnBlanco(usuario.getContrasena())) {
                throw new IllegalStateException(
                        "El SUPER_ADMIN existente tiene datos obligatorios incompletos"
                );
            }
            if (!dni.equals(usuario.getDni())) {
                throw new IllegalStateException(
                        "El DNI configurado no coincide con el del SUPER_ADMIN existente"
                );
            }
        }, () -> {
            if (usuarioRepository.existsByDni(dni)) {
                throw new IllegalStateException(
                        "El DNI configurado para el super admin ya está registrado"
                );
            }

            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setDni(dni);
            usuario.setEmail(email);
            usuario.setContrasena(passwordEncoder.encode(contrasena));
            usuario.setRol(Rol.SUPER_ADMIN);
            usuario.setStatus(UsuarioStatus.ACTIVE);
            usuarioRepository.save(usuario);
        });
    }

    private boolean estaEnBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
