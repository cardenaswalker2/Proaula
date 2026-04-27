package com.clinicaapp.model;

// import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;


// @NoArgsConstructor
// @AllArgsConstructor
// @Data
@Document(collection = "servicios")
public class Servicio {
    @Id
    private String id;
    private String nombre;
    private String descripcion;
    private Double costo; // Usamos Double para el precio

    public Servicio() {}

    public Servicio(String id, String nombre, String descripcion, Double costo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.costo = costo;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getCosto() {
        return costo;
    }
    public void setCosto(Double costo) {
        this.costo = costo;
    }
}