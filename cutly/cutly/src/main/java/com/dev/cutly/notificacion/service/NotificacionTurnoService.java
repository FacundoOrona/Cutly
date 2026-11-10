package com.dev.cutly.notificacion.service;

import com.dev.cutly.notificacion.entity.Notificacion;
import com.dev.cutly.notificacion.repository.NotificacionRepository;
import com.dev.cutly.turno.entity.Turno;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificacionTurnoService {
    private static final DateTimeFormatter FECHA_HORA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final NotificacionRepository notificacionRepository;

    public NotificacionTurnoService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public void notificarNuevoTurnoAsignado(Turno turno) {
        if (turno.getEmpleado() == null) {
            return;
        }

        String nombreCliente = turno.getCliente() != null
                ? turno.getCliente().getNombre() + " " + turno.getCliente().getApellido()
                : turno.getClienteLocal().getNombre() + " " + turno.getClienteLocal().getApellido();

        Notificacion notificacion = new Notificacion();
        notificacion.setTitulo("Nuevo turno asignado");
        notificacion.setMensaje("Se te asignó un turno para " + nombreCliente + " el "
                + turno.getFechaHoraInicio().format(FECHA_HORA_FORMATTER) + " para el servicio "
                + turno.getServicio().getNombre() + ".");
        notificacion.setLeida(false);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setUsuario(turno.getEmpleado().getUsuario());

        notificacionRepository.save(notificacion);
    }
}
