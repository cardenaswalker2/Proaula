package com.clinicaapp.service.impl;

import com.clinicaapp.model.Notificacion;
import com.clinicaapp.repository.NotificacionRepository;
import com.clinicaapp.service.INotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionServiceImpl implements INotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Override
    public Notificacion crearNotificacion(String usuarioId, String titulo, String mensaje, String link) {
        Notificacion notif = new Notificacion(usuarioId, titulo, mensaje, link);
        return notificacionRepository.save(notif);
    }

    @Override
    public List<Notificacion> getUltimasNotificaciones(String usuarioId, int limit) {
        return notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long getConteoNoLeidas(String usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    public void marcarComoLeida(String notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    @Override
    public void marcarTodasComoLeidas(String usuarioId) {
        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaDesc(usuarioId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(noLeidas);
    }
}
