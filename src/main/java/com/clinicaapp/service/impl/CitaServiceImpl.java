package com.clinicaapp.service.impl;

import com.clinicaapp.dto.CitaDTO;
import com.clinicaapp.dto.CitaDisplayDTO;
import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.ConfiguracionGlobal;
import com.clinicaapp.model.Mascota;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.repository.CitaRepository;
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.repository.ConfiguracionRepository;
import com.clinicaapp.repository.MascotaRepository;
import com.clinicaapp.repository.ServicioRepository;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.service.ICitaService;
import com.clinicaapp.service.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CitaServiceImpl implements ICitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaServiceImpl.class);

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClinicaRepository clinicaRepository;
    @Autowired
    private MascotaRepository mascotaRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private IEmailService emailService;

    @Autowired
    private ConfiguracionRepository configRepo;

    @Override
    public List<Cita> findAll() {
        return citaRepository.findAll();
    }

    @Override
    public Optional<Cita> findById(String id) {
        return citaRepository.findById(id);
    }

    @Override
    public Cita save(CitaDTO citaDTO) {
        // 1. Extraer IDs del DTO
        String usuarioId = citaDTO.getUsuarioId();
        String clinicaId = citaDTO.getClinicaId();
        String mascotaId = citaDTO.getMascotaId();
        List<String> serviciosIds = citaDTO.getServiciosIds();

        // 2. Validaciones básicas de entrada
        if (!StringUtils.hasText(usuarioId))
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo.");
        if (!StringUtils.hasText(clinicaId))
            throw new IllegalArgumentException("Debe seleccionar una clínica.");
        if (!StringUtils.hasText(mascotaId))
            throw new IllegalArgumentException("Debe seleccionar una mascota.");
        if (serviciosIds == null || serviciosIds.isEmpty())
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio.");

        // 3. Buscar la CLÍNICA para obtener su catálogo de precios personalizado
        Clinica clinica = clinicaRepository.findById(clinicaId)
                .orElseThrow(() -> new RuntimeException("La clínica seleccionada no existe."));

        // 4. Determinar el costo total de la cita
        double costoTotal = 0.0;

        for (String sId : serviciosIds) {
            Double costoServicio = 0.0;
            // Verificamos si la clínica tiene un precio específico para este servicio
            if (clinica.getPreciosServicios() != null && clinica.getPreciosServicios().containsKey(sId)) {
                costoServicio = clinica.getPreciosServicios().get(sId);
                log.info("Servicio {}: Usando precio personalizado de la clínica: ${}", sId, costoServicio);
            } else {
                // Si la clínica NO tiene precio propio, buscamos el precio global del servicio
                Servicio servicioGlobal = servicioRepository.findById(sId)
                        .orElseThrow(() -> new RuntimeException("Servicio " + sId + " no encontrado."));
                costoServicio = servicioGlobal.getCosto();
                log.info("Servicio {}: Clínica no tiene precio asignado. Usando precio global: ${}", sId,
                        costoServicio);
            }
            costoTotal += costoServicio;
        }

        // --- 5. NUEVO: Lógica de Configuración Global (Comisión) ---
        ConfiguracionGlobal globalConfig = configRepo.findById("GLOBAL_SETTINGS")
                .orElse(new ConfiguracionGlobal());

        double porcentajeComision = globalConfig.getComisionStripe();
        double gananciaPlataforma = costoTotal * (porcentajeComision / 100);

        log.info("Configuración aplicada: Comisión del {}%. Ganancia estimada plataforma: ${}",
                porcentajeComision, gananciaPlataforma);

        // 6. Crear y configurar la entidad Cita
        Cita cita = new Cita();
        BeanUtils.copyProperties(citaDTO, cita, "id");

        cita.setUsuarioId(usuarioId);
        cita.setServiciosIds(serviciosIds);
        cita.setCosto(costoTotal); // El cliente paga el precio total definido por la clínica
        cita.setEstado("Pendiente de Pago");
        cita.setEstadoPago("PENDIENTE");

        log.info("Registrando cita: Usuario {} en Clínica {} por un valor de ${}", usuarioId, clinicaId, costoTotal);

        return citaRepository.save(cita);
    }

    @Override
    public void deleteById(String id) {
        citaRepository.deleteById(id);
    }

    @Override
    public List<Cita> findByUsuarioId(String userId) {
        return citaRepository.findByUsuarioId(userId);
    }

    @Override
    public List<Cita> findByClinicaId(String clinicaId) {
        return citaRepository.findByClinicaId(clinicaId);
    }

    @Override
    public List<CitaDisplayDTO> getCitasByClinicaEmail(String email) {
        // 1. Buscamos la clínica por email para obtener su ID
        Clinica clinica = clinicaRepository.findByEmail(email);

        if (clinica == null) {
            log.warn("No se encontró clínica para el email: {}", email);
            return List.of();
        }

        // 2. Buscamos las citas usando el ID de la clínica (este campo SÍ existe en
        // Cita)
        List<Cita> citas = citaRepository.findByClinicaId(clinica.getId());

        // 3. Convertimos a DTO
        return citas.stream()
                .map(this::convertToDisplayDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Cita> findAllCitasPendientes() {
        return citaRepository.findByEstadoAndEstadoPago("Pendiente", "PAGADO");
    }

    @Override
    public List<Cita> findAllCitasActivasParaRecepcion() {
        List<String> estadosActivos = List.of("Pendiente", "Confirmada");
        return citaRepository.findByEstadoInAndEstadoPago(estadosActivos, "PAGADO");
    }

    private Cita findCitaByIdOrThrow(String id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada."));
    }

    @Override
    public void confirmarCita(String id) {
        Cita cita = findCitaByIdOrThrow(id);
        cita.setEstado("Confirmada");
        citaRepository.save(cita);
        enviarEmailConfirmacionCita(cita);
    }

    @Override
    public void cancelarCita(String id) {
        Cita cita = findCitaByIdOrThrow(id);
        cita.setEstado("Cancelada");
        citaRepository.save(cita);
    }

    @Override
    public void completarCita(String id) {
        Cita cita = findCitaByIdOrThrow(id);
        cita.setEstado("Completada");
        citaRepository.save(cita);
    }

    @Override
    public void addPaymentIntentToCita(String citaId, String paymentIntentId) {
        Cita cita = findCitaByIdOrThrow(citaId);
        cita.setPaymentIntentId(paymentIntentId);
        citaRepository.save(cita);
    }

    @Override
    public void markCitaAsPaid(String paymentIntentId) {
        Cita cita = citaRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada para el pago."));
        cita.setEstadoPago("PAGADO");
        cita.setEstado("Pendiente");
        citaRepository.save(cita);
    }

    @Override
    public void marcarComoPagadaPorRecepcion(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada: " + citaId));
        cita.setEstadoPago("PAGADO");
        cita.setEstado("Confirmada");
        cita.setPaymentIntentId("RECEPCION-" + citaId); // ← ID único por cita, no fijo
        citaRepository.save(cita);
    }

    @Override
    public List<Cita> findByEstadoAndEstadoPago(String estado, String estadoPago) {
        return citaRepository.findByEstadoAndEstadoPago(estado, estadoPago);
    }

    @Override
    public void reprogramarCita(String id, LocalDateTime nuevaFechaHora) {
        Cita cita = findCitaByIdOrThrow(id);
        cita.setFechaHora(nuevaFechaHora);
        citaRepository.save(cita);
    }

    // --- MÉTODOS PRIVADOS DE APOYO ---

    private CitaDisplayDTO convertToDisplayDTO(Cita cita) {
        Usuario usuario = usuarioRepository.findById(cita.getUsuarioId()).orElse(null);
        Clinica clinica = clinicaRepository.findById(cita.getClinicaId()).orElse(null);
        Mascota mascota = mascotaRepository.findById(cita.getMascotaId()).orElse(null);

        List<Servicio> servicios = new ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioRepository.findById(sId).ifPresent(servicios::add);
            }
        }

        return new CitaDisplayDTO(cita, usuario, clinica, mascota, servicios);
    }

    private void enviarEmailConfirmacionCita(Cita cita) {
        try {
            // Obtener datos del usuario
            Usuario usuario = usuarioRepository.findById(cita.getUsuarioId()).orElse(null);
            if (usuario == null) {
                log.warn("No se encontró usuario para enviar email de confirmación de cita {}", cita.getId());
                return;
            }

            // Obtener datos de los servicios
            List<String> nombresServicios = new ArrayList<>();
            if (cita.getServiciosIds() != null) {
                for (String sId : cita.getServiciosIds()) {
                    servicioRepository.findById(sId).ifPresent(s -> nombresServicios.add(s.getNombre()));
                }
            }
            String listaServicios = String.join(", ", nombresServicios);

            // Formatear fecha
            String fechaCita = cita.getFechaHora() != null
                    ? cita.getFechaHora()
                            .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy 'a las' HH:mm",
                                    new Locale("es", "ES")))
                    : "Fecha por confirmar";

            String emailSubject = "✅ ¡Tu Cita en ClínicaApp ha sido Confirmada!";

            String emailBody = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    /* ... existing styles ... */
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1>✅ ¡Cita Confirmada!</h1>
                                        <p>Tu cita ha sido agendada exitosamente</p>
                                    </div>
                                    <div class="content">
                                        <p class="greeting">Hola <strong>%s %s</strong>,</p>
                                        
                                        <div class="confirmation-badge">
                                            ✓ CONFIRMADA
                                        </div>
                                        
                                        <p class="message">Nos complace confirmarte que tu cita para los servicios de <strong>%s</strong> ha sido agendada exitosamente.</p>
                                        
                                        <div class="info-box">
                                            <h3>📋 Detalles de tu Cita</h3>
                                            <div class="info-row">
                                                <span class="info-label">Servicios:</span>
                                                <span class="info-value">%s</span>
                                            </div>
                                            <div class="info-row">
                                                <span class="info-label">Fecha y Hora:</span>
                                                <span class="info-value">%s</span>
                                            </div>
                                            <div class="info-row">
                                                <span class="info-label">Estado:</span>
                                                <span class="info-value" style="color: #4caf50; font-weight: bold;">Confirmada</span>
                                            </div>
                                            <div class="info-row">
                                                <span class="info-label">ID de Cita:</span>
                                                <span class="info-value">%s</span>
                                            </div>
                                        </div>
                                        
                                        <div class="costo-highlight">
                                            <div class="label">Costo Total</div>
                                            <div class="valor">$%.2f</div>
                                        </div>
                                        
                                        <div class="tips-box">
                                            <h4>💡 Recomendaciones para tu cita:</h4>
                                            <ul>
                                                <li>Llega 15 minutos antes de tu hora programada</li>
                                                <li>Trae tu identificación oficial</li>
                                                <li>Si necesitas cancelar o reprogramar, hazlo con al menos 24h de anticipación</li>
                                            </ul>
                                        </div>
                                        
                                        <center>
                                            <a href="http://localhost:8080/usuario/mis-citas" class="btn">Ver mis Citas</a>
                                        </center>
                                        
                                        <div class="footer">
                                            <p>¿Necesitas ayuda? Contáctanos en <a href="mailto:soporte@clinicaapp.com">soporte@clinicaapp.com</a></p>
                                            <p>© 2024 ClínicaApp - Todos los derechos reservados</p>
                                        </div>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    usuario.getNombre(),
                    usuario.getApellido() != null ? usuario.getApellido() : "",
                    listaServicios,
                    listaServicios,
                    fechaCita,
                    cita.getId(),
                    cita.getCosto() != null ? cita.getCosto() : 0.0);

            emailService.sendSimpleMessage(usuario.getEmail(), emailSubject, emailBody);
            log.info("Email de confirmación de cita enviado a {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("Error al enviar email de confirmación de cita: {}", e.getMessage());
            // No lanzamos excepción para que la confirmación no falle
        }

    }

    @Override
    public void registrarLlegada(String id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // Este es el estado que el ClinicaController busca para mostrar la caja azul
        cita.setEstado("En Espera");
        citaRepository.save(cita);
    }
}