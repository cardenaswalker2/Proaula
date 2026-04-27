package com.clinicaapp.dto;

import java.time.LocalDateTime;

public class PaymentDTO {
    private String citaId;
    private LocalDateTime fecha;
    private Double monto;
    private String paymentIntentId;
    private String servicioId;
    private String clinicaId;
    private String mascotaId;

    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getServicioId() { return servicioId; }
    public void setServicioId(String servicioId) { this.servicioId = servicioId; }

    public String getClinicaId() { return clinicaId; }
    public void setClinicaId(String clinicaId) { this.clinicaId = clinicaId; }

    public String getMascotaId() { return mascotaId; }
    public void setMascotaId(String mascotaId) { this.mascotaId = mascotaId; }
}
