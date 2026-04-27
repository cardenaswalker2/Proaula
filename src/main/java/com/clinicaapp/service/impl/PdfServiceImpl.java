package com.clinicaapp.service.impl;

import com.clinicaapp.model.*;
import com.clinicaapp.repository.*;
import com.clinicaapp.service.IPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PdfServiceImpl implements IPdfService {
    private static final Logger log = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Autowired private TemplateEngine templateEngine;
    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private MascotaRepository mascotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // --- 1. MÉTODO GENÉRICO (EL QUE YA TENÍAS) ---
    @Override
public ByteArrayInputStream generatePdfFromTemplate(String templateName, Map<String, Object> data) {
    Context context = new Context();
    context.setVariables(data);

    String rawHtml;
    try {
        rawHtml = templateEngine.process(templateName, context);
    } catch (Exception e) {
        log.error("Error procesando Thymeleaf: {}", templateName);
        throw new RuntimeException("Error en Thymeleaf", e);
    }

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        Document doc = Jsoup.parse(rawHtml, "UTF-8");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        String cleanHtml = doc.html();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        
        // --- CONFIGURACIÓN DE SEGURIDAD PARA FUENTES ---
        builder.useFastMode();
        
        // Esta línea es clave: le dice al motor que ignore las fuentes del sistema 
        // que dan error y use solo las que definas o las básicas.
        builder.testMode(true); 

        builder.withHtmlContent(cleanHtml, ""); 
        builder.toStream(outputStream);
        builder.run();
        
        return new ByteArrayInputStream(outputStream.toByteArray());
        
    } catch (Exception e) {
        log.error("Error crítico al generar el PDF: {}", e.getMessage());
        // Imprimimos la pila de error completa solo en caso de fallo real
        e.printStackTrace(); 
        throw new RuntimeException("Error fatal generando PDF");
    }
}

    // --- 2. MÉTODO ESPECÍFICO (EL QUE TE FALTABA PARA QUITAR EL ERROR) ---
    @Override
    public ByteArrayInputStream generarRecetaMedica(Visita visita) {
        // Buscamos los datos para que el PDF no salga vacío
        Clinica clinica = clinicaRepository.findById(visita.getClinicaId()).orElse(new Clinica());
        Mascota mascota = mascotaRepository.findById(visita.getMascotaId()).orElse(new Mascota());
        Usuario dueno = usuarioRepository.findById(mascota.getPropietarioId()).orElse(new Usuario());

        // Preparamos el mapa de datos
        Map<String, Object> data = new HashMap<>();
        data.put("visita", visita);
        data.put("clinica", clinica);
        data.put("mascota", mascota);
        data.put("dueno", dueno);

        // Reutilizamos el método genérico que ya funciona perfectamente
        return generatePdfFromTemplate("clinica/pdf_receta", data);
    }
}