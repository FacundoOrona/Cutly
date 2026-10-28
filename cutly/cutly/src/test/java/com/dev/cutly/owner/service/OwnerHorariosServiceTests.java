package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.owner.dto.horario.ActualizarHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.CrearHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.OwnerHorarioDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerHorariosServiceTests {
    private static final Long NEGOCIO_ID = 10L;
    private static final String OWNER_EMAIL = "owner@cutly.local";

    @Mock private HorarioNegocioRepository horarioRepository;
    @Mock private OwnerNegocioService ownerNegocioService;

    private OwnerHorariosService service;
    private Negocio negocio;
    private final List<HorarioNegocio> horarios = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new OwnerHorariosService(horarioRepository, ownerNegocioService);
        negocio = new Negocio();
        negocio.setNegocioId(NEGOCIO_ID);
        horarios.clear();
        lenient().when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, OWNER_EMAIL)).thenReturn(negocio);
        lenient().when(horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(NEGOCIO_ID))
                .thenAnswer(invocation -> new ArrayList<>(horarios));
        lenient().when(horarioRepository.findByHorarioIdAndNegocio_NegocioId(anyLong(), eq(NEGOCIO_ID)))
                .thenAnswer(invocation -> horarios.stream()
                        .filter(horario -> horario.getHorarioId().equals(invocation.getArgument(0)))
                        .findFirst());
        lenient().when(horarioRepository.save(any(HorarioNegocio.class))).thenAnswer(invocation -> {
            HorarioNegocio horario = invocation.getArgument(0);
            if (horario.getHorarioId() == null) horario.setHorarioId((long) horarios.size() + 1);
            if (!horarios.contains(horario)) horarios.add(horario);
            return horario;
        });
    }

    @Test
    void permiteTramosSeparadosYConsecutivosEnElMismoDia() {
        service.agregarHorario(NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL);
        service.agregarHorario(NEGOCIO_ID, request("12:00", "17:00", false), OWNER_EMAIL);
        service.agregarHorario(NEGOCIO_ID, request("18:00", "20:00", false), OWNER_EMAIL);

        assertEquals(3, horarios.size());
    }

    @Test
    void rechazaTramosQueSeSuperponenPeroPermiteLimitesIguales() {
        service.agregarHorario(NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL);
        assertStatus(HttpStatus.CONFLICT, () -> service.agregarHorario(
                NEGOCIO_ID, request("11:59", "14:00", false), OWNER_EMAIL));
        service.agregarHorario(NEGOCIO_ID, request("12:00", "14:00", false), OWNER_EMAIL);
        assertEquals(2, horarios.size());
    }

    @Test
    void cerradoEsExclusivoYSePuedeReabrirAlEliminarlo() {
        service.agregarHorario(NEGOCIO_ID, request(null, null, true), OWNER_EMAIL);
        assertEquals(LocalTime.MIDNIGHT, horarios.get(0).getHoraApertura());
        assertStatus(HttpStatus.CONFLICT, () -> service.agregarHorario(
                NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL));

        HorarioNegocio cerrado = horarios.get(0);
        when(horarioRepository.findByHorarioIdAndNegocio_NegocioId(cerrado.getHorarioId(), NEGOCIO_ID))
                .thenReturn(Optional.of(cerrado));
        service.eliminarHorario(NEGOCIO_ID, cerrado.getHorarioId(), OWNER_EMAIL);
        horarios.remove(cerrado);
        service.agregarHorario(NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL);
        assertEquals(1, horarios.size());
    }

    @Test
    void noPermiteMarcarCerradoConOtrosTramosYPermiteTransicionEditandoElUnicoTramo() {
        service.agregarHorario(NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL);
        service.agregarHorario(NEGOCIO_ID, request("17:00", "20:00", false), OWNER_EMAIL);
        HorarioNegocio primero = horarios.get(0);

        assertStatus(HttpStatus.CONFLICT, () -> service.actualizarHorario(NEGOCIO_ID, primero.getHorarioId(),
                new ActualizarHorarioRequestDto(null, null, null, true), OWNER_EMAIL));
        service.eliminarHorario(NEGOCIO_ID, horarios.get(horarios.size() - 1).getHorarioId(), OWNER_EMAIL);
        horarios.remove(horarios.size() - 1);
        OwnerHorarioDto cerrado = service.actualizarHorario(NEGOCIO_ID, primero.getHorarioId(),
                new ActualizarHorarioRequestDto(null, null, null, true), OWNER_EMAIL);

        assertTrue(cerrado.cerrado());
        assertEquals(1, horarios.size());
    }

    @Test
    void editarTramoIgnoraSuPropioIntervaloEValidaLosDemas() {
        service.agregarHorario(NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL);
        service.agregarHorario(NEGOCIO_ID, request("17:00", "20:00", false), OWNER_EMAIL);
        HorarioNegocio primero = horarios.get(0);

        service.actualizarHorario(NEGOCIO_ID, primero.getHorarioId(),
                new ActualizarHorarioRequestDto(null, LocalTime.of(10, 0), LocalTime.of(13, 0), null), OWNER_EMAIL);
        assertStatus(HttpStatus.CONFLICT, () -> service.actualizarHorario(NEGOCIO_ID, primero.getHorarioId(),
                new ActualizarHorarioRequestDto(null, LocalTime.of(16, 0), LocalTime.of(18, 0), null), OWNER_EMAIL));
        assertEquals(2, horarios.size());
    }

    @Test
    void conservaLaAutorizacionYNoLeeNiGuardaHorariosDeOtroNegocio() {
        when(ownerNegocioService.obtenerEntidadNegocioPropio(NEGOCIO_ID, OWNER_EMAIL))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.agregarHorario(
                NEGOCIO_ID, request("09:00", "12:00", false), OWNER_EMAIL));
        verify(horarioRepository, never()).findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(any());
        verify(horarioRepository, never()).save(any());
    }

    private CrearHorarioRequestDto request(String apertura, String cierre, boolean cerrado) {
        return new CrearHorarioRequestDto(DiaSemana.MONDAY,
                apertura == null ? null : LocalTime.parse(apertura),
                cierre == null ? null : LocalTime.parse(cierre), cerrado);
    }

    private static void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }
}
