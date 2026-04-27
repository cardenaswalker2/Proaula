package com.clinicaapp.service;
import com.clinicaapp.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface IServicioService {
    Servicio save(Servicio servicio);
    List<Servicio> findAll();
    Page<Servicio> findAll(Pageable pageable);
    Page<Servicio> findByNombre(String nombre, Pageable pageable);
    Optional<Servicio> findById(String id);
    List<Servicio> findByIds(List<String> ids);
    void deleteById(String id);
}