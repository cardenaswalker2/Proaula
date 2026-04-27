package com.clinicaapp.service;

import com.clinicaapp.model.Mascota;

public interface WekaPredictionService {
    /**
     * Predice el nivel de riesgo articular de una mascota usando un modelo Weka.
     * @param mascota La mascota a evaluar.
     * @return "ALTO", "MEDIO", "BAJO" o "NO_DISPONIBLE"
     */
    String predecirRiesgoArticular(Mascota mascota);
}
