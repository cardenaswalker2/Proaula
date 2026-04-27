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
@Document(collection = "resenas")
public class Resena {

    @Id
    private String id;
    
    private String titulo;
    private String comentario;
    private int calificacion;
    private LocalDateTime fechaCreacion;

    // --- CAMPO AÑADIDO QUE FALTABA ---
    // Este campo enlazará la reseña con la cita específica a la que pertenece.
    private String citaId;
    // ---------------------------------

    private String usuarioId;
    private String clinicaId;

    public Resena() {}

    public Resena(String id, String titulo, String comentario, int calificacion, LocalDateTime fechaCreacion, String citaId, String usuarioId, String clinicaId) {
        this.id = id;
        this.titulo = titulo;
        this.comentario = comentario;
        this.calificacion = calificacion;
        this.fechaCreacion = fechaCreacion;
        this.citaId = citaId;
        this.usuarioId = usuarioId;
        this.clinicaId = clinicaId;
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

    public String getComentario() {
        return comentario;
    }
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public int getCalificacion() {
        return calificacion;
    }
    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getCitaId() {
        return citaId;
    }
    public void setCitaId(String citaId) {
        this.citaId = citaId;
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
}