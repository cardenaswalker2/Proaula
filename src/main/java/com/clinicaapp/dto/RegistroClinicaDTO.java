package com.clinicaapp.dto;

public class RegistroClinicaDTO {

    // --- DATOS DE LA CLÍNICA ---
    private String nombreClinica;
    private String direccion;
    private String telefonoClinica;
    private String emailClinica;
    private String descripcion; // Añadido por si quieres que describan su clínica de una vez

    // --- DATOS DEL DUEÑO (USUARIO ADMIN) ---
    private String nombreDuenio;
    private String apellidoDuenio;
    private String emailDuenio;
    private String password;

    // --- CONSTRUCTORES ---
    
    public RegistroClinicaDTO() {
    }

    public RegistroClinicaDTO(String nombreClinica, String direccion, String telefonoClinica, String emailClinica, String descripcion, String nombreDuenio, String apellidoDuenio, String emailDuenio, String password) {
        this.nombreClinica = nombreClinica;
        this.direccion = direccion;
        this.telefonoClinica = telefonoClinica;
        this.emailClinica = emailClinica;
        this.descripcion = descripcion;
        this.nombreDuenio = nombreDuenio;
        this.apellidoDuenio = apellidoDuenio;
        this.emailDuenio = emailDuenio;
        this.password = password;
    }

    // --- GETTERS Y SETTERS (Indispensables para Spring) ---

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefonoClinica() {
        return telefonoClinica;
    }

    public void setTelefonoClinica(String telefonoClinica) {
        this.telefonoClinica = telefonoClinica;
    }

    public String getEmailClinica() {
        return emailClinica;
    }

    public void setEmailClinica(String emailClinica) {
        this.emailClinica = emailClinica;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNombreDuenio() {
        return nombreDuenio;
    }

    public void setNombreDuenio(String nombreDuenio) {
        this.nombreDuenio = nombreDuenio;
    }

    public String getApellidoDuenio() {
        return apellidoDuenio;
    }

    public void setApellidoDuenio(String apellidoDuenio) {
        this.apellidoDuenio = apellidoDuenio;
    }

    public String getEmailDuenio() {
        return emailDuenio;
    }

    public void setEmailDuenio(String emailDuenio) {
        this.emailDuenio = emailDuenio;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}