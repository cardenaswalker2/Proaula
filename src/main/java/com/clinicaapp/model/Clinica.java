package com.clinicaapp.model;

import com.clinicaapp.model.enums.EstadoClinica; // <-- IMPORTANTE: Faltaba este import
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; 
import java.util.Map;

@Document(collection = "clinicas")
public class Clinica {

    @Id
    private String id;
    
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String descripcion;
    private String imagenUrl;
    
    private List<String> serviciosOfrecidos = new ArrayList<>();
    private Map<String, Double> preciosServicios = new HashMap<>();

    private String horaApertura = "08:00";
    private String horaCierre = "18:00";
    private int duracionTurnoMinutos = 30;

    // --- NUEVOS CAMPOS ---
    private EstadoClinica estado = EstadoClinica.PENDIENTE; 
    private String usuarioAdminId; 
    
    // CAMPOS PARA PERSONALIZACIÓN
    private List<Map<String, String>> equipoMedico;
    private List<String> galeriaFotos;

    public Clinica() {}

    // Constructor (puedes dejar el que tenías, los nuevos campos se llenan por setters)
    public Clinica(String id, String nombre, String direccion, String telefono, String email, String descripcion, List<String> serviciosOfrecidos) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.descripcion = descripcion;
        this.serviciosOfrecidos = serviciosOfrecidos;
    }

    // --- GETTERS Y SETTERS (Incluyendo los nuevos) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public List<String> getServiciosOfrecidos() { return serviciosOfrecidos; }
    public void setServiciosOfrecidos(List<String> serviciosOfrecidos) { this.serviciosOfrecidos = serviciosOfrecidos; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Map<String, Double> getPreciosServicios() {
        if (preciosServicios == null) preciosServicios = new HashMap<>();
        return preciosServicios;
    }
    public void setPreciosServicios(Map<String, Double> preciosServicios) { this.preciosServicios = preciosServicios; }

    public String getHoraApertura() { return horaApertura; }
    public void setHoraApertura(String horaApertura) { this.horaApertura = horaApertura; }

    public String getHoraCierre() { return horaCierre; }
    public void setHoraCierre(String horaCierre) { this.horaCierre = horaCierre; }

    public int getDuracionTurnoMinutos() { return duracionTurnoMinutos; }
    public void setDuracionTurnoMinutos(int duracionTurnoMinutos) { this.duracionTurnoMinutos = duracionTurnoMinutos; }

    // --- GETTERS Y SETTERS PARA LOS NUEVOS CAMPOS ---
    public EstadoClinica getEstado() {
        return estado;
    }

    public void setEstado(EstadoClinica estado) {
        this.estado = estado;
    }

    public String getUsuarioAdminId() {
        return usuarioAdminId;
    }

    public void setUsuarioAdminId(String usuarioAdminId) {
        this.usuarioAdminId = usuarioAdminId;
    }

    // --- GETTERS Y SETTERS PARA PERSONALIZACIÓN ---
    public List<Map<String, String>> getEquipoMedico() {
        return equipoMedico;
    }

    public void setEquipoMedico(List<Map<String, String>> equipoMedico) {
        this.equipoMedico = equipoMedico;
    }

    public List<String> getGaleriaFotos() {
        return galeriaFotos;
    }

    public void setGaleriaFotos(List<String> galeriaFotos) {
        this.galeriaFotos = galeriaFotos;
    }
}