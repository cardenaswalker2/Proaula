package com.clinicaapp.controller;

import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.repository.CitaRepository;
import com.clinicaapp.service.IServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinicas")
public class ClinicaApiController {

    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private IServicioService servicioService;

    // 1. Buscador de clínicas
    @GetMapping("/buscar")
    public List<Clinica> buscarClinicas(
            @RequestParam(name = "term", defaultValue = "") String searchTerm) {
        return clinicaRepository.findByNombreContainingIgnoreCase(searchTerm);
    }

    // 2. Servicios con precios reales — CORREGIDO
    @GetMapping("/{id}/servicios")
    public List<Map<String, Object>> getServiciosConPrecios(@PathVariable String id) {
        Clinica clinica = clinicaRepository.findById(id).orElse(null);
        if (clinica == null) return List.of();

        List<Map<String, Object>> respuesta = new ArrayList<>();

        if (clinica.getServiciosOfrecidos() != null
                && !clinica.getServiciosOfrecidos().isEmpty()) {

            // CASO A: La clínica tiene servicios configurados
            for (String servicioId : clinica.getServiciosOfrecidos()) {
                Servicio s = servicioService.findById(servicioId).orElse(null);
                if (s == null) continue;

                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("nombre", s.getNombre());

                Double precio = (clinica.getPreciosServicios() != null
                        && clinica.getPreciosServicios().containsKey(servicioId))
                        ? clinica.getPreciosServicios().get(servicioId)
                        : s.getCosto();

                item.put("precio", precio != null ? precio : 0.0);
                respuesta.add(item);
            }

        } else {

            // CASO B: Fallback — mostrar todos los servicios globales
            List<Servicio> todos = servicioService.findAll();
            for (Servicio s : todos) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("nombre", s.getNombre());
                item.put("precio", s.getCosto() != null ? s.getCosto() : 0.0);
                respuesta.add(item);
            }
        }

        return respuesta;
    }

    // 3. Disponibilidad de horas (slots)
    @GetMapping("/{id}/disponibilidad")
    public List<String> getDisponibilidad(
            @PathVariable String id,
            @RequestParam String fecha) {

        Clinica clinica = clinicaRepository.findById(id).orElse(null);
        if (clinica == null) return List.of();

        List<String> todosLosSlots = generarSlots(
                clinica.getHoraApertura(),
                clinica.getHoraCierre(),
                clinica.getDuracionTurnoMinutos()
        );

        List<Cita> ocupadas = citaRepository.findByClinicaId(id);

        List<String> horasOcupadas = ocupadas.stream()
                .filter(c -> c.getFechaHora() != null
                        && c.getFechaHora().toString().startsWith(fecha))
                .map(c -> c.getFechaHora().toLocalTime().toString())
                .collect(Collectors.toList());

        todosLosSlots.removeAll(horasOcupadas);
        return todosLosSlots;
    }

    // Método auxiliar para generar franjas horarias
    private List<String> generarSlots(String inicio, String fin, int duracion) {
        List<String> slots = new ArrayList<>();
        try {
            LocalTime time = LocalTime.parse(inicio);
            LocalTime endTime = LocalTime.parse(fin);
            while (time.isBefore(endTime)) {
                slots.add(time.toString());
                time = time.plusMinutes(duracion);
            }
        } catch (Exception e) {
            return List.of("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00");
        }
        return slots;
    }
}