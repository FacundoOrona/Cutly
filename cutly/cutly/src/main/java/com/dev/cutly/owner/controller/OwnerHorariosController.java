package com.dev.cutly.owner.controller;

import com.dev.cutly.owner.dto.horario.ActualizarHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.CrearHorarioRequestDto;
import com.dev.cutly.owner.dto.horario.OwnerHorarioDto;
import com.dev.cutly.owner.dto.OwnerErrorResponseDto;
import com.dev.cutly.owner.service.OwnerHorariosService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/owner/negocios/{negocioId}/horarios")
public class OwnerHorariosController {

    private final OwnerHorariosService ownerHorariosService;

    public OwnerHorariosController(OwnerHorariosService ownerHorariosService) {
        this.ownerHorariosService = ownerHorariosService;
    }

    @GetMapping
    public ResponseEntity<?> listarHorarios(@PathVariable Long negocioId) {
        try {
            List<OwnerHorarioDto> horarios = ownerHorariosService.listarHorarios(
                    negocioId, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(horarios);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudieron consultar los horarios del negocio"));
        }
    }

    @PostMapping
    public ResponseEntity<?> agregarHorario(
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearHorarioRequestDto request
    ) {
        try {
            OwnerHorarioDto horario = ownerHorariosService.agregarHorario(
                    negocioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(horario);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo agregar el horario"));
        }
    }

    @PatchMapping("/{horarioId}")
    public ResponseEntity<?> actualizarHorario(
            @PathVariable Long negocioId,
            @PathVariable Long horarioId,
            @Valid @RequestBody ActualizarHorarioRequestDto request
    ) {
        try {
            OwnerHorarioDto horario = ownerHorariosService.actualizarHorario(
                    negocioId, horarioId, request, emailOwnerAutenticado()
            );
            return ResponseEntity.ok(horario);
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo modificar el horario"));
        }
    }

    @DeleteMapping("/{horarioId}")
    public ResponseEntity<?> eliminarHorario(
            @PathVariable Long negocioId,
            @PathVariable Long horarioId
    ) {
        try {
            ownerHorariosService.eliminarHorario(negocioId, horarioId, emailOwnerAutenticado());
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return respuestaError(e);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OwnerErrorResponseDto("No se pudo eliminar el horario"));
        }
    }

    private String emailOwnerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private ResponseEntity<OwnerErrorResponseDto> respuestaError(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new OwnerErrorResponseDto(e.getReason()));
    }
}
