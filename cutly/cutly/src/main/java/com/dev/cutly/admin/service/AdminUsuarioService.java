package com.dev.cutly.admin.service;

import com.dev.cutly.usuario.dto.AdminUsuarioDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AdminUsuarioService {

    private static final int TAMANO_MAXIMO_PAGINA = 100;
    private static final Set<String> CAMPOS_ORDENABLES = Set.of(
            "usuarioId", "nombre", "apellido", "dni", "email", "rol", "status"
    );

    private final UsuarioRepository usuarioRepository;

    public AdminUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Page<AdminUsuarioDto> listarUsuarios(
            Rol rol,
            UsuarioStatus status,
            String nombre,
            String apellido,
            String email,
            String dni,
            Pageable pageable
    ) {
        validarPaginacionYOrden(pageable);

        Specification<Usuario> filtros = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (rol != null) {
                predicates.add(criteriaBuilder.equal(root.get("rol"), rol));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            agregarContieneIgnorandoMayusculas(predicates, root, criteriaBuilder, "nombre", nombre);
            agregarContieneIgnorandoMayusculas(predicates, root, criteriaBuilder, "apellido", apellido);
            agregarContieneIgnorandoMayusculas(predicates, root, criteriaBuilder, "email", email);
            agregarContieneIgnorandoMayusculas(predicates, root, criteriaBuilder, "dni", dni);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return usuarioRepository.findAll(filtros, pageable).map(this::aDto);
    }

    public AdminUsuarioDto obtenerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el usuario con id " + id));
        return aDto(usuario);
    }

    private void validarPaginacionYOrden(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1
                || pageable.getPageSize() > TAMANO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("La página debe ser >= 0 y el tamaño debe estar entre 1 y 100");
        }

        for (Sort.Order orden : pageable.getSort()) {
            if (!CAMPOS_ORDENABLES.contains(orden.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenamiento no permitido: " + orden.getProperty());
            }
        }
    }

    private void agregarContieneIgnorandoMayusculas(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Usuario> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String campo,
            String valor
    ) {
        if (valor != null && !valor.isBlank()) {
            String patron = "%" + valor.trim().toLowerCase().replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_") + "%";
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(campo)), patron, '\\'
            ));
        }
    }

    private AdminUsuarioDto aDto(Usuario usuario) {
        return new AdminUsuarioDto(
                usuario.getUsuarioId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getDni(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getStatus()
        );
    }
}
