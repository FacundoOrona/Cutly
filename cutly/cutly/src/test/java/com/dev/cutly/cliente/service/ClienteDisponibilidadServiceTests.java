package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.disponibilidad.ClienteDisponibilidadDto;
import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.enums.NegocioStatus;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.negocio.repository.NegocioRepository;
import com.dev.cutly.servicio.entity.EmpleadoServicio;
import com.dev.cutly.servicio.entity.Servicio;
import com.dev.cutly.servicio.repository.EmpleadoServiciorRepository;
import com.dev.cutly.servicio.repository.ServicioRepository;
import com.dev.cutly.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteDisponibilidadServiceTests {
    private static final Long NEGOCIO_ID = 3L;
    private static final Long SERVICIO_ID = 4L;

    @Mock private NegocioRepository negocioRepository;
    @Mock private HorarioNegocioRepository horarioRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private EmpleadoServiciorRepository empleadoServicioRepository;
    @Mock private TurnoRepository turnoRepository;

    private ClienteDisponibilidadService service;
    private LocalDate lunes;
    private Servicio servicio;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        service = new ClienteDisponibilidadService(negocioRepository, horarioRepository, servicioRepository,
                empleadoRepository, empleadoServicioRepository, turnoRepository);
        lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        if (!lunes.isAfter(LocalDate.now())) lunes = lunes.plusWeeks(1);
        Negocio negocio = new Negocio();
        negocio.setNegocioId(NEGOCIO_ID);
        negocio.setStatus(NegocioStatus.ACTIVE);
        when(negocioRepository.findByNegocioIdAndStatus(NEGOCIO_ID, NegocioStatus.ACTIVE))
                .thenReturn(Optional.of(negocio));
        servicio = new Servicio();
        servicio.setServicioId(SERVICIO_ID);
        servicio.setDuracionMinutos(60);
        servicio.setActivo(true);
        servicio.setNegocio(negocio);
        when(servicioRepository.findByServicioIdAndNegocio_NegocioId(SERVICIO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(servicio));
        empleado = new Empleado();
        empleado.setEmpleadoId(5L);
        empleado.setActivo(true);
        empleado.setNegocio(negocio);
        when(empleadoRepository.findByNegocio_NegocioIdAndActivoTrueOrderByEmpleadoIdAsc(NEGOCIO_ID))
                .thenReturn(List.of(empleado));
        when(empleadoServicioRepository.findByEmpleado_EmpleadoIdAndServicio_ServicioId(5L, SERVICIO_ID))
                .thenReturn(Optional.of(new EmpleadoServicio()));
        lenient().when(turnoRepository.findTurnosSolapados(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void ofreceTurnosEnTodosLosTramosSinInvadirLaPausaNiElCierre() {
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario("09:00", "12:00", false), horario("17:00", "22:00", false)));

        List<ClienteDisponibilidadDto> slots = service.listar(NEGOCIO_ID, lunes, SERVICIO_ID, null);

        assertTrue(slots.stream().anyMatch(slot -> slot.fechaHoraInicio().toLocalTime().equals(LocalTime.of(9, 0))));
        assertTrue(slots.stream().anyMatch(slot -> slot.fechaHoraInicio().toLocalTime().equals(LocalTime.of(17, 0))));
        assertTrue(slots.stream().anyMatch(slot -> slot.fechaHoraFin().toLocalTime().equals(LocalTime.of(12, 0))));
        assertTrue(slots.stream().noneMatch(slot -> slot.fechaHoraInicio().toLocalTime().isAfter(LocalTime.NOON)
                && slot.fechaHoraInicio().toLocalTime().isBefore(LocalTime.of(17, 0))));
        assertTrue(slots.stream().noneMatch(slot -> slot.fechaHoraFin().toLocalTime().isAfter(LocalTime.of(22, 0))));
    }

    @Test
    void noOfreceTurnoQueExcedaElFinDelTramo() {
        servicio.setDuracionMinutos(181);
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario("09:00", "12:00", false)));

        assertTrue(service.listar(NEGOCIO_ID, lunes, SERVICIO_ID, null).isEmpty());
    }

    @Test
    void diaCerradoNoOfreceTurnos() {
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenReturn(List.of(horario("00:00", "00:00", true)));

        assertTrue(service.listar(NEGOCIO_ID, lunes, SERVICIO_ID, null).isEmpty());
    }

    private HorarioNegocio horario(String apertura, String cierre, boolean cerrado) {
        HorarioNegocio horario = new HorarioNegocio();
        horario.setDiaSemana(DiaSemana.MONDAY);
        horario.setHoraApertura(LocalTime.parse(apertura));
        horario.setHoraCierre(LocalTime.parse(cierre));
        horario.setCerrado(cerrado);
        return horario;
    }
}
