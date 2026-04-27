package com.clinicaapp.dto;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class VisitaDTO {
    private String id;
    private LocalDateTime fechaVisita;
    private String diagnostico;
    private String tratamiento;
    private List<String> medicamentosRecetados;
    private double costoTotal;
    private String notasAdicionales;
    private String citaId;
    private String mascotaId;
    private String clinicaId;
    private String veterinarioId;

    // --- NUEVOS CAMPOS CLÍNICOS ---
    private Double peso;
    private Double temperatura;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private String estadoConciencia;
    private String condicionCorporal;
    private LocalDateTime fechaProximaCita;

    public VisitaDTO() {}

    public VisitaDTO(String id, LocalDateTime fechaVisita, String diagnostico, String tratamiento, List<String> medicamentosRecetados, double costoTotal, String notasAdicionales, String citaId, String mascotaId, String clinicaId, String veterinarioId) {
        this.id = id;
        this.fechaVisita = fechaVisita;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.medicamentosRecetados = medicamentosRecetados;
        this.costoTotal = costoTotal;
        this.notasAdicionales = notasAdicionales;
        this.citaId = citaId;
        this.mascotaId = mascotaId;
        this.clinicaId = clinicaId;
        this.veterinarioId = veterinarioId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getFechaVisita() { return fechaVisita; }
    public void setFechaVisita(LocalDateTime fechaVisita) { this.fechaVisita = fechaVisita; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }

    public List<String> getMedicamentosRecetados() { return medicamentosRecetados; }
    public void setMedicamentosRecetados(List<String> medicamentosRecetados) { this.medicamentosRecetados = medicamentosRecetados; }

    public double getCostoTotal() { return costoTotal; }
    public void setCostoTotal(double costoTotal) { this.costoTotal = costoTotal; }

    public String getNotasAdicionales() { return notasAdicionales; }
    public void setNotasAdicionales(String notasAdicionales) { this.notasAdicionales = notasAdicionales; }

    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }

    public String getMascotaId() { return mascotaId; }
    public void setMascotaId(String mascotaId) { this.mascotaId = mascotaId; }

    public String getClinicaId() { return clinicaId; }
    public void setClinicaId(String clinicaId) { this.clinicaId = clinicaId; }

    public String getVeterinarioId() { return veterinarioId; }
    public void setVeterinarioId(String veterinarioId) { this.veterinarioId = veterinarioId; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public Integer getFrecuenciaCardiaca() { return frecuenciaCardiaca; }
    public void setFrecuenciaCardiaca(Integer frecuenciaCardiaca) { this.frecuenciaCardiaca = frecuenciaCardiaca; }

    public Integer getFrecuenciaRespiratoria() { return frecuenciaRespiratoria; }
    public void setFrecuenciaRespiratoria(Integer frecuenciaRespiratoria) { this.frecuenciaRespiratoria = frecuenciaRespiratoria; }

    public String getEstadoConciencia() { return estadoConciencia; }
    public void setEstadoConciencia(String estadoConciencia) { this.estadoConciencia = estadoConciencia; }

    public String getCondicionCorporal() { return condicionCorporal; }
    public void setCondicionCorporal(String condicionCorporal) { this.condicionCorporal = condicionCorporal; }

    public LocalDateTime getFechaProximaCita() { return fechaProximaCita; }
    public void setFechaProximaCita(LocalDateTime fechaProximaCita) { this.fechaProximaCita = fechaProximaCita; }
}