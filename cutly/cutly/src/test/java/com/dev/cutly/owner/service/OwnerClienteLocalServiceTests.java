package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.owner.dto.cliente.ClienteLocalDto;
import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerClienteLocalServiceTests {
    private static final Long NEGOCIO_ID = 20L;
    private static final String EMAIL_OWNER = "owner@cutly.local";

    @Mock private ClienteLocalRepository clienteLocalRepository;
    @Mock private OwnerNegocioService ownerNegocioService;

    private OwnerClienteLocalService service;
    private Negocio negocio;

    @BeforeEach
    void setUp() {
        service = new OwnerClienteLocalService(clienteLocalRepository, ownerNegocioService);
        negocio = new Negocio();
        negocio.setNegocioId(NEGOCIO_ID);
        lenient().when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, EMAIL_OWNER))
                .thenReturn(negocio);
    }

    @Test
    void registraClienteLocalSinCrearUsuarioYConDniNormalizado() {
        when(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(NEGOCIO_ID, "00123456"))
                .thenReturn(false);
        when(clienteLocalRepository.saveAndFlush(any(ClienteLocal.class))).thenAnswer(invocation -> {
            ClienteLocal cliente = invocation.getArgument(0);
            cliente.setClienteLocalId(31L);
            return cliente;
        });

        ClienteLocalDto dto = service.registrar(
                NEGOCIO_ID, new CrearClienteLocalRequestDto("Ana", "Pérez", " 00.123-456 ", "+54 11 5555"),
                EMAIL_OWNER
        );

        ArgumentCaptor<ClienteLocal> captor = ArgumentCaptor.forClass(ClienteLocal.class);
        verify(clienteLocalRepository).saveAndFlush(captor.capture());
        assertSame(negocio, captor.getValue().getNegocio());
        assertEquals(" 00.123-456 ", captor.getValue().getDniPresentado());
        assertEquals("00123456", captor.getValue().getDniNormalizado());
        assertEquals(31L, dto.clienteLocalId());
        assertEquals("Ana", dto.nombre());
        assertEquals(" 00.123-456 ", dto.dniPresentado());
    }

    @Test
    void rechazaDniDuplicadoDentroDelMismoNegocioAntesDeInsertar() {
        when(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(NEGOCIO_ID, "12345678"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.registrar(NEGOCIO_ID, request("12.345.678"), EMAIL_OWNER));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(clienteLocalRepository, never()).saveAndFlush(any());
    }

    @Test
    void mismoDniPuedeRegistrarseEnOtroNegocio() {
        Long otroNegocioId = NEGOCIO_ID + 1;
        Negocio otroNegocio = new Negocio();
        otroNegocio.setNegocioId(otroNegocioId);
        when(ownerNegocioService.obtenerEntidadNegocioPropio(otroNegocioId, EMAIL_OWNER))
                .thenReturn(otroNegocio);
        when(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(otroNegocioId, "12345678"))
                .thenReturn(false);
        when(clienteLocalRepository.saveAndFlush(any(ClienteLocal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registrar(otroNegocioId, request("12345678"), EMAIL_OWNER);

        verify(clienteLocalRepository).existsByNegocio_NegocioIdAndDniNormalizado(otroNegocioId, "12345678");
        verify(clienteLocalRepository).saveAndFlush(argThat(cliente -> cliente.getNegocio() == otroNegocio));
    }

    @Test
    void traduceLaCarreraContraElIndiceUnicoAConflicto() {
        when(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(NEGOCIO_ID, "12345678"))
                .thenReturn(false);
        SQLException sqlException = new SQLException("duplicate", "23000", 1062);
        ConstraintViolationException violation = new ConstraintViolationException(
                "duplicate", sqlException, "uk_cliente_local_negocio_dni_normalizado"
        );
        when(clienteLocalRepository.saveAndFlush(any(ClienteLocal.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", violation));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.registrar(NEGOCIO_ID, request("12345678"), EMAIL_OWNER));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Ya existe un cliente local con ese DNI en el negocio", exception.getReason());
    }

    @Test
    void buscaPorNombreOApellidoSoloEnNegocioAutorizadoConLimiteYOrdenEstable() {
        ClienteLocal cliente = new ClienteLocal();
        cliente.setClienteLocalId(2L);
        cliente.setNombre("María");
        cliente.setApellido("López");
        cliente.setDniPresentado("12.345.678");
        cliente.setDniNormalizado("12345678");
        cliente.setTelefono("5555");
        when(clienteLocalRepository.buscarPorNombreOApellido(eq(NEGOCIO_ID), eq("mAr"), any(Pageable.class)))
                .thenReturn(List.of(cliente));

        List<ClienteLocalDto> resultados = service.buscar(NEGOCIO_ID, " mAr ", EMAIL_OWNER);

        assertEquals(1, resultados.size());
        assertEquals(2L, resultados.get(0).clienteLocalId());
        assertEquals("12.345.678", resultados.get(0).dniPresentado());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteLocalRepository).buscarPorNombreOApellido(eq(NEGOCIO_ID), eq("mAr"), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals(List.of("apellido", "nombre", "clienteLocalId"),
                pageable.getValue().getSort().stream().map(order -> order.getProperty()).toList());
    }

    @Test
    void rechazaBusquedaConMenosDeDosCaracteresNoBlancos() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar(NEGOCIO_ID, " a ", EMAIL_OWNER));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(clienteLocalRepository, never()).buscarPorNombreOApellido(any(), any(), any(Pageable.class));
    }

    @Test
    void conservaElErrorDePropiedadDelNegocioYNoConsultaClientes() {
        when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, EMAIL_OWNER))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el negocio solicitado"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.buscar(NEGOCIO_ID, "Ana", EMAIL_OWNER));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(clienteLocalRepository);
    }

    private CrearClienteLocalRequestDto request(String dni) {
        return new CrearClienteLocalRequestDto("Ana", "Pérez", dni, "5555");
    }
}
