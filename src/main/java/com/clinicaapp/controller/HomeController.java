package com.clinicaapp.controller;

import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.service.IClinicaService;
import com.clinicaapp.service.IServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class HomeController {

    @Autowired private IClinicaService clinicaService;
    @Autowired private IServicioService servicioService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        // Obtenemos todas las clínicas
        List<Clinica> todasLasClinicas = clinicaService.findAll();
        
        // Las mezclamos para mostrar diferentes clínicas destacadas cada vez
        Collections.shuffle(todasLasClinicas);
        
        // Tomamos solo las primeras 6 para la página de inicio
        List<Clinica> clinicasDestacadas = todasLasClinicas.stream().limit(6).collect(Collectors.toList());
        
        model.addAttribute("clinicasDestacadas", clinicasDestacadas);
        
        return "index";
    }

    @GetMapping("/buscar-clinicas")
    public String buscarClinicas(@RequestParam(value = "query", required = false) String query,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "9") int size,
                                 Model model) {
        try {
            Pageable paging = PageRequest.of(page, size);
            Page<Clinica> clinicasPage;

            if (query != null && !query.isEmpty()) {
                clinicasPage = clinicaService.search(query, paging);
                model.addAttribute("query", query);
            } else {
                clinicasPage = clinicaService.findAll(paging);
            }

            model.addAttribute("clinicasPage", clinicasPage);
            
            // Lógica de paginación inteligente
            int totalPages = clinicasPage.getTotalPages();
            if (totalPages > 0) {
                int start = Math.max(1, page - 2);
                int end = Math.min(page + 3, totalPages);
                if (page < 3) {
                    end = Math.min(5, totalPages);
                }
                if (page > totalPages - 3) {
                    start = Math.max(1, totalPages - 4);
                }
                List<Integer> pageNumbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
                model.addAttribute("pageNumbers", pageNumbers);
            }

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar las clínicas.");
        }

        return "buscar_clinicas";
    }

    @GetMapping("/detalle-clinica/{id}")
    public String detalleClinica(@PathVariable String id, Model model) {
        Optional<Clinica> clinicaOpt = clinicaService.findById(id);

        if (clinicaOpt.isPresent()) {
            Clinica clinica = clinicaOpt.get();
            model.addAttribute("clinica", clinica);
            
            if (clinica.getServiciosOfrecidos() != null && !clinica.getServiciosOfrecidos().isEmpty()) {
                List<Servicio> serviciosCompletos = servicioService.findByIds(clinica.getServiciosOfrecidos());
                model.addAttribute("servicios", serviciosCompletos);
            } else {
                model.addAttribute("servicios", List.of());
            }

            return "detalle_clinica";
        } else {
            return "redirect:/buscar-clinicas?error=notfound";
        }
    }
    
    // --- MÉTODO FALTANTE AÑADIDO ---
    @GetMapping("/oficina-virtual")
    public String oficinaVirtual() {
        // Este método simplemente se encarga de mostrar la página HTML.
        // Toda la lógica de qué mostrar a cada rol está en el propio archivo
        // oficina_virtual.html usando los atributos de seguridad de Thymeleaf.
        return "oficina_virtual";
    }
    // -----------------------------

    @GetMapping("/publico/demo")
    public String mostrarDemo(Model model) {
    // No pasamos un usuario real, pasamos datos "ficticios" para la demo
    return "demo-interactiva"; 
}
}