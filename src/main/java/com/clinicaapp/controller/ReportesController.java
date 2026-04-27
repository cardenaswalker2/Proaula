package com.clinicaapp.controller;

import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.service.ICitaService;
import com.clinicaapp.service.IPdfService;
import com.clinicaapp.service.IServicioService; // <-- Asegúrate de tener este import
import com.clinicaapp.service.IUsuarioService;
import com.clinicaapp.service.IVisitaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clinicaapp.dto.ServicioReporteDTO;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;

@Controller
@RequestMapping("/admin/reportes")
public class ReportesController {

    @Autowired private IPdfService pdfService;
    @Autowired private ICitaService citaService;
    @Autowired private IVisitaService visitaService;
    @Autowired private IUsuarioService usuarioService;
    
    // ==========================================================
    // LÍNEA FALTANTE AÑADIDA
    // ==========================================================
    @Autowired private IServicioService servicioService;

    @GetMapping
    public String indexReportes(Model model) {
        
        // Obtenemos los datos base una sola vez
        List<Cita> todasLasCitas = citaService.findAll();
        List<Usuario> todosLosUsuarios = usuarioService.findAllUsers();
        
        // --- 1. DATOS PARA LOS KPIs (TARJETAS) ---
        double ingresosTotales = todasLasCitas.stream()
                .filter(c -> "PAGADO".equals(c.getEstadoPago()))
                .mapToDouble(Cita::getCosto)
                .sum();
        model.addAttribute("ingresosTotales", ingresosTotales);

        long citasCompletadas = todasLasCitas.stream()
                .filter(c -> "Completada".equals(c.getEstado()))
                .count();
        model.addAttribute("citasCompletadas", citasCompletadas);
        
        long nuevosClientesMes = todosLosUsuarios.stream()
                .filter(u -> u.getFechaCreacion() != null && 
                             u.getFechaCreacion().getMonth() == LocalDate.now().getMonth() &&
                             u.getFechaCreacion().getYear() == LocalDate.now().getYear())
                .count();
        model.addAttribute("nuevosClientesMes", nuevosClientesMes);
        
        double ticketPromedio = (citasCompletadas > 0) ? (ingresosTotales / citasCompletadas) : 0.0;
        model.addAttribute("ticketPromedio", ticketPromedio);

        // --- 2. DATOS PARA EL GRÁFICO DE CITAS POR ESTADO ---
        Map<String, Long> citasPorEstado = todasLasCitas.stream()
                .filter(cita -> cita.getEstado() != null)
                .collect(Collectors.groupingBy(Cita::getEstado, Collectors.counting()));
        List<String> labelsCitas = List.of("Pendiente de Pago", "Pendiente", "Confirmada", "Completada", "Cancelada");
        List<Long> dataCitas = labelsCitas.stream()
                .map(estado -> citasPorEstado.getOrDefault(estado, 0L))
                .collect(Collectors.toList());
        model.addAttribute("labelsCitas", toJson(labelsCitas));
        model.addAttribute("dataCitas", toJson(dataCitas));

        // --- 3. DATOS PARA EL GRÁFICO DE NUEVOS USUARIOS POR MES ---
        Map<Month, Long> usuariosPorMes = todosLosUsuarios.stream()
                .filter(u -> u.getFechaCreacion() != null && u.getFechaCreacion().getYear() == Year.now().getValue())
                .collect(Collectors.groupingBy(u -> u.getFechaCreacion().getMonth(), Collectors.counting()));
        List<String> labelsUsuarios = Arrays.stream(Month.values())
                .map(m -> m.toString().substring(0, 1) + m.toString().substring(1).toLowerCase())
                .collect(Collectors.toList());
        List<Long> dataUsuarios = Arrays.stream(Month.values())
                .map(mes -> usuariosPorMes.getOrDefault(mes, 0L))
                .collect(Collectors.toList());
        model.addAttribute("labelsUsuarios", toJson(labelsUsuarios));
        model.addAttribute("dataUsuarios", toJson(dataUsuarios));
        
        // --- 4. DATOS PARA EL GRÁFICO DE SERVICIOS MÁS POPULARES ---
        Map<String, Long> serviciosPopulares = todasLasCitas.stream()
                .filter(c -> c.getServicioId() != null)
                .collect(Collectors.groupingBy(Cita::getServicioId, Collectors.counting()));
        
        List<String> labelsServicios = serviciosPopulares.keySet().stream()
                .map(id -> servicioService.findById(id)
                        .map(Servicio::getNombre)
                        .orElse("ID: " + id.substring(0, 8) + "..."))
                .collect(Collectors.toList());
                
        List<Long> dataServicios = new ArrayList<>(serviciosPopulares.values());

        model.addAttribute("labelsServicios", toJson(labelsServicios));
        model.addAttribute("dataServicios", toJson(dataServicios));
        
        return "admin/reportes";
    }

