package com.clinicaapp.service.impl;

import com.clinicaapp.dto.PrediccionSaludDTO;
import com.clinicaapp.dto.RiesgoSaludDTO;
import com.clinicaapp.model.Mascota;
import com.clinicaapp.model.Visita;
import com.clinicaapp.model.enums.Especie;
import com.clinicaapp.service.IPredictiveHealthService;
import com.clinicaapp.service.WekaPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PredictiveHealthServiceImpl implements IPredictiveHealthService {

    @Autowired
    private WekaPredictionService wekaPredictionService;

    @Override
    public PrediccionSaludDTO generarPrediccion(Mascota mascota, List<Visita> historial) {
        PrediccionSaludDTO prediccion = new PrediccionSaludDTO();
        
        // Cargar contexto básico
        int edad = mascota.getEdad();
        String raza = mascota.getRaza();
        boolean esSenior = (mascota.getEspecie() == Especie.PERRO && edad >= 8) || 
                           (mascota.getEspecie() == Especie.GATO && edad >= 10);
        
        // 1. Análisis de Riesgos Estáticos (Raza y Edad)
        analizarRiesgosBase(mascota, edad, esSenior, prediccion);
        
        // 2. Análisis Dinámico (Historial de Visitas - IA Simulada)
        analizarHistorial(historial, prediccion);
        
        // 3. Recomendación Nova AI
        generarRecomendacionFinal(mascota, prediccion);
        
        return prediccion;
    }

    private void analizarRiesgosBase(Mascota mascota, int edad, boolean esSenior, PrediccionSaludDTO prediccion) {
        // Riesgo Articular -> AHORA PREDICHO POR WEKA (Inteligencia Artificial)
        if (mascota.getEspecie() == Especie.PERRO) {
            String prediccionWeka = wekaPredictionService.predecirRiesgoArticular(mascota);
            
            if ("ALTO".equals(prediccionWeka)) {
                prediccion.addRiesgo(new RiesgoSaludDTO(
                    "Salud Articular (Weka AI)", "Alto", 85,
                    "Nuestra IA (Weka) ha detectado un ALTO riesgo de problemas articulares basado en raza y edad.",
                    "Suplementar con condroprotectores y agendar radiografía preventiva.",
                    "bi-robot", "danger"
                ));
            } else if ("MEDIO".equals(prediccionWeka)) {
                prediccion.addRiesgo(new RiesgoSaludDTO(
                    "Salud Articular (Weka AI)", "Medio", 50,
                    "La IA estima un riesgo moderado. Comienza a tener cuidado con su peso y desgaste físico.",
                    "Mantener peso ideal y chequeos regulares.",
                    "bi-robot", "warning"
                ));
            } else if ("BAJO".equals(prediccionWeka)) {
                prediccion.addRiesgo(new RiesgoSaludDTO(
                    "Salud Articular (Weka AI)", "Bajo", 15,
                    "La IA estima que las articulaciones están fuera de riesgo por ahora.",
                    "Sigue con sus paseos regulares.",
                    "bi-robot", "success"
                ));
            }
        }

        // Riesgo Dental
        if (edad >= 5) {
            prediccion.addRiesgo(new RiesgoSaludDTO(
                "Higiene Dental", "Medio", 60,
                "La acumulación de sarro es común a partir de los 5 años, pudiendo causar gingivitis.",
                "Limpieza dental profesional y cepillado semanal.",
                "bi-brightness-high", "warning"
            ));
        }

        // Riesgo Cardíaco
        if (esSenior) {
            prediccion.addRiesgo(new RiesgoSaludDTO(
                "Sistema Cardíaco", "Medio", 45,
                "Los soplos cardíacos son comunes en etapas senior.",
                "Control de peso estricto y chequeo cardiológico anual.",
                "bi-heart-pulse", "warning"
            ));
        }
    }

    private void analizarHistorial(List<Visita> historial, PrediccionSaludDTO prediccion) {
        if (historial == null || historial.isEmpty()) return;

        boolean problemasPiel = false;
        boolean problemasDigestivos = false;
        
        for (Visita v : historial) {
            String log = (v.getDiagnostico() + " " + v.getNotasAdicionales()).toLowerCase();
            if (log.contains("alergia") || log.contains("dermatitis") || log.contains("rasca") || log.contains("piel")) {
                problemasPiel = true;
            }
            if (log.contains("diarrea") || log.contains("vomito") || log.contains("gastritis") || log.contains("estomago")) {
                problemasDigestivos = true;
            }
        }

        if (problemasPiel) {
            prediccion.addRiesgo(new RiesgoSaludDTO(
                "Dermatología", "Alto", 90,
                "El historial muestra recurrencia en problemas de piel.",
                "Realizar prueba de exclusión de alergia alimentaria.",
                "bi-fingerprint", "danger"
            ));
        }
        
        if (problemasDigestivos) {
            prediccion.addRiesgo(new RiesgoSaludDTO(
                "Salud Digestiva", "Medio", 50,
                "Se han registrado episodios de sensibilidad estomacal.",
                "Cambiar a una dieta 'Sensitive' o de fácil digestión.",
                "bi-droplet-half", "warning"
            ));
        }
    }

    private void generarRecomendacionFinal(Mascota mascota, PrediccionSaludDTO prediccion) {
        if (prediccion.getRiesgos().isEmpty()) {
            prediccion.setAnalisisGeneral("¡" + mascota.getNombre() + " parece estar en excelente estado!");
            prediccion.setRecomendacionNova("Sigan con el buen trabajo. No olviden sus vacunas anuales.");
        } else {
            prediccion.setAnalisisGeneral("He detectado " + prediccion.getRiesgos().size() + " puntos de atención preventiva para " + mascota.getNombre() + ".");
            prediccion.setRecomendacionNova("Prioriza el chequeo de sus articulaciones, es el riesgo más alto actualmente detectado por mi motor de IA.");
        }
    }
}
