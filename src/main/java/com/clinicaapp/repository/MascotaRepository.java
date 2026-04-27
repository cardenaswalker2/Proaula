package com.clinicaapp.repository;

import com.clinicaapp.model.Mascota;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MascotaRepository extends MongoRepository<Mascota, String> {
    // Buscar todas las mascotas que pertenecen a un propietario específico
    List<Mascota> findByPropietarioId(String propietarioId);

    // Buscar una mascota específica de un propietario específico
    Optional<Mascota> findByIdAndPropietarioId(String id, String propietarioId);

    // Buscar mascotas por especie
    List<Mascota> findByEspecie(com.clinicaapp.model.enums.Especie especie);
}