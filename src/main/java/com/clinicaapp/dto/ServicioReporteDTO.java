package com.clinicaapp.dto;

public class ServicioReporteDTO {

    private String nombre;
    private long cantidad;
    private double ingresos;

    public ServicioReporteDTO() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // ✅ long, no int — compatible con citasPorServicio.size() casteado
    public long getCantidad() { return cantidad; }
    public void setCantidad(long cantidad) { this.cantidad = cantidad; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }
}