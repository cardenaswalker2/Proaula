package com.clinicaapp.repository;

import com.clinicaapp.model.ConfiguracionGlobal;
import org.springframework.data.mongodb.repository.MongoRepository; // O JpaRepository si usas SQL
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionRepository extends MongoRepository<ConfiguracionGlobal, String> {
    // No necesitas escribir nada más aquí, Spring se encarga del resto.
}