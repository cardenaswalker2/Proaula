package com.clinicaapp.service.impl;

import com.clinicaapp.dto.ResenaDTO;
import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Resena;
import com.clinicaapp.repository.CitaRepository;
import com.clinicaapp.repository.ResenaRepository;
import com.clinicaapp.service.IResenaService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ResenaServiceImpl implements IResenaService {

    @Autowired private ResenaRepository resenaRepository;
    @Autowired private CitaRepository citaRepository;

    @Override
    public Resena save(ResenaDTO resenaDTO) {
        // Obtenemos la cita para saber a qué clínica y usuario pertenece la reseña
        Cita cita = citaRepository.findById(resenaDTO.getCitaId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada para la reseña."));

        Resena resena = new Resena();
        BeanUtils.copyProperties(resenaDTO, resena);
        
        // Asignamos los datos que no vienen del DTO
        resena.setClinicaId(cita.getClinicaId());
        resena.setUsuarioId(cita.getUsuarioId());
        resena.setFechaCreacion(LocalDateTime.now());

        return resenaRepository.save(resena);
    }

    @Override
    public boolean yaExisteResenaParaCita(String citaId) {
        return resenaRepository.existsByCitaId(citaId);
    }
}