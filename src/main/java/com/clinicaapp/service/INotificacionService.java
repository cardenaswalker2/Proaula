package com.clinicaapp.service;

import com.clinicaapp.model.Notificacion;
import java.util.List;

public interface INotificacionService {
    Notificacion crearNotificacion(String usuarioId, String titulo, String mensaje, String link);
    List<Notificacion> getUltimasNotificaciones(String usuarioId, int limit);
    long getConteoNoLeidas(String usuarioId);
    void marcarComoLeida(String notificacionId);
    void marcarTodasComoLeidas(String usuarioId);
}
