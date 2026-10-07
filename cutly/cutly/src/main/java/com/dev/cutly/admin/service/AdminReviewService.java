package com.dev.cutly.admin.service;

import com.dev.cutly.admin.dto.AdminReviewDto;
import com.dev.cutly.auditoria.entity.AuditLog;
import com.dev.cutly.auditoria.repository.AuditoriaRepository;
import com.dev.cutly.review.entity.Review;
import com.dev.cutly.review.repository.ReviewRepository;
import com.dev.cutly.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.time.LocalDateTime;

@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminReviewService(
            ReviewRepository reviewRepository,
            AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminReviewDto> listarReviews() {
        return reviewRepository.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminReviewDto obtenerReview(Long id) {
        return reviewRepository.findById(id)
                .map(this::aDto)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la review con id " + id));
    }

    @Transactional
    public void eliminarReview(Long id, String emailActor, String ipAddress) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontró la review con id " + id));
        AuditLog auditoria = new AuditLog();
        auditoria.setAccion("DELETE");
        auditoria.setEntidad("Review");
        auditoria.setEntidadId(review.getReviewId());
        auditoria.setDescripcion("Se eliminó una review del cliente " + review.getCliente().getUsuarioId()
                + " para el negocio " + review.getNegocio().getNegocioId());
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setIpAddress(ipAddress);
        if (emailActor != null) {
            usuarioRepository.findByEmail(emailActor).ifPresent(auditoria::setUsuario);
        }
        reviewRepository.delete(review);
        auditoriaRepository.save(auditoria);
    }

    private AdminReviewDto aDto(Review review) {
        return new AdminReviewDto(
                review.getReviewId(),
                review.getPuntuacion(),
                review.getComentario(),
                review.getCliente().getUsuarioId(),
                review.getCliente().getNombre(),
                review.getNegocio().getNegocioId(),
                review.getNegocio().getNombre(),
                review.getTurno().getTurnoId()
        );
    }
}
