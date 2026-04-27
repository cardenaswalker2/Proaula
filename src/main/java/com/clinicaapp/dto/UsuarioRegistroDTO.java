package com.clinicaapp.dto;

import com.clinicaapp.model.enums.Role;

public class UsuarioRegistroDTO {
    private String id; // <--- AGREGA ESTO
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private String telefono;
    private Role role; 
    
    // --- NUEVO CAMPO PARA RECONOCIMIENTO FACIAL ---
    private boolean facialLoginHabilitado; 

    public UsuarioRegistroDTO() {}

    // Constructor sin rol
    public UsuarioRegistroDTO(String nombre, String apellido, String email, String password, String telefono) {
        
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
    }

    // Constructor con rol
    public UsuarioRegistroDTO(String nombre, String apellido, String email, String password, String telefono, Role role) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.role = role;
    }

    // --- GETTERS Y SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // --- NUEVOS GETTER Y SETTER PARA EL ERROR DE THYMELEAF ---
    public boolean isFacialLoginHabilitado() {
        return facialLoginHabilitado;
    }

    public void setFacialLoginHabilitado(boolean facialLoginHabilitado) {
        this.facialLoginHabilitado = facialLoginHabilitado;
    }
}