package com.dev.cutly.usuario.repository;

import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRol(Rol rol);

    long countByRol(Rol rol);

    long countByStatus(com.dev.cutly.usuario.enums.UsuarioStatus status);
}
