package com.clinicaapp.controller;

import com.clinicaapp.dto.CitaDTO;
import com.clinicaapp.dto.CitaDisplayDTO;
import com.clinicaapp.dto.MascotaDTO;
import com.clinicaapp.dto.PagoListDTO;
import com.clinicaapp.dto.ResenaDTO;
import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.model.*; // Importar Visita
import com.clinicaapp.model.enums.Especie;
import com.clinicaapp.model.enums.RazaGato;
import com.clinicaapp.model.enums.RazaPerro;
// <-- Asegúrate de tener este import y el Autowired
import com.clinicaapp.service.*; // Importar IVisitaService

// lombok.AllArgsConstructor removed (unused)
import lombok.Data;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult; // <-- Añade este import

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime; // Asegúrate de importar esto
import org.springframework.format.annotation.DateTimeFormat; // Y esto también

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.InputStreamResource; // <-- Añade este import al inicio del archivo
import org.springframework.http.HttpHeaders; // <-- Añade este import
import org.springframework.http.MediaType; // <-- Añade este import
import org.springframework.http.ResponseEntity; // <-- Añade este import
import java.io.ByteArrayInputStream; // <-- Añade este import
import java.util.ArrayList;
import java.util.HashMap; // <-- Añade este import
import java.util.Map; // <-- Añade este import
import com.clinicaapp.dto.PaymentDTO;
import com.clinicaapp.dto.CitaDisplayDTO;

