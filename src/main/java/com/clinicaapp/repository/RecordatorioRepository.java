package com.clinicaapp.repository;

import com.clinicaapp.model.Recordatorio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface RecordatorioRepository extends MongoRepository<Recordatorio, String> {
    // Buscar recordatorios por usuario
    List<Recordatorio> findByUsuarioId(String usuarioId);

    // Buscar recordatorios por mascota
    List<Recordatorio> findByMascotaId(String mascotaId);

    // Buscar recordatorios completados o no completados
    List<Recordatorio> findByCompletado(boolean completado);

    // Buscar recordatorios por tipo
    List<Recordatorio> findByTipo(String tipo);

    // Buscar recordatorios que vencen antes de una fecha y hora específicas
    List<Recordatorio> findByFechaHoraRecordatorioBefore(LocalDateTime dateTime);

    // Buscar recordatorios para una mascota que vencen en un rango de fechas
    List<Recordatorio> findByMascotaIdAndFechaHoraRecordatorioBetween(String mascotaId, LocalDateTime start, LocalDateTime end);
}