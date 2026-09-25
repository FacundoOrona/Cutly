package com.dev.cutly.usuario.service;

import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService (UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado" + email));

        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getContrasena(),
                usuario.getStatus() == UsuarioStatus.ACTIVE,
                true,
                true,
                usuario.getStatus() != UsuarioStatus.BLOCKED,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );
    }
}
