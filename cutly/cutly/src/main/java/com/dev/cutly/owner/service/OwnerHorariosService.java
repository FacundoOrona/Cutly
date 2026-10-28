package com.dev.cutly.owner.service;

import com.dev.cutly.negocio.entity.HorarioNegocio;
import com.dev.cutly.negocio.entity.Negocio;
import com.dev.cutly.negocio.enums.DiaSemana;
import com.dev.cutly.negocio.repository.HorarioNegocioRepository;
import com.dev.cutly.owner.dto.horario.ActualizarHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.CrearHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.OwnerHorarioDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class OwnerHorariosService {

    private final HorarioNegocioRepository horarioRepository;
    private final OwnerNegocioService ownerNegocioService;

    public OwnerHorariosService(
            HorarioNegocioRepository horarioRepository,
            OwnerNegocioService ownerNegocioService
    ) {
        this.horarioRepository = horarioRepository;
        this.ownerNegocioService = ownerNegocioService;
    }

    public List<OwnerHorarioDto> listarHorarios(Long negocioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        return horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(negocioId)
                .stream().map(this::aDto).toList();
    }

    public OwnerHorarioDto agregarHorario(
            Long negocioId,
            CrearHorarioRequestDto request,
            String emailOwner
    ) {
        Negocio negocio = ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        LocalTime apertura = request.cerrado() ? LocalTime.MIDNIGHT : request.horaApertura();
        LocalTime cierre = request.cerrado() ? LocalTime.MIDNIGHT : request.horaCierre();
        validarHorario(request.cerrado(), apertura, cierre);
        validarSinSolapamientos(negocioId, request.diaSemana(), request.cerrado(), apertura, cierre, null);

        HorarioNegocio horario = new HorarioNegocio();
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraApertura(apertura);
        horario.setHoraCierre(cierre);
        horario.setCerrado(request.cerrado());
        horario.setNegocio(negocio);
        return aDto(horarioRepository.save(horario));
    }

    public OwnerHorarioDto actualizarHorario(
            Long negocioId,
            Long horarioId,
            ActualizarHorarioRequestDto request,
            String emailOwner
    ) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        HorarioNegocio horario = buscarHorarioPropio(negocioId, horarioId);
        DiaSemana diaSemana = request.diaSemana() == null ? horario.getDiaSemana() : request.diaSemana();
        boolean cerrado = request.cerrado() == null ? horario.isCerrado() : request.cerrado();
        LocalTime apertura = request.horaApertura() == null ? horario.getHoraApertura() : request.horaApertura();
        LocalTime cierre = request.horaCierre() == null ? horario.getHoraCierre() : request.horaCierre();

        if (cerrado) {
            apertura = LocalTime.MIDNIGHT;
            cierre = LocalTime.MIDNIGHT;
        }
        validarHorario(cerrado, apertura, cierre);
        validarSinSolapamientos(negocioId, diaSemana, cerrado, apertura, cierre, horarioId);

        horario.setDiaSemana(diaSemana);
        horario.setCerrado(cerrado);
        horario.setHoraApertura(apertura);
        horario.setHoraCierre(cierre);
        return aDto(horarioRepository.save(horario));
    }

    public void eliminarHorario(Long negocioId, Long horarioId, String emailOwner) {
        ownerNegocioService.obtenerEntidadNegocioPropio(negocioId, emailOwner);
        horarioRepository.delete(buscarHorarioPropio(negocioId, horarioId));
    }

    private HorarioNegocio buscarHorarioPropio(Long negocioId, Long horarioId) {
        return horarioRepository.findByHorarioIdAndNegocio_NegocioId(horarioId, negocioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el horario solicitado"));
    }

    private void validarHorario(boolean cerrado, LocalTime apertura, LocalTime cierre) {
        if (!cerrado && (apertura == null || cierre == null || !apertura.isBefore(cierre))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de apertura debe ser anterior a la hora de cierre");
        }
    }

    private void validarSinSolapamientos(
            Long negocioId, DiaSemana diaSemana, boolean cerrado,
            LocalTime apertura, LocalTime cierre, Long horarioIdExcluido
    ) {
        List<HorarioNegocio> horarios = new ArrayList<>(
                horarioRepository.findByNegocio_NegocioIdOrderByDiaSemanaAscHoraAperturaAsc(negocioId)
        );
        if (horarioIdExcluido != null) {
            horarios.removeIf(h -> h.getHorarioId().equals(horarioIdExcluido));
        }
        List<HorarioNegocio> mismoDia = horarios.stream()
                .filter(h -> h.getDiaSemana() == diaSemana)
                .sorted(Comparator.comparing(HorarioNegocio::getHoraApertura))
                .toList();
        if (cerrado && !mismoDia.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede marcar el día cerrado mientras tenga otros horarios");
        }
        if (!cerrado && mismoDia.stream().anyMatch(HorarioNegocio::isCerrado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El día ya está marcado como cerrado; eliminá ese registro antes de agregar un tramo");
        }
        for (HorarioNegocio existente : mismoDia) {
            if (!cerrado && apertura.isBefore(existente.getHoraCierre())
                    && cierre.isAfter(existente.getHoraApertura())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El horario se superpone con otro tramo del mismo día");
            }
        }
    }

    private OwnerHorarioDto aDto(HorarioNegocio horario) {
        return new OwnerHorarioDto(
                horario.getHorarioId(),
                horario.getNegocio().getNegocioId(),
                horario.getDiaSemana(),
                horario.getHoraApertura(),
                horario.getHoraCierre(),
                horario.isCerrado()
        );
    }
}
