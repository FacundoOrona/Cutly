package com.dev.cutly.notificacion.service;

import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.turno.entity.Turno;
import com.dev.cutly.turno.enums.TurnoStatus;
import com.dev.cutly.usuario.entity.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class NotificacionTurnoService {
    private static final DateTimeFormatter FECHA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
    private final NotificacionRepository repository;

    public NotificacionTurnoService(NotificacionRepository repository) { this.repository = repository; }

    /** Called in the same transaction that creates a client booking. */
    public void notificarNuevoTurnoSolicitado(Turno turno) {
        String cliente = nombreCliente(turno);
        String detalle = " para " + cliente + " el " + fecha(turno) + ".";
        crear(turno, turno.getNegocio().getOwner(), "TURNO_SOLICITADO", "Nuevo turno solicitado",
                "Se solicitó un turno" + detalle, "turno-solicitado");
        if (turno.getEmpleado() != null) crear(turno, turno.getEmpleado().getUsuario(), "TURNO_SOLICITADO",
                "Nuevo turno solicitado", "Se solicitó un turno" + detalle, "turno-solicitado");
    }

    /** For manual owner bookings, alert only the assigned employee when applicable. */
    public void notificarNuevoTurnoAsignado(Turno turno) {
        if (turno.getEmpleado() != null) crear(turno, turno.getEmpleado().getUsuario(), "TURNO_ASIGNADO",
                "Nuevo turno asignado", "Se te asignó un turno para " + nombreCliente(turno) + " el " + fecha(turno)
                        + " para el servicio " + turno.getServicio().getNombre() + ".", "turno-asignado");
    }

    /** Notifica al OWNER cuando un empleado completa un turno desde su módulo. */
    public void notificarTurnoCreadoPorEmpleado(Turno turno) {
        if (turno.getEmpleado() == null) return;
        String empleado = turno.getEmpleado().getUsuario().getNombre() + " "
                + turno.getEmpleado().getUsuario().getApellido();
        crear(turno, turno.getNegocio().getOwner(), "TURNO_CREADO_POR_EMPLEADO", "Turno creado por empleado",
                empleado + " registró un turno para " + nombreCliente(turno) + " el " + fecha(turno)
                        + " para el servicio " + turno.getServicio().getNombre() + ".", "creado-por-empleado");
    }

    public void notificarTurnoProximo(Turno turno) {
        crear(turno, turno.getNegocio().getOwner(), "TURNO_PROXIMO", "Turno próximo",
                "El turno de " + nombreCliente(turno) + " comienza a las " + fecha(turno)
                        + " en " + turno.getNegocio().getNombre() + ".", "turno-proximo");
    }

    public void notificarCambioEstado(Turno turno, TurnoStatus anterior, TurnoStatus nuevo, Usuario actor) {
        if (anterior == nuevo) return;
        String event = "TURNO_" + nuevo.name();
        if (nuevo == TurnoStatus.COMPLETED && turno.getEmpleado() != null && actor != null
                && actor.getUsuarioId().equals(turno.getEmpleado().getUsuario().getUsuarioId())) {
            String empleado = actor.getNombre() + " " + actor.getApellido();
            crear(turno, turno.getNegocio().getOwner(), "TURNO_COMPLETADO_POR_EMPLEADO",
                    "Turno completado por empleado",
                    empleado + " completó el turno #" + turno.getTurnoId() + " de " + nombreCliente(turno) + " el " + fecha(turno)
                            + " para el servicio " + turno.getServicio().getNombre() + ".",
                    "completado-por-empleado");
        }
        boolean actorEsCliente = actor != null && turno.getCliente() != null
                && actor.getUsuarioId().equals(turno.getCliente().getUsuarioId());
        if (turno.getCliente() != null && !actorEsCliente) {
            String accion = switch (nuevo) {
                case CONFIRMED -> "confirmado";
                case REJECTED -> "rechazado";
                case CANCELLED -> "cancelado";
                case COMPLETED -> "completado";
                default -> "actualizado";
            };
            crear(turno, turno.getCliente(), event, "Turno " + accion,
                    "Tu turno en " + turno.getNegocio().getNombre() + " del " + fecha(turno) + " fue " + accion + ".",
                    "estado-" + nuevo.name().toLowerCase());
        }
        if (nuevo == TurnoStatus.CANCELLED && turno.getCliente() != null && actorEsCliente) {
            Usuario responsable = turno.getEmpleado() != null ? turno.getEmpleado().getUsuario() : turno.getOwnerResponsable();
            crear(turno, responsable, "TURNO_CANCELADO_CLIENTE", "Turno cancelado por cliente",
                    "El cliente canceló el turno del " + fecha(turno) + ".", "cancelado-cliente");
        }
    }

    private void crear(Turno turno, Usuario destinatario, String tipo, String titulo, String mensaje, String evento) {
        if (destinatario == null) return;
        String key = "turno:" + turno.getTurnoId() + ":" + evento + ":usuario:" + destinatario.getUsuarioId();
        if (repository.findByClaveEvento(key).isPresent()) return;
        Notificacion n = new Notificacion();
        n.setTitulo(titulo); n.setMensaje(mensaje); n.setLeida(false); n.setFechaCreacion(LocalDateTime.now());
        n.setTipoEvento(tipo); n.setNegocioId(turno.getNegocio().getNegocioId());
        n.setRecursoTipo("TURNO"); n.setRecursoId(turno.getTurnoId()); n.setClaveEvento(key); n.setUsuario(destinatario);
        repository.save(n);
    }

    private String nombreCliente(Turno turno) {
        return turno.getCliente() != null ? turno.getCliente().getNombre() + " " + turno.getCliente().getApellido()
                : turno.getClienteLocal().getNombre() + " " + turno.getClienteLocal().getApellido();
    }
    private String fecha(Turno turno) { return turno.getFechaHoraInicio().format(FECHA_HORA_FORMATTER); }
}
