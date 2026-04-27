package com.clinicaapp.controller;

import com.clinicaapp.model.Clinica;
import com.clinicaapp.service.IClinicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ClinicaRestController {

    @Autowired private IClinicaService clinicaService;

    // Endpoint simple que devuelve una lista de coincidencias (máx 10)
    @GetMapping("/api/clinicas/search")
    public List<ClinicaSummary> search(@RequestParam("q") String q) {
        Pageable p = PageRequest.of(0, 10);
        return clinicaService.search(q, p)
                .getContent()
                .stream()
                .map(c -> new ClinicaSummary(c.getId(), c.getNombre()))
                .collect(Collectors.toList());
    }

    public static class ClinicaSummary {
        private String id;
        private String nombre;

        public ClinicaSummary() {}
        public ClinicaSummary(String id, String nombre) { this.id = id; this.nombre = nombre; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }
}
