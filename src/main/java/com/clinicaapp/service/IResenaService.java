package com.clinicaapp.service;

import com.clinicaapp.dto.ResenaDTO;
import com.clinicaapp.model.Resena;

public interface IResenaService {
    Resena save(ResenaDTO resenaDTO);
    boolean yaExisteResenaParaCita(String citaId);
}