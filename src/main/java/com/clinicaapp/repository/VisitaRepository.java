package com.clinicaapp.repository;

import com.clinicaapp.model.Visita;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface VisitaRepository extends MongoRepository<Visita, String> {
    // Buscar visitas por mascota
    List<Visita> findByMascotaId(String mascotaId);

    // Buscar visitas por clínica
    List<Visita> findByClinicaId(String clinicaId);

    // Buscar una visita asociada a una cita específica
    Optional<Visita> findByCitaId(String citaId);

    // Buscar visitas realizadas por un veterinario específico (si implementas veterinarios como usuarios)
    List<Visita> findByVeterinarioId(String veterinarioId);

    // Buscar visitas en un rango de fechas
    List<Visita> findByFechaVisitaBetween(LocalDateTime start, LocalDateTime end);

    // Buscar visitas para una mascota en un rango de fechas
    List<Visita> findByMascotaIdAndFechaVisitaBetween(String mascotaId, LocalDateTime start, LocalDateTime end);

    List<Visita> findByMascotaIdOrderByFechaVisitaDesc(String mascotaId);
}