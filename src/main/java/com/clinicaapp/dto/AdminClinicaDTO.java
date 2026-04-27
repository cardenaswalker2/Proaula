package com.clinicaapp.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminClinicaDTO {
    // --- DATOS DE LA CLÍNICA ---
    private String id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String descripcion;
    private List<String> serviciosOfrecidos = new ArrayList<>();
    private String imagenUrl;
    private String usuarioAdminId; // Para el caso de edición o linkeo manual

    // --- DATOS DEL NUEVO DUEÑO (Solo para creación) ---
    private String nombreDuenio;
    private String apellidoDuenio;
    private String emailDuenio;
    private String passwordDuenio;

    public AdminClinicaDTO() {}

    // Getters y Setters
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

    public List<String> getServiciosOfrecidos() {
        if (serviciosOfrecidos == null) serviciosOfrecidos = new ArrayList<>();
        return serviciosOfrecidos;
    }
    public void setServiciosOfrecidos(List<String> serviciosOfrecidos) { this.serviciosOfrecidos = serviciosOfrecidos; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getUsuarioAdminId() { return usuarioAdminId; }
    public void setUsuarioAdminId(String usuarioAdminId) { this.usuarioAdminId = usuarioAdminId; }

    public String getNombreDuenio() { return nombreDuenio; }
    public void setNombreDuenio(String nombreDuenio) { this.nombreDuenio = nombreDuenio; }

    public String getApellidoDuenio() { return apellidoDuenio; }
    public void setApellidoDuenio(String apellidoDuenio) { this.apellidoDuenio = apellidoDuenio; }

    public String getEmailDuenio() { return emailDuenio; }
    public void setEmailDuenio(String emailDuenio) { this.emailDuenio = emailDuenio; }

    public String getPasswordDuenio() { return passwordDuenio; }
    public void setPasswordDuenio(String passwordDuenio) { this.passwordDuenio = passwordDuenio; }
}
