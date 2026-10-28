package com.dev.cutly.empleado.service;

import com.dev.cutly.empleado.entity.Empleado;
import com.dev.cutly.empleado.repository.EmpleadoRepository;
import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoHorarioServiceTests {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private HorarioNegocioRepository horarioRepository;
    @Mock private TurnoRepository turnoRepository;

    private EmpleadoHorarioService service;
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        service = new EmpleadoHorarioService(usuarioRepository, empleadoRepository, horarioRepository, turnoRepository);
        lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        if (!lunes.isAfter(LocalDate.now())) lunes = lunes.plusWeeks(1);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(7L);
        usuario.setRol(Rol.EMPLOYEE);
        when(usuarioRepository.findByEmail("empleado@cutly.local")).thenReturn(Optional.of(usuario));
        Empleado empleado = new Empleado();
        empleado.setEmpleadoId(8L);
        empleado.setUsuario(usuario);
        empleado.setActivo(true);
        Negocio negocio = new Negocio();
        negocio.setNegocioId(9L);
        empleado.setNegocio(negocio);
        when(empleadoRepository.findByUsuario_UsuarioId(7L)).thenReturn(Optional.of(empleado));
        when(turnoRepository.findTurnosSolapadosPorEmpleado(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void disponibilidadDevuelveCadaTramoYNoIncluyeLaPausa() {
        when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(9L))
                .thenReturn(List.of(horario("09:00", "12:00", false), horario("17:00", "22:00", false)));

        var disponibilidad = service.disponibilidad(lunes, "empleado@cutly.local");

        assertEquals(2, disponibilidad.size());
        assertEquals(LocalDateTime.of(lunes, LocalTime.of(9, 0)), disponibilidad.get(0).fechaHoraInicio());
        assertEquals(LocalDateTime.of(lunes, LocalTime.NOON), disponibilidad.get(0).fechaHoraFin());
        assertEquals(LocalDateTime.of(lunes, LocalTime.of(17, 0)), disponibilidad.get(1).fechaHoraInicio());
        assertEquals(LocalDateTime.of(lunes, LocalTime.of(22, 0)), disponibilidad.get(1).fechaHoraFin());
    }

    private HorarioNegocio horario(String apertura, String cierre, boolean cerrado) {
        HorarioNegocio horario = new HorarioNegocio();
        horario.setDiaSemana(com.dev.cutly.negocio.enums.DiaSemana.MONDAY);
        horario.setHoraApertura(LocalTime.parse(apertura));
        horario.setHoraCierre(LocalTime.parse(cierre));
        horario.setCerrado(cerrado);
        return horario;
    }
}
