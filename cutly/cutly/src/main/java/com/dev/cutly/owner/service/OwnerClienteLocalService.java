package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.cliente.service.DniLocalNormalizer;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.cliente.ClienteLocalDto;
import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

@Service
@Transactional
public class OwnerClienteLocalService {
    private static final int LIMITE_BUSQUEDA = 50;
    private static final String UNIQUE_DNI_CONSTRAINT = "uk_cliente_local_negocio_dni_normalizado";

    private final ClienteLocalRepository clienteLocalRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerClienteLocalService(
            ClienteLocalRepository clienteLocalRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.clienteLocalRepository = clienteLocalRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    @Transactional(readOnly = true)
    public List<ClienteLocalDto> buscar(Long negocioId, String texto, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        String consulta = validarTextoBusqueda(texto);
        Sort orden = Sort.by(
                Sort.Order.asc("apellido"),
                Sort.Order.asc("nombre"),
                Sort.Order.asc("clienteLocalId")
        );
        return clienteLocalRepository.buscarPorNombreOApellido(
                        negocioId,
                        consulta,
                        PageRequest.of(0, LIMITE_BUSQUEDA, orden)
                ).stream()
                .map(this::aDto)
                .toList();
    }

    public ClienteLocalDto registrar(
            Long negocioId,
            CrearClienteLocalRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        String dniNormalizado = DniLocalNormalizer.normalizar(request.dni());
        if (clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(negocioId, dniNormalizado)) {
            throw dniDuplicado();
        }

        ClienteLocal cliente = new ClienteLocal();
        cliente.setNegocio(negocio);
        cliente.setNombre(request.nombre().strip());
        cliente.setApellido(request.apellido().strip());
        cliente.setDniPresentado(request.dni());
        cliente.setDniNormalizado(dniNormalizado);
        cliente.setTelefono(request.telefono().strip());

        try {
            return aDto(clienteLocalRepository.saveAndFlush(cliente));
        } catch (DataIntegrityViolationException exception) {
            if (esDniDuplicado(exception)) {
                throw dniDuplicado();
            }
            throw exception;
        }
    }

    private String validarTextoBusqueda(String texto) {
        if (texto == null || texto.codePoints().filter(caracter -> !Character.isWhitespace(caracter)).count() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La búsqueda debe contener al menos dos caracteres no blancos");
        }
        return texto.strip();
    }

    private boolean esDniDuplicado(DataIntegrityViolationException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if (constraint != null && constraint.equalsIgnoreCase(UNIQUE_DNI_CONSTRAINT)) {
                    return true;
                }
            }
            if (causa instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 1062 || "23505".equals(sqlException.getSQLState()))) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private ResponseStatusException dniDuplicado() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya existe un cliente local con ese DNI en el negocio");
    }

    private ClienteLocalDto aDto(ClienteLocal cliente) {
        return new ClienteLocalDto(
                cliente.getClienteLocalId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getDniPresentado(),
                cliente.getTelefono()
        );
    }
}
