package com.clinicaapp.dto;

import com.clinicaapp.model.*;
import lombok.Data;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Data
public class CitaDisplayDTO {
    private String id;
    private String fechaHora;
    private String estado;
    private String estadoPago;
    private String mascotaId;
    private String fechaHoraIso;
    private Double costo; // <-- CAMPO AÑADIDO PARA LAS FINANZAS
    private Double probabilidadAusencia; // <-- IA WEKA

    // Datos del Usuario
    private String nombreUsuario;
    private String emailUsuario;
    private String telefonoUsuario;
    
    // Datos relacionados
    private String nombreClinica;
    private String nombreMascota;
    private List<String> nombresServicios = new ArrayList<>();

    public CitaDisplayDTO() {}

    // Constructor para listas generales
    public CitaDisplayDTO(String id, String fechaHora, String estado, String estadoPago, String nombreUsuario, String emailUsuario, String telefonoUsuario, String nombreClinica, String nombreMascota, List<String> nombresServicios) {
        this.id = id;
        this.fechaHora = fechaHora;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.nombreUsuario = nombreUsuario;
        this.emailUsuario = emailUsuario;
        this.telefonoUsuario = telefonoUsuario;
        this.nombreClinica = nombreClinica;
        this.nombreMascota = nombreMascota;
        this.nombresServicios = nombresServicios != null ? nombresServicios : new ArrayList<>();
    }

    // Constructor mejorado (EL QUE USA TU SERVICIO)
    public CitaDisplayDTO(Cita cita, Usuario usuario, Clinica clinica, Mascota mascota, List<Servicio> servicios) {
        this.id = cita.getId();
        
        // Asignación del costo desde la entidad Cita (IMPORTANTE)
        this.costo = cita.getCosto() != null ? cita.getCosto() : 0.0;

        if (cita.getFechaHora() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            this.fechaHora = cita.getFechaHora().format(formatter);
            this.fechaHoraIso = cita.getFechaHora().toString(); 
        } else {
            this.fechaHora = "N/A";
            this.fechaHoraIso = null;
        }

        this.estado = cita.getEstado();
        this.estadoPago = cita.getEstadoPago();
        this.nombreUsuario = (usuario != null) ? usuario.getNombre() + " " + (usuario.getApellido() != null ? usuario.getApellido() : "") : "Usuario Desconocido";
        this.emailUsuario = (usuario != null) ? usuario.getEmail() : "N/A";
        this.telefonoUsuario = (usuario != null) ? usuario.getTelefono() : "N/A";
        this.nombreClinica = (clinica != null && clinica.getNombre() != null) ? clinica.getNombre() : "Clínica Desconocida";
        this.nombreMascota = (mascota != null && mascota.getNombre() != null) ? mascota.getNombre() : "Mascota Desconocida";
        
        if (servicios != null && !servicios.isEmpty()) {
            this.nombresServicios = servicios.stream()
                .map(Servicio::getNombre)
                .collect(Collectors.toList());
        } else {
            this.nombresServicios = new ArrayList<>();
        }
        
        this.mascotaId = (mascota != null) ? mascota.getId() : null;
    }

    // --- MÉTODOS GETTER Y SETTER EXPLÍCITOS (Para asegurar que el controlador los vea) ---

    public Double getCosto() {
        return costo;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public Double getProbabilidadAusencia() {
        return probabilidadAusencia;
    }

    public void setProbabilidadAusencia(Double probabilidadAusencia) {
        this.probabilidadAusencia = probabilidadAusencia;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getEmailUsuario() { return emailUsuario; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }
    public String getTelefonoUsuario() { return telefonoUsuario; }
    public void setTelefonoUsuario(String telefonoUsuario) { this.telefonoUsuario = telefonoUsuario; }
    public String getNombreClinica() { return nombreClinica; }
    public void setNombreClinica(String nombreClinica) { this.nombreClinica = nombreClinica; }
    public String getNombreMascota() { return nombreMascota; }
    public void setNombreMascota(String nombreMascota) { this.nombreMascota = nombreMascota; }
    public List<String> getNombresServicios() { return nombresServicios; }
    public void setNombresServicios(List<String> nombresServicios) { this.nombresServicios = nombresServicios; }
    
    // Método para obtener el primer servicio o un string combinado
    public String getNombreServicio() {
        if (nombresServicios == null || nombresServicios.isEmpty()) {
            return "Servicio Desconocido";
        }
        if (nombresServicios.size() == 1) {
            return nombresServicios.get(0);
        }
        return String.join(", ", nombresServicios);
    }
    public void setNombreServicio(String nombreServicio) {
        // No hace nada para el set, pero es para compatibilidad con serialización
    }
}