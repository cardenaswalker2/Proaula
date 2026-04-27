package com.clinicaapp.controller;

import com.clinicaapp.dto.CitaDisplayDTO;
import com.clinicaapp.dto.RegistroClinicaDTO;
import com.clinicaapp.dto.VisitaDTO;
import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Mascota;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.Visita;
import com.clinicaapp.model.enums.EstadoClinica;
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.repository.VisitaRepository;
import com.clinicaapp.service.ICitaService;
import com.clinicaapp.service.IMascotaService;
import com.clinicaapp.service.IVisitaService;
import com.clinicaapp.service.IPdfService;
import com.clinicaapp.service.IClinicaService;
import com.clinicaapp.service.IUsuarioService;
import com.clinicaapp.service.IServicioService;
import com.clinicaapp.service.IEmailService;
import com.clinicaapp.service.INotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clinica")
public class ClinicaController {

    private static final Logger log = LoggerFactory.getLogger(ClinicaController.class);

    @Autowired
    private ClinicaRepository clinicaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VisitaRepository visitaRepository;
    @Autowired
    private ICitaService citaService;
    @Autowired
    private IMascotaService mascotaService;
    @Autowired
    private IVisitaService visitaService;
    @Autowired
    private IPdfService pdfService;
    @Autowired
    private IClinicaService clinicaService;
    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private IServicioService servicioService;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private INotificacionService notificacionService;

    // --- MÉTODO AUXILIAR PARA OBTENER LA CLÍNICA CORRECTA ---
    private Clinica obtenerClinicaLogueada(Authentication auth) {
        if (auth == null)
            return null;
        Usuario usuario = usuarioRepository.findByEmail(auth.getName());
        if (usuario == null)
            return null;
        return clinicaRepository.findByUsuarioAdminId(usuario.getId());
    }

    // 1. DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Clinica clinica = obtenerClinicaLogueada(authentication);
        if (clinica == null)
            return "redirect:/login?error=no_clinica";

        if (clinica.getEstado() != EstadoClinica.APROBADA) {
            return "redirect:/login?error=pendiente";
        }

        List<CitaDisplayDTO> todas = citaService.getCitasByClinicaEmail(clinica.getEmail());
        List<CitaDisplayDTO> salaDeEspera = todas.stream()
                .filter(c -> "En Espera".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        double ingresosTotales = todas.stream()
                .filter(c -> "PAGADO".equalsIgnoreCase(c.getEstadoPago()))
                .mapToDouble(c -> c.getCosto() != null ? c.getCosto() : 0.0)
                .sum();

        model.addAttribute("clinica", clinica);
        model.addAttribute("salaDeEspera", salaDeEspera);
        model.addAttribute("totalCitas", todas.size());
        model.addAttribute("totalIngresos", ingresosTotales);

        // --- 1. DATOS PARA GRÁFICO DE ESTADOS DE CITAS ---
        Map<String, Long> statsEstados = todas.stream()
                .filter(c -> c.getEstado() != null)
                .collect(Collectors.groupingBy(c -> {
                    String est = c.getEstado();
                    if (est.contains("Pendiente")) return "Pendiente";
                    return est;
                }, Collectors.counting()));
        
        List<String> labelsEstados = Arrays.asList("En Espera", "Pendiente", "Confirmada", "Completada", "Cancelada");
        List<Long> dataEstados = labelsEstados.stream()
                .map(e -> statsEstados.getOrDefault(e, 0L))
                .collect(Collectors.toList());
        
        model.addAttribute("labelsEstadosJson", toJson(labelsEstados));
        model.addAttribute("dataEstadosJson", toJson(dataEstados));

        // --- 2. DATOS PARA GRÁFICO DE INGRESOS SEMANALES ---
        List<String> diasSemana = new ArrayList<>();
        List<Double> ingresosSemana = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate d = hoy.minusDays(i);
            diasSemana.add(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")));
            
            double sum = todas.stream()
                .filter(c -> c.getFechaHoraIso() != null && LocalDateTime.parse(c.getFechaHoraIso()).toLocalDate().equals(d))
                .filter(c -> "PAGADO".equalsIgnoreCase(c.getEstadoPago()))
                .mapToDouble(c -> c.getCosto() != null ? c.getCosto() : 0.0)
                .sum();
            ingresosSemana.add(sum);
        }
        
        model.addAttribute("labelsIngresosJson", toJson(diasSemana));
        model.addAttribute("dataIngresosJson", toJson(ingresosSemana));

        // --- 3. DATOS PARA SERVICIOS POPULARES ---
        Map<String, Long> countServicios = todas.stream()
                .filter(c -> c.getNombreServicio() != null)
                .collect(Collectors.groupingBy(CitaDisplayDTO::getNombreServicio, Collectors.counting()));
        
        List<Map.Entry<String, Long>> topServicios = countServicios.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("labelsTopServiciosJson", toJson(topServicios.stream().map(Map.Entry::getKey).collect(Collectors.toList())));
        model.addAttribute("dataTopServiciosJson", toJson(topServicios.stream().map(Map.Entry::getValue).collect(Collectors.toList())));

        return "clinica/dashboard";
    }