import java.util.Comparator; // <-- ESTE ES EL QUE FALTA

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private IMascotaService mascotaService;
    @Autowired
    private ICitaService citaService;
    @Autowired
    private IClinicaService clinicaService;
    @Autowired
    private IVisitaService visitaService; // Añadir el servicio de visitas
    @Autowired
    private IPdfService pdfService;
    @Autowired
    private IServicioService servicioService;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private IResenaService resenaService;
    @Autowired
    private IPredictiveHealthService predictiveHealthService;
    @Autowired
    private IJuegoService juegoService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Principal principal) {
        if (principal != null) {
            Usuario usuario = getLoggedUser(principal);
            if (usuario != null) {
                int cantidadMascotas = mascotaService.findByPropietarioId(usuario.getId()).size();
                model.addAttribute("cantidadMascotas", cantidadMascotas);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    // Método auxiliar para obtener el Usuario logueado
    private Usuario getLoggedUser(Principal principal) {
        if (principal == null)
            return null;

        // Si es una autenticación de Spring Security, extraer el principal real
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            Object p = auth.getPrincipal();

            log.debug("getLoggedUser: principal class={}, principal.toString={}",
                    p != null ? p.getClass().getName() : "null", String.valueOf(p));

            // OAuth2 principal (viene con atributos como 'email')
            if (p instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) p;
                Object emailObj = oauthUser.getAttributes().get("email");
                log.debug("OAuth2User attributes={}", oauthUser.getAttributes());
                if (emailObj != null) {
                    return usuarioService.findByEmail(emailObj.toString());
                }
            }

            // UserDetails (username as email)
            if (p instanceof UserDetails) {
                String username = ((UserDetails) p).getUsername();
                return usuarioService.findByEmail(username);
            }
        }

        // Fallback: intentar con principal.getName()
        try {
            String name = principal.getName();
            log.debug("getLoggedUser: fallback principal.getName()={}", name);
            return usuarioService.findByEmail(name);
        } catch (Exception e) {
            log.error("Error buscando usuario por principal.getName(): {}", e.toString());
            return null;
        }
    }

    // Método auxiliar display DTO
    private List<CitaDisplayDTO> convertToDisplayDTO(List<Cita> citas) {
        return citas.stream().map(cita -> {
            Usuario u = usuarioService.findById(cita.getUsuarioId()).orElse(null);
            Clinica c = clinicaService.findById(cita.getClinicaId()).orElse(null);
            Mascota m = mascotaService.findById(cita.getMascotaId()).orElse(null);
            
            List<Servicio> servicios = new ArrayList<>();
            if (cita.getServiciosIds() != null) {
                for (String sId : cita.getServiciosIds()) {
                    servicioService.findById(sId).ifPresent(servicios::add);
                }
            }
            return new CitaDisplayDTO(cita, u, c, m, servicios);
        }).collect(Collectors.toList());
    }

    // DENTRO DE UsuarioController.java

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null)
            return "redirect:/login?error=noauth";

        model.addAttribute("usuario", usuario);

        // 1. Buscamos todas las citas
        List<Cita> misCitasRaw = citaService.findByUsuarioId(usuario.getId());

        // 2. Filtramos y ORDENAMOS (Aquí es donde se usa el Comparator)
        List<CitaDisplayDTO> citasActivas = convertToDisplayDTO(misCitasRaw.stream()
                .filter(c -> !"Cancelada".equalsIgnoreCase(c.getEstado()))
                // Ordenar: Las más recientes primero
                .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList()));

        model.addAttribute("citasActivas", citasActivas);

        // 3. Mascotas para el panel lateral
        List<Mascota> misMascotas = mascotaService.findByPropietarioId(usuario.getId());
        model.addAttribute("mascotas", misMascotas);

        return "usuario/dashboard_usuario_privado";
    }

    // --- Gestión de Mascotas ---
    // DENTRO DE UsuarioController.java

    @GetMapping("/mis-mascotas") // Antes decía "/mascotas", eso causaba el 404
    public String misMascotas(Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null) return "redirect:/login?error=noauth";

        List<Mascota> mascotas = mascotaService.findByPropietarioId(usuario.getId());
        model.addAttribute("mascotas", mascotas);
        
        // Retorna el archivo que tienes en templates/usuario/mis_mascotas.html
        return "usuario/mis_mascotas"; 
    }

    @GetMapping("/mascotas/nueva")
    public String formNuevaMascota(Model model) {
        model.addAttribute("mascotaDTO", new MascotaDTO());
        cargarEnumsMascota(model);
        return "usuario/form_editar_mascota";
    }

    @GetMapping("/mascotas/editar/{id}")
    public String formEditarMascota(@PathVariable String id, Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        Optional<Mascota> mascotaOpt = mascotaService.findByIdAndPropietarioId(id, usuario.getId());

        if (mascotaOpt.isPresent()) {
            MascotaDTO dto = new MascotaDTO();
            BeanUtils.copyProperties(mascotaOpt.get(), dto);
            model.addAttribute("mascotaDTO", dto);
            cargarEnumsMascota(model);
            return "usuario/form_editar_mascota";
        }
        return "redirect:/usuario/mis-mascotas?error=noauth";
    }

    @GetMapping("/historial/{id}")
    public String historialMascota(@PathVariable String id, Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        Optional<Mascota> mascotaOpt = mascotaService.findByIdAndPropietarioId(id, usuario.getId());

        if (mascotaOpt.isPresent()) {
            Mascota mascota = mascotaOpt.get();
            List<Visita> visitas = visitaService.findByMascotaId(mascota.getId());

            // Construimos una lista enriquecida con nombre de clínica y veterinario
            List<HistorialVisitaDTO> historialDTOs = visitas.stream().map(visita -> {
                String nombreClinica = clinicaService.findById(visita.getClinicaId())
                        .map(Clinica::getNombre)
                        .orElse("Clínica Desconocida");

                String nombreVeterinario = visita.getVeterinarioId() != null
                        ? usuarioService.findById(visita.getVeterinarioId())
                                .map(vet -> vet.getNombre() + " "
                                        + (vet.getApellido() != null ? vet.getApellido() : ""))
                                .orElse("N/A")
                        : "N/A";

                return new HistorialVisitaDTO(visita, nombreClinica, nombreVeterinario);
            }).collect(Collectors.toList());

            model.addAttribute("mascota", mascota);
            model.addAttribute("visitasDTO", historialDTOs); // Pasamos la nueva lista

            // --- LLAMADA AL MOTOR DE PREDICCIÓN NOVA AI ---
            model.addAttribute("prediccion", predictiveHealthService.generarPrediccion(mascota, visitas));

            return "usuario/historial_mascota";
        }
        return "redirect:/usuario/mis-mascotas?error=noauth";
    }

    private void cargarEnumsMascota(Model model) {
        model.addAttribute("especies", Especie.values());
        model.addAttribute("razasPerro", RazaPerro.values());
        model.addAttribute("razasGato", RazaGato.values());
    }

    @PostMapping("/mascotas/guardar")
    public String guardarMascota(@ModelAttribute MascotaDTO mascotaDTO,
                                 @RequestParam(value = "fotoFile", required = false) org.springframework.web.multipart.MultipartFile fotoFile,
                                 Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        mascotaDTO.setPropietarioId(usuario.getId());

        if (fotoFile != null && !fotoFile.isEmpty()) {
            try {
                String uploadDir = "uploads";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                String fileName = java.util.UUID.randomUUID().toString() + "_" + fotoFile.getOriginalFilename();
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                java.nio.file.Files.copy(fotoFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                mascotaDTO.setFotoUrl("/uploads/" + fileName);
            } catch (java.io.IOException e) {
                log.error("Error guardando foto de mascota", e);
            }
        } else {
            if (mascotaDTO.getId() != null && !mascotaDTO.getId().isEmpty()) {
                mascotaService.findById(mascotaDTO.getId()).ifPresent(existing -> mascotaDTO.setFotoUrl(existing.getFotoUrl()));
            }
        }

        mascotaService.save(mascotaDTO);
        return "redirect:/usuario/mis-mascotas";
    }

    // --- Gestión de Citas (Solicitar y Ver) ---
    @GetMapping("/citas/solicitar")
    public String formSolicitarCita(
            @RequestParam(required = false) String clinicaId,
            @RequestParam(required = false) String mascotaId,
            @RequestParam(required = false) String servicioId,
            @RequestParam(required = false) String fechaHora,
            @RequestParam(required = false) String motivo,
            Model model,
            Principal principal) {
        // 1. Obtener el usuario logueado
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null) {
            // Medida de seguridad: si no se encuentra el usuario, redirigir al login
            return "redirect:/login";
        }

        // 2. Crear un DTO para el formulario (con precarga de IA si aplica)
        CitaDTO citaDTO = new CitaDTO();
        if (mascotaId != null && !mascotaId.isEmpty()) citaDTO.setMascotaId(mascotaId);
        if (servicioId != null && !servicioId.isEmpty()) citaDTO.setServiciosIds(List.of(servicioId));
        if (fechaHora != null && !fechaHora.isEmpty()) {
            try { citaDTO.setFechaHora(LocalDateTime.parse(fechaHora)); } catch (Exception e) {}
        }
        if (motivo != null && !motivo.isEmpty()) citaDTO.setMotivo(motivo);
        
        model.addAttribute("citaDTO", citaDTO);

        // 3. Cargar las mascotas del usuario
        List<Mascota> misMascotas = mascotaService.findByPropietarioId(usuario.getId());
        model.addAttribute("misMascotas", misMascotas);

        // 4. Manejar la clínica (preselección o lista completa)
        if (clinicaId != null && !clinicaId.isEmpty()) {
            // Si venimos de "detalle_clinica", preseleccionamos esa clínica
            clinicaService.findById(clinicaId).ifPresent(c -> {
                model.addAttribute("clinicaPreseleccionada", c);
                citaDTO.setClinicaId(c.getId()); // Asignar el ID al DTO
            });
            // No cargamos todas las clínicas para no tener duplicados en el select
            model.addAttribute("clinicas", List.of()); // Lista vacía
        } else {
            // Si venimos del dashboard, cargamos todas las clínicas
            model.addAttribute("clinicas", clinicaService.findAll());
        }

        // 5. Devolver la vista
        return "usuario/form_solicitar_cita";
    }

    @PostMapping("/citas/guardar")
    public String guardarSolicitudCita(@ModelAttribute("citaDTO") CitaDTO citaDTO,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        Usuario usuario = getLoggedUser(principal);
        if (usuario == null) {
            return "redirect:/login";
        }

        // --- MANEJO DE ERRORES BÁSICO ---
        // Si hay un problema con los datos del formulario (ej. fecha mal formada),
        // volvemos al formulario.
        if (bindingResult.hasErrors()) {
            System.err.println("Error de binding al guardar cita: " + bindingResult.getAllErrors());
            // Recargamos los datos para que la vista no se rompa
            model.addAttribute("misMascotas", mascotaService.findByPropietarioId(usuario.getId()));
            model.addAttribute("clinicas", clinicaService.findAll());
            model.addAttribute("error", "Datos inválidos. Por favor, revisa el formato de la fecha.");
            return "usuario/form_solicitar_cita";
        }

        // Completamos el DTO con el ID del usuario
        citaDTO.setUsuarioId(usuario.getId());

        try {
            // Guardamos la cita. El servicio se encargará de calcular el costo y ponerla
            // como "PENDIENTE"
            Cita citaGuardada = citaService.save(citaDTO);

            // --- ¡CAMBIO CRUCIAL! ---
            // En lugar de redirigir a "Mis Citas", redirigimos al checkout con el ID de la
            // nueva cita.
            return "redirect:/usuario/checkout/" + citaGuardada.getId();

        } catch (Exception e) {
            // Si el servicio falla (ej: servicio no encontrado), volvemos al formulario
            System.err.println("Error en el servicio al guardar cita: " + e.getMessage());
            model.addAttribute("misMascotas", mascotaService.findByPropietarioId(usuario.getId()));
            model.addAttribute("clinicas", clinicaService.findAll());
            model.addAttribute("error", "No se pudo crear la cita. Por favor, inténtalo de nuevo.");
            return "usuario/form_solicitar_cita";
        }
    }

    @GetMapping("/mis-citas")
    public String misCitas(Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        List<Cita> citas = citaService.findByUsuarioId(usuario.getId());
        model.addAttribute("citas", convertToDisplayDTO(citas));
        return "usuario/mis_citas";
    }

    // --- Perfil ---
    @GetMapping("/perfil")
    public String verPerfil(Model model, Principal principal,
            @RequestParam(name = "fromWelcome", required = false, defaultValue = "false") boolean fromWelcome) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null)
            return "redirect:/login";

        UsuarioRegistroDTO dto = new UsuarioRegistroDTO();

        // Copia nombre, apellido, email, telefono
        BeanUtils.copyProperties(usuario, dto);

        // Evitamos pasar la contraseña por seguridad
        dto.setPassword(null);

        // --- LÍNEA CRUCIAL: Pasamos el estado del login facial al DTO ---
        dto.setFacialLoginHabilitado(usuario.isFacialLoginHabilitado());

        model.addAttribute("usuarioDTO", dto);
        model.addAttribute("fromWelcome", fromWelcome);

        return "usuario/form_perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@ModelAttribute UsuarioRegistroDTO usuarioDTO,
            @RequestParam(name = "fromWelcome", required = false, defaultValue = "false") boolean fromWelcome,
            Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        // Actualizamos el usuario con los datos del formulario
        usuarioService.update(usuario.getId(), usuarioDTO);

        // Si venimos desde el flujo de bienvenida, enviamos al dashboard normal
        if (fromWelcome) {
            return "redirect:/usuario/dashboard?perfilCompletadoFromWelcome";
        }

        return "redirect:/usuario/dashboard?perfilActualizado";
    }

    @GetMapping("/historial/{id}/pdf")
    public ResponseEntity<InputStreamResource> descargarHistorialPdf(@PathVariable String id, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        // Verificamos que la mascota sea del usuario por seguridad
        Optional<Mascota> mascotaOpt = mascotaService.findByIdAndPropietarioId(id, usuario.getId());

        if (mascotaOpt.isPresent()) {
            Mascota mascota = mascotaOpt.get();
            List<Visita> visitas = visitaService.findByMascotaId(mascota.getId());

            // Construimos visitasDTO enriquecido (similar a historial view)
            List<HistorialVisitaDTO> visitasDTO = visitas.stream().map(visita -> {
                String nombreClinica = clinicaService.findById(visita.getClinicaId())
                        .map(Clinica::getNombre)
                        .orElse("Clínica Desconocida");

                String nombreVeterinario = visita.getVeterinarioId() != null
                        ? usuarioService.findById(visita.getVeterinarioId())
                                .map(vet -> vet.getNombre() + " "
                                        + (vet.getApellido() != null ? vet.getApellido() : ""))
                                .orElse("N/A")
                        : "N/A";

                return new HistorialVisitaDTO(visita, nombreClinica, nombreVeterinario);
            }).collect(Collectors.toList());

            // 1. Preparamos los datos que la plantilla PDF necesita
            Map<String, Object> data = new HashMap<>();
            data.put("mascota", mascota);
            data.put("visitasDTO", visitasDTO);
            data.put("nombrePropietario", usuario.getNombre() + " " + usuario.getApellido());
            data.put("propietarioTelefono", usuario.getTelefono());
            data.put("propietarioEmail", usuario.getEmail());
            data.put("fechaGeneracion", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            // 2. Llamamos al servicio para generar el PDF en memoria
            try {
                log.info("Generating PDF for mascota id={}, mascota.nombre={}, visitasCount={}", mascota.getId(),
                        mascota.getNombre(), visitasDTO.size());
                log.debug("Data keys passed to template: {}", data.keySet());
                ByteArrayInputStream pdfStream = pdfService.generatePdfFromTemplate("pdf/historial_template", data);

                // 3. Preparamos los encabezados de la respuesta HTTP para forzar la descarga
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition",
                        "attachment; filename=historial_clinico_" + mascota.getNombre() + ".pdf");

                // 4. Devolvemos la respuesta
                return ResponseEntity
                        .ok()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new InputStreamResource(pdfStream));
            } catch (Exception e) {
                log.error("Error generating PDF for mascota id={}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(500).build();
            }
        }

        // Si la mascota no existe o no pertenece al usuario, devolvemos un error
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/servicios-clinica/{clinicaId}")
    @ResponseBody // <-- Muy importante: indica que no devuelve una vista, sino datos (JSON)
    public List<Servicio> getServiciosPorClinica(@PathVariable String clinicaId) {
        Clinica clinica = clinicaService.findById(clinicaId)
                .orElseThrow(() -> new RuntimeException("Clínica no encontrada con ID: " + clinicaId));

        // Log para depuración: mostrar id de clínica y número de servicios
        // referenciados
        int serviciosCount = (clinica.getServiciosOfrecidos() == null) ? 0 : clinica.getServiciosOfrecidos().size();
        log.info("getServiciosPorClinica: clinicaId={} serviciosReferenced={}", clinicaId, serviciosCount);

        // Verificamos si la clínica tiene servicios asignados
        if (clinica.getServiciosOfrecidos() == null || clinica.getServiciosOfrecidos().isEmpty()) {
            return List.of(); // Devolvemos una lista vacía
        }

        return servicioService.findByIds(clinica.getServiciosOfrecidos());
    }

    @PostMapping("/historial/{id}/enviar-email")
    public String enviarHistorialPorEmail(@PathVariable String id,
            @RequestParam("emailDestino") String emailDestino, // <-- NUEVO PARÁMETRO
            Principal principal,
            RedirectAttributes redirectAttributes) { // <-- USAMOS RedirectAttributes

        Usuario usuario = getLoggedUser(principal);
        Optional<Mascota> mascotaOpt = mascotaService.findByIdAndPropietarioId(id, usuario.getId());

        if (mascotaOpt.isPresent()) {
            // ... (lógica para preparar los datos y generar el PDF, no cambia)
            Mascota mascota = mascotaOpt.get();
            List<Visita> visitas = visitaService.findByMascotaId(mascota.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("mascota", mascota);
            data.put("visitas", visitas);
            data.put("nombrePropietario", usuario.getNombre() + " " + usuario.getApellido());
            data.put("propietarioTelefono", usuario.getTelefono()); // Asumo que tu clase Usuario tiene un método
                                                                    // getTelefono()
            data.put("propietarioEmail", usuario.getEmail());
            data.put("fechaGeneracion", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            ByteArrayInputStream pdfStream = pdfService.generatePdfFromTemplate("pdf/historial_template", data);

            try {
                String subject = "Historial Clínico de " + mascota.getNombre();
                String body = String.format("...");
                String attachmentName = "Historial_" + mascota.getNombre() + ".pdf";

                emailService.sendMessageWithAttachment(
                        emailDestino, // <-- Usamos el email del formulario
                        subject,
                        body,
                        attachmentName,
                        pdfStream.readAllBytes());

                // Usamos RedirectAttributes para pasar el email a la URL de éxito
                redirectAttributes.addAttribute("emailEnviado", "true");
                redirectAttributes.addAttribute("email", emailDestino);
                return "redirect:/usuario/historial/" + id;

            } catch (Exception e) {
                log.error("No se pudo enviar el historial por email para la mascota {}: {}", id, e.getMessage());
                return "redirect:/usuario/historial/" + id + "?errorEmail=true";
            }
        }
        return "redirect:/usuario/mis-mascotas?error=noauth";
    }

    @Data // Lombok
    class HistorialVisitaDTO {
        private Visita visita;
        private String nombreClinica;
        private String veterinarioNombre;
        private String propietarioTelefono;
        private String propietarioEmail;

        public HistorialVisitaDTO() {
        }

        public HistorialVisitaDTO(Visita visita, String nombreClinica) {
            this.visita = visita;
            this.nombreClinica = nombreClinica;
        }

        public HistorialVisitaDTO(Visita visita, String nombreClinica, String veterinarioNombre) {
            this.visita = visita;
            this.nombreClinica = nombreClinica;
            this.veterinarioNombre = veterinarioNombre;
        }

        public Visita getVisita() {
            return visita;
        }

        public void setVisita(Visita visita) {
            this.visita = visita;
        }

        public String getNombreClinica() {
            return nombreClinica;
        }

        public void setNombreClinica(String nombreClinica) {
            this.nombreClinica = nombreClinica;
        }

        public String getVeterinarioNombre() {
            return veterinarioNombre;
        }

        public void setVeterinarioNombre(String veterinarioNombre) {
            this.veterinarioNombre = veterinarioNombre;
        }

        public String getPropietarioTelefono() {
            return propietarioTelefono;
        }

        public void setPropietarioTelefono(String propietarioTelefono) {
            this.propietarioTelefono = propietarioTelefono;
        }

        public String getPropietarioEmail() {
            return propietarioEmail;
        }

        public void setPropietarioEmail(String propietarioEmail) {
            this.propietarioEmail = propietarioEmail;
        }
    }

    @GetMapping("/resena/nueva")
    public String formNuevaResena(@RequestParam("citaId") String citaId, Model model,
            RedirectAttributes redirectAttributes) {
        // Verificamos si ya existe una reseña para esta cita
        if (resenaService.yaExisteResenaParaCita(citaId)) {
            redirectAttributes.addFlashAttribute("mensajeInfo", "Ya has dejado una reseña para esta cita.");
            return "redirect:/usuario/mis-citas";
        }

        // Pasamos los datos necesarios a la vista
        ResenaDTO resenaDTO = new ResenaDTO();
        resenaDTO.setCitaId(citaId);
        model.addAttribute("resenaDTO", resenaDTO);

        // Opcional: pasar información de la cita a la vista para mostrar un resumen
        // Cita cita = citaService.findById(citaId).orElse(null);
        // model.addAttribute("cita", cita);

        return "usuario/form_resena";
    }

    // --- MÉTODO POST PARA GUARDAR LA RESEÑA ---
    @PostMapping("/resena/guardar")
    public String guardarResena(@ModelAttribute("resenaDTO") ResenaDTO resenaDTO, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // Verificamos de nuevo que no se envíen reseñas duplicadas
            if (resenaService.yaExisteResenaParaCita(resenaDTO.getCitaId())) {
                redirectAttributes.addFlashAttribute("mensajeError", "Error: Ya existe una reseña para esta cita.");
                return "redirect:/usuario/mis-citas";
            }

            resenaService.save(resenaDTO);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Gracias! Tu reseña ha sido publicada.");
        } catch (Exception e) {
            log.error("Error al guardar la reseña: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Hubo un problema al guardar tu reseña.");
        }

        return "redirect:/usuario/mis-citas";
    }

    @GetMapping("/welcome")
    public String welcomePage(Model model, Principal principal) {
        // Obtenemos el usuario para poder mostrar su nombre en la bienvenida
        Usuario usuario = getLoggedUser(principal);
        model.addAttribute("usuario", usuario);
        return "usuario/welcome";
    }

    // ------------------ Pagos (Mi historial de pagos) ------------------
    @GetMapping("/pagos")
    public String misPagos(Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null)
            return "redirect:/login";

        // Obtenemos todas las citas pagadas del usuario
        List<Cita> citasPagadas = citaService.findByUsuarioId(usuario.getId()).stream()
                .filter(c -> "PAGADO".equals(c.getEstadoPago()))
                .collect(Collectors.toList());

        // Construimos una lista de DTOs con los nombres ya resueltos
        List<PagoListDTO> pagos = citasPagadas.stream().map(cita -> {
            PagoListDTO dto = new PagoListDTO();
            dto.setCitaId(cita.getId());
            dto.setFecha(cita.getFechaHora());
            dto.setMonto(cita.getCosto());
            dto.setPaymentIntentId(cita.getPaymentIntentId());

            // Buscamos los nombres y los asignamos
            clinicaService.findById(cita.getClinicaId())
                    .ifPresent(c -> dto.setNombreClinica(c.getNombre()));

            List<String> nombresServicios = new ArrayList<>();
            if (cita.getServiciosIds() != null) {
                for (String sId : cita.getServiciosIds()) {
                    servicioService.findById(sId).ifPresent(s -> nombresServicios.add(s.getNombre()));
                }
            }
            dto.setNombreServicio(String.join(", ", nombresServicios));

            mascotaService.findById(cita.getMascotaId())
                    .ifPresent(m -> dto.setNombreMascota(m.getNombre()));

            return dto;
        }).collect(Collectors.toList());

        model.addAttribute("pagos", pagos);
        return "usuario/mis_pagos"; // Devuelve la vista mejorada
    }

    @GetMapping("/pagos/{citaId}")
    public String verPagoDetalle(@PathVariable String citaId, Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null)
            return "redirect:/login";

        Cita cita = citaService.findById(citaId).orElse(null);
        if (cita == null || !usuario.getId().equals(cita.getUsuarioId())) {
            return "redirect:/usuario/pagos?error=notfound";
        }

        PaymentDTO dto = new PaymentDTO();
        dto.setCitaId(cita.getId());
        dto.setFecha(cita.getFechaHora());
        dto.setMonto(cita.getCosto());
        dto.setPaymentIntentId(cita.getPaymentIntentId());
        dto.setServicioId(cita.getServicioId());
        dto.setClinicaId(cita.getClinicaId());
        dto.setMascotaId(cita.getMascotaId());

        // Resolver nombres legibles
        List<String> nombresServicios = new ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioService.findById(sId).ifPresent(s -> nombresServicios.add(s.getNombre()));
            }
        }
        model.addAttribute("nombreServicio", String.join(", ", nombresServicios));
        clinicaService.findById(cita.getClinicaId()).ifPresent(c -> model.addAttribute("nombreClinica", c.getNombre()));
        mascotaService.findById(cita.getMascotaId()).ifPresent(m -> model.addAttribute("nombreMascota", m.getNombre()));

        model.addAttribute("pago", dto);
        return "usuario/pago_detalle";
    }

    @GetMapping("/pagos/factura/{citaId}/pdf")
    public ResponseEntity<byte[]> descargarFacturaPdf(@PathVariable String citaId, Principal principal) {
        // Validar que la cita pertenece al usuario logueado
        String usuarioId = getLoggedUser(principal).getId();
        Cita cita = citaService.findById(citaId).orElse(null);

        if (cita == null || !cita.getUsuarioId().equals(usuarioId) || !"PAGADO".equals(cita.getEstadoPago())) {
            return ResponseEntity.status(403).build(); // No autorizado o no encontrado
        }

        // Reunir toda la información necesaria para la factura
        Clinica clinica = clinicaService.findById(cita.getClinicaId()).orElseThrow();
        Usuario usuario = getLoggedUser(principal);
        Mascota mascota = mascotaService.findById(cita.getMascotaId()).orElseThrow();

        // Crear el mapa de datos para la plantilla de la factura
        Map<String, Object> data = new HashMap<>();
        data.put("nombreClinica", clinica.getNombre());
        data.put("direccionClinica", clinica.getDireccion());
        data.put("emailClinica", clinica.getEmail());
        data.put("numeroFactura", cita.getId().substring(0, 8).toUpperCase());
        data.put("fechaFactura", cita.getFechaHora());
        data.put("paymentIntentId", cita.getPaymentIntentId());
        data.put("nombreCliente", usuario.getNombre() + " " + usuario.getApellido());
        data.put("emailCliente", usuario.getEmail());
        data.put("nombreMascota", mascota.getNombre());

        List<Map<String, Object>> itemsFactura = new ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioService.findById(sId).ifPresent(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("descripcion", s.getNombre());
                    // Buscamos el precio personalizado de la clínica si existe
                    Double precio = (clinica.getPreciosServicios() != null && clinica.getPreciosServicios().containsKey(sId))
                        ? clinica.getPreciosServicios().get(sId)
                        : s.getCosto();
                    item.put("costo", precio);
                    itemsFactura.add(item);
                });
            }
        }
        data.put("itemsFactura", itemsFactura);

        // Calcular totales (ejemplo con IVA del 19%)
        double subtotal = cita.getCosto();
        double impuestos = subtotal * 0.19;
        double costoTotal = subtotal + impuestos;
        data.put("subtotal", subtotal);
        data.put("impuestos", impuestos);
        data.put("costoTotal", costoTotal);

        // Generar el PDF
        ByteArrayInputStream pdfStream = pdfService.generatePdfFromTemplate("pdf/factura_template", data);

        byte[] pdfBytes = pdfStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=factura-" + cita.getId() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/citas/{id}") // <-- CAMBIO 1: Ruta más limpia y estándar
    public String verCitaDetalle(@PathVariable String id, Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null)
            return "redirect:/login";

        // Busca la cita y verifica que pertenezca al usuario
        Optional<Cita> citaOpt = citaService.findById(id);
        if (citaOpt.isEmpty() || !citaOpt.get().getUsuarioId().equals(usuario.getId())) {
            return "redirect:/usuario/mis-citas?error=notfound";
        }

        Cita cita = citaOpt.get();
        Clinica clinica = clinicaService.findById(cita.getClinicaId()).orElse(new Clinica());
        Mascota mascota = mascotaService.findById(cita.getMascotaId()).orElse(new Mascota());

        // Crear el DTO como ya lo haces
        List<Servicio> servicios = new ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioService.findById(sId).ifPresent(servicios::add);
            }
        }

        CitaDisplayDTO citaDto = new CitaDisplayDTO(cita, usuario, clinica, mascota, servicios);
        model.addAttribute("cita", citaDto);

        // Si la cita está completada, buscamos la visita para mostrar resumen médico
        visitaService.findByCitaId(id).ifPresent(v -> model.addAttribute("visita", v));

        // CAMBIO 2: Asegúrate de que el nombre coincide exactamente con tu archivo HTML
        return "usuario/ver_cita";
    }

    @PostMapping("/mis-citas/{id}/cancelar")
    public String cancelarCita(@PathVariable String id, Principal principal, RedirectAttributes attributes) {
        // 1. Obtener el usuario logueado
        Usuario usuario = getLoggedUser(principal);

        // 2. Buscar la cita para asegurarse de que existe
        Optional<Cita> citaOpt = citaService.findById(id);

        if (citaOpt.isEmpty()) {
            attributes.addFlashAttribute("error", "La cita que intentas cancelar no existe.");
            return "redirect:/usuario/mis-citas";
        }

        // 3. ¡MUY IMPORTANTE! Verificar que la cita pertenece al usuario logueado
        Cita cita = citaOpt.get();
        if (!cita.getUsuarioId().equals(usuario.getId())) {
            attributes.addFlashAttribute("error", "No tienes permiso para cancelar esta cita.");
            return "redirect:/usuario/mis-citas";
        }

        // 4. Si todo está bien, llamar al servicio para cancelar
        try {
            citaService.cancelarCita(id);
            attributes.addFlashAttribute("success", "Tu cita ha sido cancelada exitosamente.");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Ocurrió un error al intentar cancelar la cita.");
        }

        return "redirect:/usuario/mis-citas";
    }

    @PostMapping("/mis-citas/{id}/reprogramar")
    public String reprogramarCita(@PathVariable String id,
            @RequestParam("nuevaFechaHora") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevaFechaHora,
            Principal principal,
            RedirectAttributes attributes) {

        Usuario usuario = getLoggedUser(principal);
        Optional<Cita> citaOpt = citaService.findById(id);

        if (citaOpt.isEmpty()) {
            attributes.addFlashAttribute("error", "La cita que intentas reprogramar no existe.");
            return "redirect:/usuario/mis-citas";
        }

        Cita cita = citaOpt.get();
        if (!cita.getUsuarioId().equals(usuario.getId())) {
            attributes.addFlashAttribute("error", "No tienes permiso para reprogramar esta cita.");
            return "redirect:/usuario/mis-citas";
        }

        try {
            citaService.reprogramarCita(id, nuevaFechaHora);
            attributes.addFlashAttribute("success", "Tu cita ha sido reprogramada exitosamente.");
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error al reprogramar: " + e.getMessage());
        }

        return "redirect:/usuario/mis-citas";
    }

    @GetMapping("/juego")
    public String mostrarJuegoMascota(Model model, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        model.addAttribute("mascotaVirtual", juegoService.obtenerMascotaUsuario(usuario.getId()));
        return "juego-mascota";
    }

    @PostMapping("/api/juego/accion")
    @ResponseBody
    public ResponseEntity<MascotaVirtual> realizarAccionJuego(@RequestParam String accion, Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        MascotaVirtual mv = juegoService.realizarAccion(usuario.getId(), accion);
        return ResponseEntity.ok(mv);
    }

    @GetMapping("/api/juego/estado")
    @ResponseBody
    public ResponseEntity<MascotaVirtual> obtenerEstadoJuego(Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        MascotaVirtual mv = juegoService.obtenerMascotaUsuario(usuario.getId());
        return ResponseEntity.ok(mv);
    }

    @GetMapping("/citas/{citaId}/receta")
    public ResponseEntity<InputStreamResource> descargarRecetaUsuario(@PathVariable String citaId,
            Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario == null) return ResponseEntity.status(401).build();

        Optional<Cita> citaOpt = citaService.findById(citaId);

        // Verificación de seguridad: la cita debe existir y ser de este usuario
        if (citaOpt.isPresent() && usuario.getId().equals(citaOpt.get().getUsuarioId())) {

            // Buscamos la visita médica asociada a esa cita
            Optional<Visita> visitaOpt = visitaService.findByCitaId(citaId);

            if (visitaOpt.isPresent()) {
                Visita visita = visitaOpt.get();

                // Check defensivo para IDs nulos que romperían el servicio
                Clinica clinica = (visita.getClinicaId() != null) 
                    ? clinicaService.findById(visita.getClinicaId()).orElse(new Clinica())
                    : new Clinica();
                
                Mascota mascota = (visita.getMascotaId() != null)
                    ? mascotaService.findById(visita.getMascotaId()).orElse(new Mascota())
                    : new Mascota();

                Map<String, Object> data = new HashMap<>();
                data.put("visita", visita);
                data.put("clinica", clinica);
                data.put("mascota", mascota);
                data.put("dueno", usuario);

                try {
                    ByteArrayInputStream bis = pdfService.generatePdfFromTemplate("clinica/pdf_receta", data);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Disposition", "inline; filename=receta_" + citaId + ".pdf");

                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(new InputStreamResource(bis));
                } catch (Exception e) {
                    log.error("Error crítico generando PDF de receta: {}", e.getMessage());
                    return ResponseEntity.status(500).build();
                }
            }
        }
        return ResponseEntity.notFound().build();
    }


    @GetMapping("/api/ai/contexto-completo")
@ResponseBody
public Map<String, Object> getContextoIA(Principal principal) {
    Usuario u = getLoggedUser(principal);
    List<Mascota> mascotas = mascotaService.findByPropietarioId(u.getId());
    List<Cita> citas = citaService.findByUsuarioId(u.getId());
    
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("nombreUsuario", u.getNombre());
    ctx.put("totalMascotas", mascotas.size());
    ctx.put("nombresMascotas", mascotas.stream().map(Mascota::getNombre).collect(Collectors.toList()));
    ctx.put("citasPendientes", citas.stream().filter(c -> !"Cancelada".equalsIgnoreCase(c.getEstado())).count());
    
    // Si hay citas, traer la fecha de la próxima
    if(!citas.isEmpty()){
        ctx.put("proximaCita", citas.get(0).getFechaHora());
    }
    
    return ctx;
}
}