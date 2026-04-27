package com.clinicaapp.service.impl;

import com.clinicaapp.dto.VisitaDTO;
import com.clinicaapp.model.Visita;
import com.clinicaapp.repository.CitaRepository; // No es necesario si usas el servicio
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.repository.MascotaRepository;
import com.clinicaapp.repository.VisitaRepository;
import com.clinicaapp.service.ICitaService;
import com.clinicaapp.service.IVisitaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import para verificar strings

import java.util.List;
import java.util.Optional;

@Service
public class VisitaServiceImpl implements IVisitaService {

    private static final Logger log = LoggerFactory.getLogger(VisitaServiceImpl.class);

    @Autowired
    private VisitaRepository visitaRepository;
    @Autowired
    private MascotaRepository mascotaRepository;
    @Autowired
    private ClinicaRepository clinicaRepository;
    @Autowired
    private ICitaService citaService; // Correcto: inyectar el servicio

    @Override
    public List<Visita> findAll() {
        return visitaRepository.findAll();
    }

    @Override
    public Optional<Visita> findById(String id) {
        return visitaRepository.findById(id);
    }

    @Override
    @Transactional
    public Visita save(VisitaDTO visitaDTO) {

        // --- VALIDACIONES MEJORADAS ---
        // Verificamos que los IDs no solo existan, sino que no sean nulos o vacíos
        // antes de usarlos.
        if (!StringUtils.hasText(visitaDTO.getMascotaId())) {
            throw new IllegalArgumentException("El ID de la mascota es obligatorio para registrar una visita.");
        }
        if (!StringUtils.hasText(visitaDTO.getClinicaId())) {
            throw new IllegalArgumentException("El ID de la clínica es obligatorio para registrar una visita.");
        }

        // Verificamos que los IDs realmente correspondan a un documento en la base de
        // datos.
        if (!mascotaRepository.existsById(visitaDTO.getMascotaId())) {
            throw new RuntimeException("Mascota no encontrada con ID: " + visitaDTO.getMascotaId());
        }
        if (!clinicaRepository.existsById(visitaDTO.getClinicaId())) {
            throw new RuntimeException("Clínica no encontrada con ID: " + visitaDTO.getClinicaId());
        }
        // -----------------------------

        Visita visita = new Visita();

        // Lógica para ACTUALIZACIÓN (si se edita una visita)
        if (StringUtils.hasText(visitaDTO.getId())) {
            visita = visitaRepository.findById(visitaDTO.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Visita no encontrada para actualizar con ID: " + visitaDTO.getId()));
        }

        BeanUtils.copyProperties(visitaDTO, visita);

        Visita savedVisita = visitaRepository.save(visita);
        log.info("Visita con ID {} guardada exitosamente para la mascota {}", savedVisita.getId(),
                savedVisita.getMascotaId());

        // --- ACTUALIZACIÓN DE CITA (CON MANEJO DE ERRORES MEJORADO) ---
        // Usamos StringUtils.hasText para una verificación más segura (cubre null, "",
        // " ")
        if (StringUtils.hasText(visitaDTO.getCitaId())) {
            try {
                citaService.completarCita(visitaDTO.getCitaId());
                log.info("La cita asociada con ID {} ha sido marcada como 'Completada'.", visitaDTO.getCitaId());
            } catch (Exception e) {
                // Si falla la actualización de la cita, no revertimos el guardado de la visita.
                // Simplemente registramos una advertencia para que un administrador pueda
                // revisarlo.
                log.error(
                        "ADVERTENCIA: La visita {} fue guardada, pero no se pudo actualizar el estado de la cita {}. Causa: {}",
                        savedVisita.getId(), visitaDTO.getCitaId(), e.getMessage());
            }
        }
        // -------------------------------------------------------------

        return savedVisita;
    }

    @Override
    public void deleteById(String id) {
        if (!visitaRepository.existsById(id)) {
            throw new RuntimeException("Visita no encontrada con ID: " + id);
        }
        visitaRepository.deleteById(id);
    }

    @Override
    public List<Visita> findByMascotaId(String mascotaId) {
        return visitaRepository.findByMascotaId(mascotaId);
    }

    @Override
    public List<Visita> findByClinicaId(String clinicaId) {
        return visitaRepository.findByClinicaId(clinicaId);
    }

    @Override
    public Optional<Visita> findByCitaId(String citaId) {
        return visitaRepository.findByCitaId(citaId);
    }

    @Override
    public List<Visita> getHistorialByMascota(String mascotaId) {
        return visitaRepository.findByMascotaIdOrderByFechaVisitaDesc(mascotaId);
    }

    
}