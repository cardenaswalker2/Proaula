package com.clinicaapp.controller;

import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.service.IClinicaService;
import com.clinicaapp.service.IServicioService;
import com.clinicaapp.controller.NovaAIController.NovaRequest;
import com.clinicaapp.controller.NovaAIController.NovaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/public/demo")
public class PublicDemoController {

    @Autowired
    private IClinicaService clinicaService;

    @Autowired
    private IServicioService servicioService;

    @Autowired
    private NovaAIController novaAIController;

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getDemoData() {
        Map<String, Object> data = new HashMap<>();
        
        // Obtenemos un par de clínicas reales para el demo
        List<Clinica> clinicas = clinicaService.findAll().stream().limit(3).toList();
        data.put("clinicas", clinicas);
        
        // Obtenemos servicios comunes
        List<Servicio> servicios = servicioService.findAll().stream().limit(5).toList();
        data.put("servicios", servicios);
        
        return ResponseEntity.ok(data);
    }

    @PostMapping("/think")
    public ResponseEntity<NovaResponse> thinkDemo(@RequestBody NovaRequest request) {
        // En la demo, el contexto es siempre un usuario ficticio llamado "Visitante"
        if (request.getContexto() == null) {
            request.setContexto(new HashMap<>());
        }
        request.getContexto().put("user", "Visitante");
        
        // Reutilizamos el cerebro de Nova AI
        return novaAIController.pensar(request);
    }
}
