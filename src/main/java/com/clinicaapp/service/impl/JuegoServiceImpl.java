package com.clinicaapp.service.impl;

import com.clinicaapp.model.MascotaVirtual;
import com.clinicaapp.repository.MascotaVirtualRepository;
import com.clinicaapp.service.IJuegoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class JuegoServiceImpl implements IJuegoService {

    @Autowired
    private MascotaVirtualRepository repo;

    @Override
    public MascotaVirtual obtenerMascotaUsuario(String usuarioId) {
        MascotaVirtual mv = repo.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    MascotaVirtual nueva = new MascotaVirtual(usuarioId, "Nova-Pet");
                    return repo.save(nueva);
                });
        
        sincronizarEstado(mv);
        return repo.save(mv);
    }

    @Override
    public void sincronizarEstado(MascotaVirtual mv) {
        LocalDateTime ahora = LocalDateTime.now();
        long minutosTranscurridos = Duration.between(mv.getUltimaActualizacion(), ahora).toMinutes();
        
        if (minutosTranscurridos > 0) {
            // Desgaste natural (ej: 0.1 de hambre por minuto = 6 por hora)
            mv.setHambre(mv.getHambre() + (minutosTranscurridos * 0.1));
            mv.setEnergia(mv.getEnergia() - (minutosTranscurridos * 0.05));
            
            // Penalización por negligencia
            if (mv.getHambre() > 80) {
                mv.setSalud(mv.getSalud() - (minutosTranscurridos * 0.05));
                mv.setFelicidad(mv.getFelicidad() - (minutosTranscurridos * 0.1));
            }
            
            mv.setUltimaActualizacion(ahora);
        }
    }

    @Override
    public MascotaVirtual realizarAccion(String usuarioId, String accion) {
        MascotaVirtual mv = obtenerMascotaUsuario(usuarioId);
        
        switch (accion.toUpperCase()) {
            case "FEED":
                mv.setHambre(mv.getHambre() - 30);
                mv.setSalud(mv.getSalud() + 5);
                break;
            case "PLAY":
                if (mv.getEnergia() > 20) {
                    mv.setFelicidad(mv.getFelicidad() + 25);
                    mv.setEnergia(mv.getEnergia() - 15);
                    mv.setHambre(mv.getHambre() + 10);
                }
                break;
            case "HEAL":
                mv.setSalud(mv.getSalud() + 40);
                mv.setFelicidad(mv.getFelicidad() - 10);
                break;
            case "SLEEP":
                mv.setEnergia(100);
                break;
        }
        
        mv.setUltimaActualizacion(LocalDateTime.now());
        return repo.save(mv);
    }
}
