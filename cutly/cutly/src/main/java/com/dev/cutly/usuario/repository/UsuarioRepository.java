package com.dev.cutly.usuario.repository;

import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.usuarioId = :id")
    Optional<Usuario> findWithLockingByUsuarioId(@Param("id") Long id);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDniAndUsuarioIdNot(String dni, Long usuarioId);

    boolean existsByEmailAndUsuarioIdNot(String email, Long usuarioId);

    List<Usuario> findByRol(Rol rol);

    long countByRol(Rol rol);

    long countByStatus(com.dev.cutly.usuario.enums.UsuarioStatus status);
}
