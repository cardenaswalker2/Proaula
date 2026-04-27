package com.clinicaapp.repository;

import com.clinicaapp.model.MascotaVirtual;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface MascotaVirtualRepository extends MongoRepository<MascotaVirtual, String> {
    Optional<MascotaVirtual> findByUsuarioId(String usuarioId);
}
