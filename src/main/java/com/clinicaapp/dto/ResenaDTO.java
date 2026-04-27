package com.clinicaapp.dto;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class ResenaDTO {

    // --- CAMPO CORREGIDO ---
    // Este campo debe llamarse 'citaId' para que Lombok genere getCitaId() y setCitaId()
    private String citaId; 
    // ----------------------
    
    private String titulo;
    private String comentario;
    private int calificacion;

    // Los campos 'id', 'usuarioId', y 'clinicaId' no son necesarios aquí
    // porque el servicio los obtiene de la 'citaId' o los genera al guardar.

    public ResenaDTO() {}

    public ResenaDTO(String citaId, String titulo, String comentario, int calificacion) {
        this.citaId = citaId;
        this.titulo = titulo;
        this.comentario = comentario;
        this.calificacion = calificacion;
    }

    public String getCitaId() { return citaId; }
    public void setCitaId(String citaId) { this.citaId = citaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public int getCalificacion() { return calificacion; }
    public void setCalificacion(int calificacion) { this.calificacion = calificacion; }
}