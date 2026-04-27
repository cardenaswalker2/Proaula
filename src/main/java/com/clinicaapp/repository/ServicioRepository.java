package com.clinicaapp.repository;

import com.clinicaapp.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicioRepository extends MongoRepository<Servicio, String> {
    Page<Servicio> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}