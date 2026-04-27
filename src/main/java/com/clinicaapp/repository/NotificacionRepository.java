package com.clinicaapp.repository;

import com.clinicaapp.model.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    List<Notificacion> findByUsuarioIdOrderByFechaDesc(String usuarioId);
    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByFechaDesc(String usuarioId);
    long countByUsuarioIdAndLeidaFalse(String usuarioId);
}
