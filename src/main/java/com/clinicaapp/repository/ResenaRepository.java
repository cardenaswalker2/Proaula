package com.clinicaapp.repository;

import com.clinicaapp.model.Resena;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends MongoRepository<Resena, String> {
    // Buscar todas las reseñas para una clínica específica
    List<Resena> findByClinicaId(String clinicaId);

    // Buscar todas las reseñas escritas por un usuario específico
    List<Resena> findByUsuarioId(String usuarioId);

    // Buscar reseñas con una calificación igual o superior a un valor
    List<Resena> findByCalificacionGreaterThanEqual(int calificacion);

    // Buscar reseñas por clínica y calificación mínima
    List<Resena> findByClinicaIdAndCalificacionGreaterThanEqual(String clinicaId, int calificacion);

    boolean existsByCitaId(String citaId);
}