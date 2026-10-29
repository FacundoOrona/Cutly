package com.dev.cutly.owner.service;

import com.dev.cutly.cliente.entity.ClienteLocal;
import com.dev.cutly.cliente.repository.ClienteLocalRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.owner.dto.cliente.CrearClienteLocalRequestDto;
import com.dev.cutly.owner.dto.turno.CrearOwnerTurnoManualRequestDto;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.turno.service.TurnoHorarioValidator;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.enums.UsuarioStatus;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@Import({OwnerTurnosService.class, OwnerClienteLocalService.class, TurnoHorarioValidator.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OwnerTurnoManualRollbackTests {
    private static final AtomicInteger NEXT_FIXTURE = new AtomicInteger();

    @Autowired private OwnerTurnosService ownerTurnosService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private HorarioNegocioRepository horarioRepository;
    @Autowired private ClienteLocalRepository clienteLocalRepository;
    @Autowired private TurnoRepository turnoRepository;

    @MockitoBean private OwnerNegocioService ownerNegocioService;

    private Negocio negocio;
    private Long servicioId;
    private LocalDateTime inicio;
    private String ownerEmail;

    @BeforeEach
    void setUp() {
        int fixture = NEXT_FIXTURE.incrementAndGet();
        ownerEmail = "rollback-owner-" + fixture + "@example.test";
        Usuario owner = new Usuario();
        owner.setNombre("Olga");
        owner.setApellido("Owner");
        owner.setDni(String.format("%08d", 98765432 + fixture));
        owner.setEmail(ownerEmail);
        owner.setContrasena("encoded-test-password");
        owner.setRol(Rol.OWNER);
        owner.setStatus(UsuarioStatus.ACTIVE);
        owner = usuarioRepository.saveAndFlush(owner);

        negocio = new Negocio();
        negocio.setNombre("Negocio transaccional");
        negocio.setStatus(NegocioStatus.ACTIVE);
        negocio.setOwner(owner);
        negocio = negocioRepository.saveAndFlush(negocio);
        when(ownerNegocioService.obtenerEntidadNegocioPropio(negocio.getNegocioId(), ownerEmail))
                .thenReturn(negocio);

        Servicio servicio = new Servicio();
        servicio.setNombre("Corte");
        servicio.setPrecio(new BigDecimal("100.00"));
        servicio.setDuracionMinutos(30);
        servicio.setActivo(true);
        servicio.setNegocio(negocio);
        servicio = servicioRepository.saveAndFlush(servicio);
        servicioId = servicio.getServicioId();

        inicio = LocalDate.now().plusDays(14).atTime(10, 0);
        HorarioNegocio horario = new HorarioNegocio();
        horario.setDiaSemana(DiaSemana.valueOf(inicio.getDayOfWeek().name()));
        horario.setHoraApertura(LocalTime.of(9, 0));
        horario.setHoraCierre(LocalTime.of(18, 0));
        horario.setCerrado(false);
        horario.setNegocio(negocio);
        horarioRepository.saveAndFlush(horario);
    }

    @Test
    void revierteClienteLocalCuandoLaPersistenciaDelTurnoFalla() {
        assertThrows(RuntimeException.class, () -> ownerTurnosService.crearManual(
                negocio.getNegocioId(),
                new CrearOwnerTurnoManualRequestDto(
                        servicioId,
                        inicio,
                        "x".repeat(501),
                        null,
                        new CrearClienteLocalRequestDto("Ana", "Pérez", "12.345.678", "5555"),
                        null
                ),
                ownerEmail
        ));

        assertFalse(clienteLocalRepository.existsByNegocio_NegocioIdAndDniNormalizado(
                negocio.getNegocioId(), "12345678"));
    }

    @Test
    void solapamientosDelOwnerSonGlobalesYRespetanLosEstadosQueLiberanLaAgenda() {
        Usuario owner = negocio.getOwner();
        Negocio otroNegocio = new Negocio();
        otroNegocio.setNombre("Otro negocio del mismo owner");
        otroNegocio.setStatus(NegocioStatus.ACTIVE);
        otroNegocio.setOwner(owner);
        otroNegocio = negocioRepository.saveAndFlush(otroNegocio);

        Servicio otroServicio = new Servicio();
        otroServicio.setNombre("Otro servicio");
        otroServicio.setPrecio(new BigDecimal("120.00"));
        otroServicio.setDuracionMinutos(30);
        otroServicio.setActivo(true);
        otroServicio.setNegocio(otroNegocio);
        otroServicio = servicioRepository.saveAndFlush(otroServicio);

        ClienteLocal local = new ClienteLocal();
        local.setNegocio(otroNegocio);
        local.setNombre("Ana");
        local.setApellido("Pérez");
        local.setDniPresentado("12345678");
        local.setDniNormalizado("12345678");
        local.setTelefono("5555");
        local = clienteLocalRepository.saveAndFlush(local);

        Turno turno = new Turno();
        turno.setFechaHoraInicio(inicio);
        turno.setFechaHoraFin(inicio.plusMinutes(30));
        turno.setStatus(TurnoStatus.CONFIRMED);
        turno.setClienteLocal(local);
        turno.setNegocio(otroNegocio);
        turno.setOwnerResponsable(owner);
        turno.setServicio(otroServicio);
        turno = turnoRepository.saveAndFlush(turno);

        assertEquals(1, turnoRepository.findTurnosSolapadosPorOwner(
                owner.getUsuarioId(), inicio, inicio.plusMinutes(30)).size());

        turno.setStatus(TurnoStatus.CANCELLED);
        turnoRepository.saveAndFlush(turno);
        assertEquals(0, turnoRepository.findTurnosSolapadosPorOwner(
                owner.getUsuarioId(), inicio, inicio.plusMinutes(30)).size());

        turno.setStatus(TurnoStatus.REJECTED);
        turnoRepository.saveAndFlush(turno);
        assertEquals(0, turnoRepository.findTurnosSolapadosPorOwner(
                owner.getUsuarioId(), inicio, inicio.plusMinutes(30)).size());

        turno.setStatus(TurnoStatus.NO_SHOW);
        turnoRepository.saveAndFlush(turno);
        assertEquals(1, turnoRepository.findTurnosSolapadosPorOwner(
                owner.getUsuarioId(), inicio, inicio.plusMinutes(30)).size());
    }
}
