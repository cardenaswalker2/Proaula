package com.clinicaapp.service;

import com.clinicaapp.dto.CitaDTO;
import com.clinicaapp.dto.CitaDisplayDTO;
import com.clinicaapp.model.Cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



public interface ICitaService {
    // Métodos CRUD básicos
    List<Cita> findAll();
    Optional<Cita> findById(String id);
    Cita save(CitaDTO citaDTO);
    void deleteById(String id);
    void registrarLlegada(String id);

    // Métodos de búsqueda
    List<Cita> findByUsuarioId(String userId);
    List<Cita> findByClinicaId(String clinicaId);
    List<Cita> findAllCitasPendientes();
    List<Cita> findAllCitasActivasParaRecepcion();

    List<Cita> findByEstadoAndEstadoPago(String estado, String estadoPago);

    // Métodos de cambio de estado
    void confirmarCita(String id);
    void cancelarCita(String id);
    void completarCita(String id);
    
    // --- NUEVOS MÉTODOS DE PAGO AÑADIDOS A LA INTERFAZ ---
    void addPaymentIntentToCita(String citaId, String paymentIntentId);
    void markCitaAsPaid(String paymentIntentId);
    void marcarComoPagadaPorRecepcion(String citaId);
    // --------------------------------------------------------

    void reprogramarCita(String id, LocalDateTime nuevaFechaHora);

    List<CitaDisplayDTO> getCitasByClinicaEmail(String email);
}