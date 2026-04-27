package com.clinicaapp.service;

import com.clinicaapp.dto.PrediccionSaludDTO;
import com.clinicaapp.model.Mascota;
import com.clinicaapp.model.Visita;
import java.util.List;

public interface IPredictiveHealthService {
    PrediccionSaludDTO generarPrediccion(Mascota mascota, List<Visita> historial);
}