    private String toJson(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @GetMapping("/descargar/resumen-mensual")
public ResponseEntity<InputStreamResource> descargarReporteMensual() {
    
    // --- 1. LÓGICA PARA CALCULAR DATOS REALES DEL MES ACTUAL ---
    LocalDate hoy = LocalDate.now();
    Month mesActual = hoy.getMonth();
    int anioActual = hoy.getYear();

    List<Cita> citasDelMes = citaService.findAll().stream()
            .filter(c -> c.getFechaHora() != null &&
                         c.getFechaHora().getMonth() == mesActual &&
                         c.getFechaHora().getYear() == anioActual)
            .collect(Collectors.toList());

    List<Usuario> usuariosDelMes = usuarioService.findAllUsers().stream()
            .filter(u -> u.getFechaCreacion() != null &&
                         u.getFechaCreacion().getMonth() == mesActual &&
                         u.getFechaCreacion().getYear() == anioActual)
            .collect(Collectors.toList());

    double ingresosMes = citasDelMes.stream()
            .filter(c -> "PAGADO".equals(c.getEstadoPago()))
            .mapToDouble(Cita::getCosto)
            .sum();

    long totalVisitasMes = citasDelMes.stream()
            .filter(c -> "Completada".equals(c.getEstado()))
            .count();
            
    long nuevosClientes = usuariosDelMes.size();

    double ticketPromedio = (totalVisitasMes > 0) ? (ingresosMes / totalVisitasMes) : 0.0;

    // Lógica para servicios más rentables del mes
    List<ServicioReporteDTO> serviciosPopulares = citasDelMes.stream()
            .filter(c -> "PAGADO".equals(c.getEstadoPago()))
            .collect(Collectors.groupingBy(Cita::getServicioId))
            .entrySet().stream()
            .map(entry -> {
                ServicioReporteDTO dto = new ServicioReporteDTO();
                String servicioId = entry.getKey();
                List<Cita> citasPorServicio = entry.getValue();
                
                dto.setNombre(servicioService.findById(servicioId).map(Servicio::getNombre).orElse("Desconocido"));
                dto.setCantidad((long) citasPorServicio.size());
                dto.setIngresos(citasPorServicio.stream().mapToDouble(Cita::getCosto).sum());
                return dto;
            })
            .sorted(Comparator.comparing(ServicioReporteDTO::getIngresos).reversed()) // Ordenar por ingresos
            .limit(5) // Mostrar solo el top 5
            .collect(Collectors.toList());

    // --- 2. PREPARAR DATOS PARA LA PLANTILLA ---
    Map<String, Object> data = new HashMap<>();
    
    // Formatear el nombre del mes en español
    String nombreMes = mesActual.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
    data.put("mesReporte", Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " " + anioActual);
    
    data.put("ingresosMes", ingresosMes);
    data.put("totalVisitasMes", totalVisitasMes);
    data.put("nuevosClientes", nuevosClientes);
    data.put("ticketPromedio", ticketPromedio);
    data.put("serviciosPopulares", serviciosPopulares);
    data.put("fechaGeneracion", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

    // --- 3. Generar y devolver el PDF ---
    ByteArrayInputStream pdfStream = pdfService.generatePdfFromTemplate("pdf/reporte_template", data);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "inline; filename=reporte_mensual.pdf");

    return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(pdfStream));
}
}