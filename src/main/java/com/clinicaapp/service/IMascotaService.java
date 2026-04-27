package com.clinicaapp.service;

import com.clinicaapp.dto.MascotaDTO;
import com.clinicaapp.model.Mascota;
import java.util.List;
import java.util.Optional;

public interface IMascotaService {
    List<Mascota> findAll();
    Optional<Mascota> findById(String id);
    Mascota save(MascotaDTO mascotaDTO);
    void deleteById(String id);

    // Métodos específicos de negocio
    List<Mascota> findByPropietarioId(String propietarioId);
    Optional<Mascota> findByIdAndPropietarioId(String id, String propietarioId);
}