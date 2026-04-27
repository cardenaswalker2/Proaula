package com.clinicaapp.service;

import com.clinicaapp.model.Cita;

public interface WekaNoShowPredictionService {
    /**
     * Predice la probabilidad de que el cliente falte a esta cita.
     * @param cita La cita a evaluar.
     * @return Probabilidad del 0.0 al 100.0 de que falte.
     */
    Double predecirProbabilidadAusencia(Cita cita);
}
