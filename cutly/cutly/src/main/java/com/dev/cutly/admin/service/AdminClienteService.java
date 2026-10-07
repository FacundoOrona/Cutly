package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.cliente.AdminClienteDto;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminClienteService {

    private final UsuarioRepository usuarioRepository;

    public AdminClienteService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<AdminClienteDto> listarClientes() {
        return usuarioRepository.findByRol(Rol.CLIENT).stream().map(this::aDto).toList();
    }

    public AdminClienteDto obtenerCliente(Long id) {
        return aDto(buscarCliente(id));
    }

    public AdminClienteDto actualizarEstado(Long id, UsuarioStatus status) {
        Usuario cliente = buscarCliente(id);
        cliente.setStatus(status);
        return aDto(usuarioRepository.save(cliente));
    }

    private Usuario buscarCliente(Long id) {
        Usuario cliente = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el cliente con id " + id));
        if (cliente.getRol() != Rol.CLIENT) {
            throw new NoSuchElementException("No se encontró el cliente con id " + id);
        }
        return cliente;
    }

    private AdminClienteDto aDto(Usuario cliente) {
        return new AdminClienteDto(
                cliente.getUsuarioId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getDni(),
                cliente.getEmail(),
                cliente.getStatus()
        );
    }
}
