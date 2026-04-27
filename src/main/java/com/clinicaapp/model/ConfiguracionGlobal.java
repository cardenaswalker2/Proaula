package com.clinicaapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "configuracion")
public class ConfiguracionGlobal {

    @Id
    private String id = "GLOBAL_SETTINGS";

    private double comisionStripe = 10.0;
    private String emailContacto = "soporte@clinicaapp.com";
    private String telefonoSoporte = "+57 300 0000000";
    private String mensajeBienvenida = "¡Bienvenido a ClínicaApp!";
    private boolean sistemaActivo = true; 

    // Constructor vacío (Indispensable para Spring Data)
    public ConfiguracionGlobal() {}

    // --- GETTERS Y SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getComisionStripe() {
        return comisionStripe;
    }

    public void setComisionStripe(double comisionStripe) {
        this.comisionStripe = comisionStripe;
    }

    public String getEmailContacto() {
        return emailContacto;
    }

    public void setEmailContacto(String emailContacto) {
        this.emailContacto = emailContacto;
    }

    public String getTelefonoSoporte() {
        return telefonoSoporte;
    }

    public void setTelefonoSoporte(String telefonoSoporte) {
        this.telefonoSoporte = telefonoSoporte;
    }

    public String getMensajeBienvenida() {
        return mensajeBienvenida;
    }

    public void setMensajeBienvenida(String mensajeBienvenida) {
        this.mensajeBienvenida = mensajeBienvenida;
    }

    public boolean isSistemaActivo() {
        return sistemaActivo;
    }

    public void setSistemaActivo(boolean sistemaActivo) {
        this.sistemaActivo = sistemaActivo;
    }
}