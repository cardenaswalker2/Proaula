package com.clinicaapp.dto;

// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class ClinicaDTO {
    private String id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String descripcion;
    private List<String> serviciosOfrecidos = new ArrayList<>();
    private String imagenUrl;
    private String usuarioAdminId;
    private double latitud;
    private double longitud;

    public ClinicaDTO() {}

    public ClinicaDTO(String id, String nombre, String direccion, String telefono, String email, String descripcion, List<String> serviciosOfrecidos, String imagenUrl, String usuarioAdminId) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.descripcion = descripcion;
        this.serviciosOfrecidos = serviciosOfrecidos;
        this.imagenUrl = imagenUrl;
        this.usuarioAdminId = usuarioAdminId;
    }

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

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}