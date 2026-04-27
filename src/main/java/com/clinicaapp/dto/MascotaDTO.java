package com.clinicaapp.dto;

import com.clinicaapp.model.enums.Especie;
import com.clinicaapp.model.enums.RazaGato;
import com.clinicaapp.model.enums.RazaPerro;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import java.time.LocalDate;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class MascotaDTO {
    private String id;
    private String nombre;
    private Especie especie;
    private RazaPerro razaPerro;
    private RazaGato razaGato;
    private LocalDate fechaNacimiento;
    private String propietarioId;
    private String fotoUrl;
    private String razaPersonalizada;

    public MascotaDTO() {}

    public MascotaDTO(String id, String nombre, Especie especie, RazaPerro razaPerro, RazaGato razaGato, LocalDate fechaNacimiento, String propietarioId) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.razaPerro = razaPerro;
        this.razaGato = razaGato;
        this.fechaNacimiento = fechaNacimiento;
        this.propietarioId = propietarioId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Especie getEspecie() { return especie; }
    public void setEspecie(Especie especie) { this.especie = especie; }

    public RazaPerro getRazaPerro() { return razaPerro; }
    public void setRazaPerro(RazaPerro razaPerro) { this.razaPerro = razaPerro; }

    public RazaGato getRazaGato() { return razaGato; }
    public void setRazaGato(RazaGato razaGato) { this.razaGato = razaGato; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getPropietarioId() { return propietarioId; }
    public void setPropietarioId(String propietarioId) { this.propietarioId = propietarioId; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getRazaPersonalizada() { return razaPersonalizada; }
    public void setRazaPersonalizada(String razaPersonalizada) { this.razaPersonalizada = razaPersonalizada; }
}