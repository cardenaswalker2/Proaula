package com.clinicaapp.model;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
@Document(collection = "recordatorios")
public class Recordatorio {
    @Id
    private String id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHoraRecordatorio;
    private String tipo; // Ej: "Vacunación", "Desparasitación", "Cita"
    private boolean completado;

    private String usuarioId; // Propietario de la mascota
    private String mascotaId; // Mascota a la que se refiere el recordatorio

    public Recordatorio() {}

    public Recordatorio(String id, String titulo, String descripcion, LocalDateTime fechaHoraRecordatorio, String tipo, boolean completado, String usuarioId, String mascotaId) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaHoraRecordatorio = fechaHoraRecordatorio;
        this.tipo = tipo;
        this.completado = completado;
        this.usuarioId = usuarioId;
        this.mascotaId = mascotaId;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHoraRecordatorio() {
        return fechaHoraRecordatorio;
    }
    public void setFechaHoraRecordatorio(LocalDateTime fechaHoraRecordatorio) {
        this.fechaHoraRecordatorio = fechaHoraRecordatorio;
    }

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isCompletado() {
        return completado;
    }
    public void setCompletado(boolean completado) {
        this.completado = completado;
    }

    public String getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getMascotaId() {
        return mascotaId;
    }
    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }
}