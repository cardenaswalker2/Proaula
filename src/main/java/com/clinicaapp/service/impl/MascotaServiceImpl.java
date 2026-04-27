package com.clinicaapp.service.impl;

import com.clinicaapp.dto.MascotaDTO;
import com.clinicaapp.model.Mascota;
import com.clinicaapp.repository.MascotaRepository;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.service.IMascotaService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MascotaServiceImpl implements IMascotaService {

    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Override
    public List<Mascota> findAll() {
        return mascotaRepository.findAll();
    }

    @Override
    public Optional<Mascota> findById(String id) {
        return mascotaRepository.findById(id);
    }

    @Override
public Mascota save(MascotaDTO mascotaDTO) {
    // Validar que el propietario exista
    if (!usuarioRepository.existsById(mascotaDTO.getPropietarioId())) {
        throw new RuntimeException("Propietario no encontrado con ID: " + mascotaDTO.getPropietarioId());
    }

    Mascota mascota;

    // Lógica para ACTUALIZAR una mascota existente
    if (mascotaDTO.getId() != null && !mascotaDTO.getId().trim().isEmpty()) {
        mascota = mascotaRepository.findById(mascotaDTO.getId())
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada para actualizar con ID: " + mascotaDTO.getId()));
        
        // Copiamos las propiedades pero ignoramos el ID para no sobreescribirlo
        BeanUtils.copyProperties(mascotaDTO, mascota, "id");
    } 
    // Lógica para CREAR una nueva mascota
    else {
        mascota = new Mascota();
        // Copiamos las propiedades, el ID será generado automáticamente por MongoDB
        BeanUtils.copyProperties(mascotaDTO, mascota, "id");
    }

    return mascotaRepository.save(mascota);
}

    @Override
    public void deleteById(String id) {
        if (!mascotaRepository.existsById(id)) {
            throw new RuntimeException("Mascota no encontrada con ID: " + id);
        }
        mascotaRepository.deleteById(id);
    }

    @Override
    public List<Mascota> findByPropietarioId(String propietarioId) {
        return mascotaRepository.findByPropietarioId(propietarioId);
    }

    @Override
    public Optional<Mascota> findByIdAndPropietarioId(String id, String propietarioId) {
        return mascotaRepository.findByIdAndPropietarioId(id, propietarioId);
    }
}