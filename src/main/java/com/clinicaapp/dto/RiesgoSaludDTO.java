package com.clinicaapp.dto;

public class RiesgoSaludDTO {
    private String categoria;    
    private String nivelRiesgo; 
    private int porcentaje;     
    private String descripcion; 
    private String accionRecomendada; 
    private String iconClass;   
    private String colorClass;  

    public RiesgoSaludDTO() {}

    public RiesgoSaludDTO(String categoria, String nivelRiesgo, int porcentaje, String descripcion, String accionRecomendada, String iconClass, String colorClass) {
        this.categoria = categoria;
        this.nivelRiesgo = nivelRiesgo;
        this.porcentaje = porcentaje;
        this.descripcion = descripcion;
        this.accionRecomendada = accionRecomendada;
        this.iconClass = iconClass;
        this.colorClass = colorClass;
    }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public int getPorcentaje() { return porcentaje; }
    public void setPorcentaje(int porcentaje) { this.porcentaje = porcentaje; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getAccionRecomendada() { return accionRecomendada; }
    public void setAccionRecomendada(String accionRecomendada) { this.accionRecomendada = accionRecomendada; }

    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }

    public String getColorClass() { return colorClass; }
    public void setColorClass(String colorClass) { this.colorClass = colorClass; }
}