    private String toJson(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // 2. LISTADO DE CITAS
    @GetMapping("/citas")
    public String verCitas(Authentication authentication, Model model) {
        Clinica clinica = obtenerClinicaLogueada(authentication);
        if (clinica == null)
            return "redirect:/login";
        model.addAttribute("citas", citaService.getCitasByClinicaEmail(clinica.getEmail()));
        model.addAttribute("clinica", clinica);
        return "clinica/citas";
    }

    // 3. REGISTRAR ATENCIÓN MÉDICA (FORMULARIO)
    @GetMapping("/atender/{citaId}")
    public String formAtenderPaciente(@PathVariable String citaId, Model model, Authentication auth) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        Optional<Cita> citaOpt = citaService.findById(citaId);

        if (citaOpt.isEmpty() || clinica == null)
            return "redirect:/clinica/citas";

        Cita cita = citaOpt.get();
        VisitaDTO visitaDTO = new VisitaDTO();
        visitaDTO.setCitaId(cita.getId());
        visitaDTO.setMascotaId(cita.getMascotaId());
        visitaDTO.setClinicaId(clinica.getId());
        visitaDTO.setFechaVisita(LocalDateTime.now());
        visitaDTO.setCostoTotal(0.0);

        model.addAttribute("nombreMascota", mascotaService.findById(cita.getMascotaId())
                .map(Mascota::getNombre).orElse("Paciente"));

        List<String> nombresServicios = new ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioService.findById(sId).ifPresent(s -> nombresServicios.add(s.getNombre()));
            }
        }
        model.addAttribute("serviciosSolicitados", String.join(", ", nombresServicios));

        model.addAttribute("visitaDTO", visitaDTO);
        model.addAttribute("clinica", clinica);
        return "clinica/form_visita";
    }

    // 4. GUARDAR ATENCIÓN Y ENVIAR RECETA
    @PostMapping("/visitas/guardar")
    public String guardarAtencion(@ModelAttribute VisitaDTO visitaDTO, Authentication auth) {
        // 1. Guardamos la visita en la BD
        Visita visitaSaved = visitaService.save(visitaDTO);
        // 2. Cerramos la cita
        citaService.completarCita(visitaDTO.getCitaId());

        try {
            Clinica clinica = obtenerClinicaLogueada(auth);
            Mascota mascota = mascotaService.findById(visitaDTO.getMascotaId()).orElseThrow();
            Usuario dueno = usuarioService.findById(mascota.getPropietarioId()).orElseThrow();

            // 2.1 Generar Notificación Interna
            String msgNotif = "Se ha completado la atención de " + mascota.getNombre() + ".";
            if (visitaSaved.getFechaProximaCita() != null) {
                msgNotif += " Tu próxima cita es el " + visitaSaved.getFechaProximaCita().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ".";
            }
            notificacionService.crearNotificacion(
                dueno.getId(), 
                "¡Consulta Médica Finalizada!", 
                msgNotif, 
                "/usuario/historial/" + mascota.getId()
            );

            // 3. Generamos el PDF para adjuntarlo
            Map<String, Object> data = new HashMap<>();
            data.put("visita", visitaSaved);
            data.put("clinica", clinica);
            data.put("mascota", mascota);
            data.put("dueno", dueno);

            ByteArrayInputStream bis = pdfService.generatePdfFromTemplate("clinica/pdf_receta", data);
            byte[] pdfBytes = bis.readAllBytes();

            // --- DISEÑO DE EMAIL MÉDICO PREMIUM ---
            String subject = "📄 Receta Médica: " + mascota.getNombre() + " - " + clinica.getNombre();

            String htmlBody = String.format(
                    """
                            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; padding: 40px 0;">
                                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">

                                    <!-- Encabezado con Gradiente Azul Médico -->
                                    <div style="background: linear-gradient(135deg, #0284c7 0%%, #0369a1 100%%); padding: 35px 20px; text-align: center; color: white;">
                                        <div style="background: rgba(255,255,255,0.2); width: 60px; height: 60px; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px;">
                                            <span style="font-size: 30px;">🩺</span>
                                        </div>
                                        <h1 style="margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">Resumen de Consulta Médica</h1>
                                        <p style="margin: 5px 0 0; opacity: 0.8; font-size: 14px;">%s</p>
                                    </div>

                                    <div style="padding: 40px; color: #334155;">
                                        <p style="font-size: 16px;">Hola <strong>%s</strong>,</p>
                                        <p style="line-height: 1.6; font-size: 15px; color: #475569;">
                                            Esperamos que <strong>%s</strong> se encuentre mucho mejor. Te enviamos los detalles de la atención recibida el día de hoy y la receta médica digital adjunta a este correo.
                                        </p>

                                        <!-- Caja de Resumen de la Mascota -->
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin: 25px 0;">
                                            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                                                <span style="color: #64748b; font-size: 13px; font-weight: 600; text-transform: uppercase;">Paciente:</span>
                                                <strong style="color: #0f172a;">%s</strong>
                                            </div>
                                            <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
                                                <span style="color: #64748b; font-size: 13px; font-weight: 600; text-transform: uppercase;">Servicio:</span>
                                                <strong style="color: #0f172a;">Consulta Veterinaria</strong>
                                            </div>
                                            <div style="border-top: 1px solid #e2e8f0; margin-top: 15px; padding-top: 15px;">
                                                <p style="margin: 0; color: #64748b; font-size: 13px; font-weight: 600;">DIAGNÓSTICO:</p>
                                                <p style="margin: 5px 0 0; color: #334155; font-style: italic;">"%s"</p>
                                            </div>
                                        </div>

                                        <div style="text-align: center; margin: 35px 0;">
                                            <p style="font-size: 14px; color: #64748b; margin-bottom: 15px;">Para ver las instrucciones detalladas y dosis, abre el PDF adjunto.</p>
                                            <div style="background: #f0f9ff; color: #0369a1; display: inline-block; padding: 10px 20px; border-radius: 8px; font-weight: 700; border: 1px dashed #0369a1;">
                                                📎 Archivo adjunto: Receta_%s.pdf
                                            </div>
                                        </div>

                                        <p style="font-size: 14px; line-height: 1.6; color: #64748b; text-align: center;">
                                            Si tienes dudas sobre el tratamiento, comunícate con nosotros al <br>
                                            <strong style="color: #0f172a;">%s</strong>
                                        </p>
                                    </div>

                                    <!-- Footer -->
                                    <div style="background-color: #f8fafc; padding: 25px; text-align: center; border-top: 1px solid #e2e8f0;">
                                        <p style="margin: 0; font-size: 12px; color: #94a3b8;">
                                            Este es un documento médico oficial generado por ClínicaApp.<br>
                                            Consérvalo para el historial de tu mascota.
                                        </p>
                                    </div>
                                </div>
                            </div>
                            """,
                    clinica.getNombre(),
                    dueno.getNombre(),
                    mascota.getNombre(),
                    mascota.getNombre(),
                    visitaSaved.getDiagnostico(), // Asegúrate que tu modelo Visita tenga getDiagnostico()
                    mascota.getNombre(),
                    clinica.getTelefono());

            // 4. Enviamos el correo con el diseño y el adjunto
            emailService.sendMessageWithAttachment(
                    dueno.getEmail(),
                    subject,
                    htmlBody,
                    "Receta_" + mascota.getNombre() + ".pdf",
                    pdfBytes);

            log.info("✅ Receta Premium enviada con éxito a: {}", dueno.getEmail());

        } catch (Exception e) {
            log.error("❌ Error al procesar envío de receta: {}", e.getMessage());
        }
        return "redirect:/clinica/citas?success=atendida";
    }

    // 5. ACCIONES DE CITAS
    @PostMapping("/citas/confirmar/{id}")
    public String confirmarCita(@PathVariable String id) {
        citaService.confirmarCita(id);
        return "redirect:/clinica/citas?success=confirmada";
    }

    @PostMapping("/citas/cancelar/{id}")
    public String cancelarCita(@PathVariable String id) {
        citaService.cancelarCita(id);
        return "redirect:/clinica/citas?success=cancelada";
    }

    // 6. PERFIL DE SEDE
    @GetMapping("/perfil")
    public String mostrarPerfil(Authentication authentication, Model model) {
        Clinica clinica = obtenerClinicaLogueada(authentication);
        if (clinica == null)
            return "redirect:/login";
        model.addAttribute("clinica", clinica);
        return "clinica/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute Clinica clinicaEditada, Authentication authentication) {
        Clinica clinicaOriginal = obtenerClinicaLogueada(authentication);
        if (clinicaOriginal != null) {
            clinicaOriginal.setNombre(clinicaEditada.getNombre());
            clinicaOriginal.setDireccion(clinicaEditada.getDireccion());
            clinicaOriginal.setTelefono(clinicaEditada.getTelefono());
            clinicaOriginal.setDescripcion(clinicaEditada.getDescripcion());
            clinicaRepository.save(clinicaOriginal);
        }
        return "redirect:/clinica/perfil?success";
    }

    // 7. HISTORIAL DE MASCOTA
    @GetMapping("/mascota/{mascotaId}/historial")
    public String verHistorialMascota(@PathVariable String mascotaId, Model model, Authentication auth) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        Optional<Mascota> mascotaOpt = mascotaService.findById(mascotaId);
        if (mascotaOpt.isEmpty() || clinica == null)
            return "redirect:/clinica/citas?error";

        model.addAttribute("mascota", mascotaOpt.get());
        model.addAttribute("historial", visitaService.getHistorialByMascota(mascotaId));
        model.addAttribute("clinica", clinica);
        return "clinica/historial_mascota";
    }

    // 8. DESCARGAR RECETA PDF
    @GetMapping("/descargar-receta/{visitaId}")
    public ResponseEntity<InputStreamResource> descargarReceta(@PathVariable String visitaId, Authentication auth) {
        Optional<Visita> visitaOpt = visitaService.findById(visitaId);
        Clinica clinica = obtenerClinicaLogueada(auth);

        if (visitaOpt.isEmpty() || clinica == null)
            return ResponseEntity.notFound().build();

        Visita visita = visitaOpt.get();
        Mascota mascota = mascotaService.findById(visita.getMascotaId()).orElse(new Mascota());
        Usuario dueno = usuarioService.findById(mascota.getPropietarioId()).orElse(new Usuario());

        Map<String, Object> data = new HashMap<>();
        data.put("visita", visita);
        data.put("clinica", clinica);
        data.put("mascota", mascota);
        data.put("dueno", dueno);

        ByteArrayInputStream bis = pdfService.generatePdfFromTemplate("clinica/pdf_receta", data);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=receta_" + mascota.getNombre() + ".pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    // 9. GESTIÓN DE SERVICIOS Y PRECIOS
    @GetMapping("/mis-servicios")
    public String gestionarServicios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Authentication auth, Model model) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        if (clinica == null)
            return "redirect:/login";

        Pageable pageable = PageRequest.of(page, size);
        Page<Servicio> serviciosPage;

        if (search != null && !search.trim().isEmpty()) {
            serviciosPage = servicioService.findByNombre(search, pageable);
        } else {
            serviciosPage = servicioService.findAll(pageable);
        }

        model.addAttribute("clinica", clinica);
        model.addAttribute("serviciosPage", serviciosPage);
        model.addAttribute("serviciosMaestros", serviciosPage.getContent());
        model.addAttribute("misPrecios", clinica.getPreciosServicios());
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);

        return "clinica/gestion_servicios";
    }

    @PostMapping("/mis-servicios/guardar")
    public String guardarServicios(
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "displayedIds", required = false) List<String> displayedIds,
            Authentication auth) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        if (clinica == null)
            return "redirect:/login";

        final Map<String, Double> preciosActuales = (clinica.getPreciosServicios() != null)
                ? clinica.getPreciosServicios()
                : new HashMap<>();

        // Si tenemos la lista de IDs que se mostraron en esta página
        if (displayedIds != null) {
            for (String sId : displayedIds) {
                String ofreceKey = "ofrece_" + sId;
                String precioKey = "precio_" + sId;

                // Si el checkbox de "ofrece" está marcado
                if (allParams.containsKey(ofreceKey)) {
                    String precioStr = allParams.get(precioKey);
                    if (precioStr != null && !precioStr.isEmpty()) {
                        preciosActuales.put(sId, Double.parseDouble(precioStr));
                    }
                } else {
                    // Si no está marcado, lo quitamos de los servicios ofrecidos por esta clínica
                    preciosActuales.remove(sId);
                }
            }
        } else {
            // Fallback: lógica antigua si no hay displayedIds (por seguridad)
            allParams.forEach((key, value) -> {
                if (key.startsWith("precio_") && !value.isEmpty()) {
                    String sId = key.replace("precio_", "");
                    if (allParams.containsKey("ofrece_" + sId)) {
                        preciosActuales.put(sId, Double.parseDouble(value));
                    }
                }
            });
        }

        clinica.setPreciosServicios(preciosActuales);
        clinica.setServiciosOfrecidos(new ArrayList<>(preciosActuales.keySet()));
        clinicaRepository.save(clinica);

        // Redirigir a la misma página que estaba (si podemos obtenerla de allParams o
        // similar)
        String page = allParams.getOrDefault("page", "0");
        return "redirect:/clinica/mis-servicios?success&page=" + page;
    }

    // 10. REPORTE DE VENTAS
    @GetMapping("/reporte-ventas")
    public String reporteVentas(Authentication authentication, Model model) {
        Clinica clinica = obtenerClinicaLogueada(authentication);
        if (clinica == null)
            return "redirect:/login";

        List<CitaDisplayDTO> ventas = citaService.getCitasByClinicaEmail(clinica.getEmail()).stream()
                .filter(c -> "PAGADO".equalsIgnoreCase(c.getEstadoPago()))
                .collect(Collectors.toList());

        double totalRecaudado = ventas.stream().mapToDouble(v -> v.getCosto() != null ? v.getCosto() : 0.0).sum();

        model.addAttribute("clinica", clinica);
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("ticketPromedio", ventas.isEmpty() ? 0.0 : totalRecaudado / ventas.size());

        return "clinica/reporte_ventas";
    }

    // 11. HORARIO LABORAL
    @GetMapping("/horario")
    public String mostrarHorario(Authentication auth, Model model) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        if (clinica == null)
            return "redirect:/login";
        model.addAttribute("clinica", clinica);
        return "clinica/horario";
    }

    @PostMapping("/horario/guardar")
    public String guardarHorario(@RequestParam String horaApertura, @RequestParam String horaCierre,
            @RequestParam int duracionTurnoMinutos, Authentication auth) {
        Clinica clinica = obtenerClinicaLogueada(auth);
        if (clinica != null) {
            clinica.setHoraApertura(horaApertura);
            clinica.setHoraCierre(horaCierre);
            clinica.setDuracionTurnoMinutos(duracionTurnoMinutos);
            clinicaRepository.save(clinica);
        }
        return "redirect:/clinica/dashboard?horarioActualizado";
    }

    // 12. REGISTRO PÚBLICO DE CLÍNICA
    @GetMapping("/registrar-clinica")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("registroDto", new RegistroClinicaDTO());
        return "registro_clinica";
    }

    @PostMapping("/registrar-clinica")
    public String procesarRegistroClinica(@ModelAttribute("registroDto") RegistroClinicaDTO registroDto,
                                          @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                                          RedirectAttributes flash) {
        try {
            clinicaService.registrarSolicitudClinica(registroDto, imagenFile);
            return "registro_exitoso";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Hubo un error: " + e.getMessage());
            return "redirect:/clinica/registrar-clinica";
        }
    }
}