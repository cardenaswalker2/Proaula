package com.clinicaapp.controller;

import com.clinicaapp.dto.CitaDTO;
import com.clinicaapp.dto.CitaDisplayDTO;
import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.dto.VisitaDTO;
import com.clinicaapp.model.*;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recepcion")
public class RecepcionistaController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionistaController.class);

    @Autowired
    private ICitaService citaService;
    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private IClinicaService clinicaService;
    @Autowired
    private IMascotaService mascotaService;
    @Autowired
    private IVisitaService visitaService;
    @Autowired
    private IServicioService servicioService;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private ISmsService smsService;
    @Autowired
    private INotificacionService notificacionService;
    @Autowired
    private WekaNoShowPredictionService wekaNoShowService;

    // ==========================================================
    // DASHBOARD
    // ==========================================================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Obtenemos todas las citas que tienen significado hoy (no canceladas)
        List<Cita> todasLasCitas = citaService.findAll();
        LocalDate hoy = LocalDate.now();

        List<Cita> citasDeHoy = todasLasCitas.stream()
                .filter(c -> c.getFechaHora() != null && c.getFechaHora().toLocalDate().equals(hoy))
                .filter(c -> !"Cancelada".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        List<CitaDisplayDTO> dtosHoy = convertToDisplayDTO(citasDeHoy);

        // Categorización para el Command Center (Normalización de Estados)
        List<CitaDisplayDTO> programadas = dtosHoy.stream()
                .filter(c -> {
                    String est = c.getEstado();
                    return est != null && (est.contains("Pendiente") || est.equalsIgnoreCase("Confirmada"));
                })
                .collect(Collectors.toList());

        List<CitaDisplayDTO> enEspera = dtosHoy.stream()
                .filter(c -> "En Espera".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        List<CitaDisplayDTO> completadas = dtosHoy.stream()
                .filter(c -> "Completada".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        model.addAttribute("citasActivas", dtosHoy); // Mantener para compatibilidad móvil rápida
        model.addAttribute("programadas", programadas);
        model.addAttribute("enEspera", enEspera);
        model.addAttribute("completadas", completadas);

        // Métricas rápidas
        model.addAttribute("totalHoy", dtosHoy.size());
        model.addAttribute("totalEnEdificio", enEspera.size());
        model.addAttribute("totalAtendidos", completadas.size());

        return "recepcion/dashboard_recepcionista";
    }

    // ==========================================================
    // GESTIÓN DE CITAS
    // ==========================================================
    @GetMapping("/citas")
    public String gestionarCitas(
            @RequestParam(name = "estado", required = false) String estado,
            Model model) {

        List<Cita> citasFiltradas;

        if (estado != null && !estado.isEmpty()) {
            String estadoBusqueda = "";
            if ("Pendientes".equalsIgnoreCase(estado)) {
                estadoBusqueda = "Pendiente";
            } else if ("Confirmadas".equalsIgnoreCase(estado)) {
                estadoBusqueda = "Confirmada";
            } else if ("Completadas".equalsIgnoreCase(estado)) {
                estadoBusqueda = "Completada";
            }
            citasFiltradas = citaService.findByEstadoAndEstadoPago(estadoBusqueda, "PAGADO");
        } else {
            citasFiltradas = citaService.findAllCitasActivasParaRecepcion();
        }

        model.addAttribute("citas", convertToDisplayDTO(citasFiltradas));
        model.addAttribute("estadoActual", estado);
        return "recepcion/gestion_citas";
    }

    @GetMapping("/citas/nueva")
    public String formNuevaCitaRecepcion(Model model) {
        model.addAttribute("citaDTO", new CitaDTO());
        model.addAttribute("clientes", usuarioService.findByRole(Role.ROLE_USER));
        model.addAttribute("clinicas", clinicaService.findAll());
        model.addAttribute("mascotas", new ArrayList<Mascota>());
        return "recepcion/form_solicitar_cita_recepcion";
    }

    @GetMapping("/api/mascotas-usuario/{userId}")
    @ResponseBody
    public List<Mascota> getMascotasUsuario(@PathVariable String userId) {
        return mascotaService.findByPropietarioId(userId);
    }

    // ✅ CORREGIDO: eliminadas las líneas con addPaymentIntentToCita y
    // markCitaAsPaid
    @PostMapping("/citas/guardar")
    public String guardarCita(@ModelAttribute CitaDTO citaDTO,
            RedirectAttributes redirectAttributes) {
        try {
            // 1. Guardar la cita (se guarda como "Pendiente de Pago" por defecto en el servicio)
            Cita citaGuardada = citaService.save(citaDTO);

            // 2. Crear notificación para el usuario con link al checkout
            String tituloNotif = "💳 Nueva Cita Pendiente de Pago";
            String msjNotif = "Se ha agendado una cita para tu mascota. Por favor, realiza el pago para confirmar.";
            String linkNotif = "/usuario/checkout/" + citaGuardada.getId();
            
            notificacionService.crearNotificacion(citaGuardada.getUsuarioId(), tituloNotif, msjNotif, linkNotif);

            redirectAttributes.addFlashAttribute("mensajeExito", "Cita solicitada exitosamente. Se ha enviado una notificación al cliente para que realice el pago.");
            return "redirect:/recepcion/citas";

        } catch (Exception e) {
            log.error("Error al guardar cita desde recepción: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al agendar la cita: " + e.getMessage());
            return "redirect:/recepcion/citas/nueva";
        }
    }

    @PostMapping("/citas/{id}/confirmar")
    public String confirmarCita(@PathVariable String id,
            @RequestParam("email") String email,
            @RequestParam("telefono") String telefono,
            RedirectAttributes redirectAttributes) {
        try {
            Cita cita = citaService.findById(id).orElseThrow();
            usuarioService.actualizarDatosContacto(cita.getUsuarioId(), email, telefono);
            citaService.registrarLlegada(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Paciente enviado a sala de espera.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al procesar: " + e.getMessage());
        }
        return "redirect:/recepcion/dashboard";
    }

    @PostMapping("/citas/{id}/cancelar")
    public String cancelarCita(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            citaService.cancelarCita(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cita cancelada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al cancelar la cita.");
        }
        return "redirect:/recepcion/dashboard";
    }

    @PostMapping("/citas/{id}/llegada")
    public String registrarLlegada(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            citaService.registrarLlegada(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Check-in completado! El paciente ha sido enviado a la Sala de Espera.");
        } catch (Exception e) {
            log.error("Error al registrar llegada de cita {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo registrar la llegada.");
        }
        return "redirect:/recepcion/dashboard";
    }

    // ==========================================================
    // GESTIÓN DE CLIENTES
    // ==========================================================
    @GetMapping("/clientes")
    public String listarClientes(Model model) {
        model.addAttribute("clientes", usuarioService.findByRole(Role.ROLE_USER));
        return "recepcion/gestion_clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String formNuevoCliente(Model model) {
        model.addAttribute("usuarioRegistroDTO", new UsuarioRegistroDTO());
        return "recepcion/form_registro_cliente";
    }

    // ✅ CORREGIDO: con validación de email duplicado y manejo de errores
    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute UsuarioRegistroDTO registroDTO,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            if (usuarioService.findByEmail(registroDTO.getEmail()) != null) {
                model.addAttribute("mensajeError",
                        "Ya existe un usuario registrado con el correo: " + registroDTO.getEmail());
                model.addAttribute("usuarioRegistroDTO", registroDTO);
                return "recepcion/form_registro_cliente";
            }

            usuarioService.createUsuarioWithRole(registroDTO, Role.ROLE_USER);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Cliente " + registroDTO.getNombre() + " registrado exitosamente.");
            return "redirect:/recepcion/clientes";

        } catch (Exception e) {
            log.error("Error al guardar cliente: {}", e.getMessage());
            model.addAttribute("mensajeError", "Error al registrar: " + e.getMessage());
            model.addAttribute("usuarioRegistroDTO", registroDTO);
            return "recepcion/form_registro_cliente";
        }
    }

    @GetMapping("/clientes/{clienteId}")
    public String verPerfilCliente(@PathVariable String clienteId,
            Model model,
            RedirectAttributes attributes) {
        Optional<Usuario> clienteOpt = usuarioService.findById(clienteId);

        if (clienteOpt.isEmpty()) {
            attributes.addFlashAttribute("error", "El cliente solicitado no fue encontrado.");
            return "redirect:/recepcion/clientes";
        }

        List<Mascota> mascotas = mascotaService.findByPropietarioId(clienteId);
        model.addAttribute("cliente", clienteOpt.get());
        model.addAttribute("mascotas", mascotas);
        return "recepcion/perfil_cliente";
    }

    // ==========================================================
    // REGISTRO DE VISITAS
    // ==========================================================
    @GetMapping("/visitas/nueva")
    public String formNuevaVisita(
            @RequestParam(required = false) String citaId,
            Model model) {

        VisitaDTO visitaDTO = new VisitaDTO();
        visitaDTO.setFechaVisita(LocalDateTime.now());

        if (citaId != null && !citaId.isBlank()) {
            Optional<Cita> citaOpt = citaService.findById(citaId);
            if (citaOpt.isPresent()) {
                Cita cita = citaOpt.get();
                visitaDTO.setCitaId(cita.getId());
                visitaDTO.setMascotaId(cita.getMascotaId());
                visitaDTO.setClinicaId(cita.getClinicaId());
                CitaDisplayDTO displayDTO = convertToDisplayDTO(List.of(cita)).get(0);
                model.addAttribute("citaPreseleccionada", displayDTO);
            }
        }

        model.addAttribute("visitaDTO", visitaDTO);

        if (citaId == null || citaId.isBlank()) {
            model.addAttribute("clientes", usuarioService.findByRole(Role.ROLE_USER));
            model.addAttribute("clinicas", clinicaService.findAll());
        }

        return "recepcion/form_registrar_visita_recepcion";
    }

    @PostMapping("/visitas/guardar")
    public String guardarVisita(@ModelAttribute("visitaDTO") VisitaDTO visitaDTO,
            RedirectAttributes redirectAttributes) {
        try {
            visitaService.save(visitaDTO);
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Visita registrada exitosamente!");
            return "redirect:/recepcion/citas";
        } catch (Exception e) {
            log.error("Error al guardar la visita: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Error al registrar la visita.");
            return "redirect:/recepcion/citas";
        }
    }

    // ==========================================================
    // MÉTODO AUXILIAR
    // ==========================================================
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
            
            CitaDisplayDTO dto = new CitaDisplayDTO(cita, u, c, m, servicios);
            
            // WEKA: Calcular probabilidad de ausencia (solo si está Pendiente o Confirmada)
            if ("Pendiente".equalsIgnoreCase(cita.getEstado()) || "Confirmada".equalsIgnoreCase(cita.getEstado())) {
                Double prob = wekaNoShowService.predecirProbabilidadAusencia(cita);
                dto.setProbabilidadAusencia(prob);
            } else {
                dto.setProbabilidadAusencia(0.0);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
}