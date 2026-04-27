package com.clinicaapp.dto;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat; // <-- AÑADE ESTE IMPORT

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class CitaDTO {
    private String id;

    // --- ANOTACIÓN CORREGIDA ---
    // Le dice a Spring exactamente cómo interpretar el String que llega del formulario HTML
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime fechaHora;
    // -------------------------

    private String motivo;
    private String estado;
    private String usuarioId;
    private String clinicaId;
    private String mascotaId;
    private List<String> serviciosIds = new ArrayList<>(); // Lista de IDs de servicios

    public CitaDTO() {}

    public CitaDTO(String id, LocalDateTime fechaHora, String motivo, String estado, String usuarioId, String clinicaId, String mascotaId, List<String> serviciosIds) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.motivo = motivo;
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.clinicaId = clinicaId;
        this.mascotaId = mascotaId;
        this.serviciosIds = serviciosIds != null ? serviciosIds : new ArrayList<>();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getClinicaId() {
        return clinicaId;
    }
    public void setClinicaId(String clinicaId) {
        this.clinicaId = clinicaId;
    }

    public String getMascotaId() {
        return mascotaId;
    }
    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }

    public List<String> getServiciosIds() {
        return serviciosIds;
    }
    public void setServiciosIds(List<String> serviciosIds) {
        this.serviciosIds = serviciosIds;
    }
    
    // Para retrocompatibilidad en algunos flujos
    public String getServicioId() {
        return (serviciosIds != null && !serviciosIds.isEmpty()) ? serviciosIds.get(0) : null;
    }

    public void setServicioId(String servicioId) {
        if (this.serviciosIds == null) {
            this.serviciosIds = new ArrayList<>();
        }
        this.serviciosIds.clear();
        if (servicioId != null && !servicioId.isEmpty()) {
            this.serviciosIds.add(servicioId);
        }
    }
}