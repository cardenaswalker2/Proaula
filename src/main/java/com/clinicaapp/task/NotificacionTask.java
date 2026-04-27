package com.clinicaapp.task;

import com.clinicaapp.model.*;
import com.clinicaapp.repository.*;
import com.clinicaapp.service.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NotificacionTask {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTask.class);

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClinicaRepository clinicaRepository;
    @Autowired private IEmailService emailService;

    // Se ejecuta todos los días a las 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?") 
    public void enviarRecordatoriosCitas() {
        log.info("Iniciando tarea programada: Envío de recordatorios de citas...");

        // 1. Definimos el rango de "Mañana"
        LocalDate mañana = LocalDate.now().plusDays(1);
        LocalDateTime inicioMañana = mañana.atStartOfDay();
        LocalDateTime finMañana = mañana.atTime(LocalTime.MAX);

        // 2. Buscamos citas confirmadas y pagadas para mañana
        List<Cita> citasManana = citaRepository.findByFechaHoraBetweenAndEstadoAndEstadoPago(
                inicioMañana, finMañana, "Confirmada", "PAGADO"
        );

        log.info("Se encontraron {} citas para mañana.", citasManana.size());

        for (Cita cita : citasManana) {
            try {
                Usuario usuario = usuarioRepository.findById(cita.getUsuarioId()).orElse(null);
                Clinica clinica = clinicaRepository.findById(cita.getClinicaId()).orElse(null);

                if (usuario != null && clinica != null) {
                    enviarEmailRecordatorio(usuario, clinica, cita);
                    log.info("Recordatorio enviado a: {}", usuario.getEmail());
                }
            } catch (Exception e) {
                log.error("Error enviando recordatorio para la cita {}: {}", cita.getId(), e.getMessage());
            }
        }
    }

    private void enviarEmailRecordatorio(Usuario u, Clinica c, Cita cita) {
        String hora = cita.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm"));
        String fecha = cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String subject = "⏰ Recordatorio: Mañana tienes una cita en " + c.getNombre();
        
        String body = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;">
                <h2 style="color: #2c3e50; text-align: center;">¡No lo olvides!</h2>
                <p>Hola <strong>%s</strong>,</p>
                <p>Te recordamos que tienes una cita programada para el día de mañana:</p>
                <div style="background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Sede:</strong> %s</p>
                    <p><strong>Dirección:</strong> %s</p>
                    <p><strong>Fecha:</strong> %s</p>
                    <p><strong>Hora:</strong> %s</p>
                </div>
                <p style="color: #7f8c8d; font-size: 12px; text-align: center;">
                    Por favor, llega 10 minutos antes. Si no puedes asistir, cancela desde la app.
                </p>
            </div>
            """, u.getNombre(), c.getNombre(), c.getDireccion(), fecha, hora);

        emailService.sendSimpleMessage(u.getEmail(), subject, body);
    }
}