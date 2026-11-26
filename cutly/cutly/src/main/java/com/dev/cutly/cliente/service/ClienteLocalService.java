package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.negocio.entity.Negocio;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

@Service
@Transactional
public class ClienteLocalService {
    private static final String UNIQUE_DNI_CONSTRAINT = "uk_cliente_local_negocio_dni_normalizado";

    private final ClienteLocalRepository clienteLocalRepository;

    public ClienteLocalService(ClienteLocalRepository clienteLocalRepository) {
        this.clienteLocalRepository = clienteLocalRepository;
    }

    public ClienteLocal registrar(Negocio negocio, String nombre, String apellido, String dni, String telefono) {
        String dniNormalizado = DniLocalNormalizer.normalizar(dni);
        Long negocioId = negocio.getNegocioId();
        if (clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(negocioId, dniNormalizado)) {
            throw dniDuplicado();
        }

        ClienteLocal cliente = new ClienteLocal();
        cliente.setNegocio(negocio);
        cliente.setNombre(nombre.strip());
        cliente.setApellido(apellido.strip());
        cliente.setDniPresentado(dni);
        cliente.setDniNormalizado(dniNormalizado);
        cliente.setTelefono(telefono.strip());

        try {
            return clienteLocalRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException exception) {
            if (esDniDuplicado(exception)) throw dniDuplicado();
            throw exception;
        }
    }

    private boolean esDniDuplicado(DataIntegrityViolationException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violation) {
                String constraint = violation.getConstraintName();
                if (constraint != null && constraint.equalsIgnoreCase(UNIQUE_DNI_CONSTRAINT)) return true;
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
}
