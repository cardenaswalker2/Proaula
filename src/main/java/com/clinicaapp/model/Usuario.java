package com.clinicaapp.model;

import com.clinicaapp.model.enums.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;
    private String apellido;
    private String email;
    private String password;
    private String telefono;
    private Role role;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;
    private LocalDateTime fechaCreacion;

    // --- NUEVO CAMPO PARA CONTROL DE SEGURIDAD ---
    private boolean activo = true;

    private java.util.List<Double> descriptorFacial;

    // Indica si el usuario ya registró su rostro exitosamente
    private boolean facialLoginHabilitado = false;

    // Constructor vacío
    public Usuario() {
    }

    // Constructor con todos los campos (Actualizado con 'activo')
    public Usuario(String id, String nombre, String apellido, String email, String password,
            String telefono, Role role, String resetPasswordToken,
            LocalDateTime resetPasswordTokenExpiry, LocalDateTime fechaCreacion,
            boolean activo, java.util.List<Double> descriptorFacial, boolean facialLoginHabilitado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.role = role;
        this.resetPasswordToken = resetPasswordToken;
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
        this.descriptorFacial = descriptorFacial;
        this.facialLoginHabilitado = facialLoginHabilitado;
    }

    // Constructor sin ID (Actualizado)
    public Usuario(String nombre, String apellido, String email, String password,
            String telefono, Role role) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.telefono = telefono;
        this.role = role;
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true; // Por defecto activo al crearse
    }

    // --- GETTERS Y SETTERS EXISTENTES ---
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiry() {
        return resetPasswordTokenExpiry;
    }

    public void setResetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // --- NUEVO GETTER Y SETTER PARA 'ACTIVO' ---
    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // Helper methods para Thymeleaf
    public String getNombreCompleto() {
        String nom = nombre != null ? nombre : "";
        String ape = apellido != null ? apellido : "";
        return (nom + " " + ape).trim();
    }

    public String getIniciales() {
        String iniNom = (nombre != null && !nombre.isEmpty()) ? nombre.substring(0, 1) : "N";
        String iniApe = (apellido != null && !apellido.isEmpty()) ? apellido.substring(0, 1) : "N";
        return (iniNom + iniApe).toUpperCase();
    }

    public java.util.List<Double> getDescriptorFacial() {
        return descriptorFacial;
    }

    public void setDescriptorFacial(java.util.List<Double> descriptorFacial) {
        this.descriptorFacial = descriptorFacial;
    }

    public boolean isFacialLoginHabilitado() {
        return facialLoginHabilitado;
    }

    public void setFacialLoginHabilitado(boolean facialLoginHabilitado) {
        this.facialLoginHabilitado = facialLoginHabilitado;
    }
}