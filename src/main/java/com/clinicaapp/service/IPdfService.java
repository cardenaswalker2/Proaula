package com.clinicaapp.service;

import java.io.ByteArrayInputStream;
import java.util.Map;

import com.clinicaapp.model.Visita;

public interface IPdfService {
    // Genera un PDF a partir de una plantilla Thymeleaf y un mapa de datos
    ByteArrayInputStream generatePdfFromTemplate(String templateName, Map<String, Object> data);

    // Métodos específicos para generar diferentes tipos de PDFs
    // ByteArrayInputStream generateFacturaPdf(Factura factura);
    // ByteArrayInputStream generateHistorialClinicoPdf(Mascota mascota, List<Visita> visitas);
    // ByteArrayInputStream generateReportePdf(Reporte reporte);

    ByteArrayInputStream generarRecetaMedica(Visita visita);

    
} 