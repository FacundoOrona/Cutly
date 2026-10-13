package com.dev.cutly.cliente.service;

import com.dev.cutly.cliente.dto.review.ActualizarReviewRequestDto;
import com.dev.cutly.cliente.dto.review.ClienteReviewDto;
import com.dev.cutly.cliente.dto.review.CrearReviewRequestDto;
import com.dev.cutly.review.entity.Review;
import com.dev.cutly.review.repository.ReviewRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.turno.repository.TurnoRepository;
import com.dev.cutly.usuario.entity.Usuario;
import com.dev.cutly.usuario.enums.Rol;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ClienteReviewService {
    private final ReviewRepository reviewRepository;
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;

    public ClienteReviewService(ReviewRepository reviewRepository, TurnoRepository turnoRepository, UsuarioRepository usuarioRepository) {
        this.reviewRepository = reviewRepository;
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public ClienteReviewDto crear(CrearReviewRequestDto request, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        Turno turno = turnoRepository.findByTurnoIdAndCliente_UsuarioId(request.turnoId(), cliente.getUsuarioId())
                .orElseThrow(() -> noEncontrado("No se encontró el turno propio solicitado"));
        if (turno.getStatus() != TurnoStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede reseñar un turno completado");
        }
        if (reviewRepository.existsByTurno_TurnoId(turno.getTurnoId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya tiene una reseña");
        }

        Review review = new Review();
        review.setPuntuacion(request.puntuacion());
        review.setComentario(request.comentario());
        review.setCliente(cliente);
        review.setNegocio(turno.getNegocio());
        review.setTurno(turno);
        return aDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ClienteReviewDto> listar(String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        return reviewRepository.findByCliente_UsuarioIdOrderByReviewIdDesc(cliente.getUsuarioId())
                .stream().map(this::aDto).toList();
    }

    public ClienteReviewDto actualizar(Long reviewId, ActualizarReviewRequestDto request, String emailCliente) {
        Review review = buscarPropia(reviewId, emailCliente);
        if (request.puntuacion() != null) review.setPuntuacion(request.puntuacion());
        if (request.comentario() != null) review.setComentario(request.comentario());
        return aDto(reviewRepository.save(review));
    }

    public void eliminar(Long reviewId, String emailCliente) {
        reviewRepository.delete(buscarPropia(reviewId, emailCliente));
    }

    private Review buscarPropia(Long reviewId, String emailCliente) {
        Usuario cliente = obtenerCliente(emailCliente);
        return reviewRepository.findByReviewIdAndCliente_UsuarioId(reviewId, cliente.getUsuarioId())
                .orElseThrow(() -> noEncontrado("No se encontró la reseña solicitada"));
    }

    private Usuario obtenerCliente(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado");
        }
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al cliente autenticado"));
        if (cliente.getRol() != Rol.CLIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta autenticada no es un cliente");
        }
        return cliente;
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }

    private ClienteReviewDto aDto(Review review) {
        return new ClienteReviewDto(review.getReviewId(), review.getPuntuacion(), review.getComentario(),
                review.getTurno().getTurnoId(), review.getNegocio().getNegocioId(), review.getNegocio().getNombre());
    }
}
