package com.clinicaapp.model;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
@Document(collection = "citas")
public class Cita {

    @Id
    private String id;
    private LocalDateTime fechaHora;
    private String motivo;
    private String estado; // Ej: "Pendiente", "Confirmada", "Cancelada", "Completada"

    private Double costo;      // El costo de la cita al momento de la solicitud
    private String estadoPago; // "PENDIENTE", "PAGADO", "FALLIDO"
    private String paymentIntentId; // ID de la transacción de Stripe

    private String usuarioId;  // ID del usuario que solicita la cita
    private String clinicaId;  // ID de la clínica
    private String mascotaId;  // ID de la mascota
    private List<String> serviciosIds = new ArrayList<>(); // Lista de IDs de servicios seleccionados

    public Cita() {}

    public Cita(String id, LocalDateTime fechaHora, String motivo, String estado, Double costo, String estadoPago, String paymentIntentId, String usuarioId, String clinicaId, String mascotaId, List<String> serviciosIds) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.motivo = motivo;
        this.estado = estado;
        this.costo = costo;
        this.estadoPago = estadoPago;
        this.paymentIntentId = paymentIntentId;
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

    public Double getCosto() {
        return costo;
    }
    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public String getEstadoPago() {
        return estadoPago;
    }
    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
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
    
    // Método de conveniencia para compatibilidad o para el primer servicio
    public String getFirstServicioId() {
        if (serviciosIds != null && !serviciosIds.isEmpty()) {
            return serviciosIds.get(0);
        }
        return null;
    }

    public String getServicioId() {
        return getFirstServicioId();
    }
}