package com.clinicaapp.service;

import com.clinicaapp.dto.ClinicaDTO;
import com.clinicaapp.dto.AdminClinicaDTO;
import com.clinicaapp.dto.RegistroClinicaDTO;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.enums.EstadoClinica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface IClinicaService {

    Clinica save(ClinicaDTO clinicaDTO, MultipartFile imagenFile);
    Clinica saveAdmin(AdminClinicaDTO adminDTO, MultipartFile imagenFile);
    Optional<Clinica> findById(String id);
    void deleteById(String id);
    List<Clinica> findAll();
    Page<Clinica> findAll(Pageable pageable);
    Page<Clinica> search(String query, Pageable pageable);
    Page<Clinica> findPaginated(String keyword, Pageable pageable);

    // --- NUEVOS MÉTODOS PARA EL FLUJO DE REGISTRO ---
    void registrarSolicitudClinica(RegistroClinicaDTO dto, MultipartFile imagenFile);
    List<Clinica> buscarPorEstado(EstadoClinica estado);

    void aprobarClinica(String id);
    void rechazarClinica(String id);
}