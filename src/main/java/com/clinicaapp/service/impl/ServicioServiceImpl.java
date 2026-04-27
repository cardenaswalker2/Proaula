package com.clinicaapp.service.impl;

import com.clinicaapp.model.Servicio;
import com.clinicaapp.repository.ServicioRepository;
import com.clinicaapp.service.IServicioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ServicioServiceImpl implements IServicioService {
    @Autowired private ServicioRepository servicioRepository;

    @Override public Servicio save(Servicio servicio) { return servicioRepository.save(servicio); }
    @Override public List<Servicio> findAll() { return servicioRepository.findAll(); }
    @Override public Page<Servicio> findAll(Pageable pageable) { return servicioRepository.findAll(pageable); }
    @Override public Page<Servicio> findByNombre(String nombre, Pageable pageable) { return servicioRepository.findByNombreContainingIgnoreCase(nombre, pageable); }
    @Override public Optional<Servicio> findById(String id) { return servicioRepository.findById(id); }
    @Override public List<Servicio> findByIds(List<String> ids) { return (List<Servicio>) servicioRepository.findAllById(ids); }

    private static final Logger log = LoggerFactory.getLogger(ServicioServiceImpl.class);

    @Override
    public void deleteById(String id) {
    if (servicioRepository.existsById(id)) {
        servicioRepository.deleteById(id);
        log.info("Servicio maestro con ID {} eliminado por el administrador", id);
    } else {
        throw new RuntimeException("El servicio no existe");
    }
}
}