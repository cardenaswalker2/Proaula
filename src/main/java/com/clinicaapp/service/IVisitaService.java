package com.clinicaapp.service;

import com.clinicaapp.dto.VisitaDTO;
import com.clinicaapp.model.Visita;
import java.util.List;
import java.util.Optional;

public interface IVisitaService {
    List<Visita> findAll();
    Optional<Visita> findById(String id);
    Visita save(VisitaDTO visitaDTO);
    void deleteById(String id);

    // Métodos específicos de negocio
    List<Visita> findByMascotaId(String mascotaId);
    List<Visita> findByClinicaId(String clinicaId);
    Optional<Visita> findByCitaId(String citaId);
    List<Visita> getHistorialByMascota(String mascotaId);

    
}