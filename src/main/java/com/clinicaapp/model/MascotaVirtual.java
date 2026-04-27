package com.clinicaapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "mascotas_virtuales")
public class MascotaVirtual {

    @Id
    private String id;
    private String usuarioId;
    private String nombre;
    
    private double salud = 100.0;
    private double hambre = 0.0;
    private double felicidad = 100.0;
    private double energia = 100.0;
    
    private LocalDateTime ultimaActualizacion;

    public MascotaVirtual() {
        this.ultimaActualizacion = LocalDateTime.now();
    }

    public MascotaVirtual(String usuarioId, String nombre) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.ultimaActualizacion = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getSalud() { return salud; }
    public void setSalud(double salud) { this.salud = Math.max(0, Math.min(100, salud)); }

    public double getHambre() { return hambre; }
    public void setHambre(double hambre) { this.hambre = Math.max(0, Math.min(100, hambre)); }

    public double getFelicidad() { return felicidad; }
    public void setFelicidad(double felicidad) { this.felicidad = Math.max(0, Math.min(100, felicidad)); }

    public double getEnergia() { return energia; }
    public void setEnergia(double energia) { this.energia = Math.max(0, Math.min(100, energia)); }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
