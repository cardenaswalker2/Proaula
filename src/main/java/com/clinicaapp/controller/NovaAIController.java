package com.clinicaapp.controller;

import com.clinicaapp.model.*;
import com.clinicaapp.dto.CitaDTO;
import com.clinicaapp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/nova-brain")
@CrossOrigin(origins = "*")
public class NovaAIController {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private IMascotaService mascotaService;
    @Autowired
    private ICitaService citaService;
    @Autowired
    private IClinicaService clinicaService;
    @Autowired
    private IServicioService servicioService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, Pattern> MOTOR_LOGICO = new LinkedHashMap<>();

    static {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

        // PRIORIDAD 1: MASCOTAS
        MOTOR_LOGICO.put("GOTO_MASCOTAS", Pattern
                .compile(".*(ver|mis|lista|mostrar|abre|tengo)\\s+(mascotas|perros|gatos|animales|peludos).*", flags));

        // PRIORIDAD 2: CITAS (Acciones)
        MOTOR_LOGICO.put("GOTO_SOLICITAR_CITA", Pattern
                .compile(".*(agendar|solicitar|pedir|ponme|nueva|reservar|sacar)\\s+(cita|turno|medico|vet).*", flags));
        MOTOR_LOGICO.put("GOTO_CANCELAR_CITA",
                Pattern.compile(".*(cancelar|anular|borrar|eliminar|quitar)\\s+(cita|turno).*", flags));
        MOTOR_LOGICO.put("GOTO_CITAS_LISTA",
                Pattern.compile(".*(ver|mis|proximas|cuando tengo|agenda|historial)\\s+(citas|turnos).*", flags));

        // PRIORIDAD 3: SALUD Y EMERGENCIAS
        MOTOR_LOGICO.put("SALUD_CONSEJO", Pattern.compile(
                ".*(vomito|diarrea|fiebre|no come|triste|decaido|herida|sangre|vacuna|enfermo|duele|dolor).*", flags));
        MOTOR_LOGICO.put("EMERGENCIA",
                Pattern.compile(".*(urgente|emergencia|me muero|se muere|auxilio|ayuda urgente|accidente).*", flags));
        MOTOR_LOGICO.put("HISTORIAL_MEDICO",
                Pattern.compile(".*(historial|vacunas|recetas|medicina|examenes|resultados|diagnostico).*", flags));

        // PRIORIDAD 4: USUARIO Y FINANZAS
        MOTOR_LOGICO.put("GOTO_PERFIL", Pattern.compile(
                ".*(ver|ir|abre|mostrar|ajustes|configurar)\\s+(mi perfil|mis datos|quien soy|mi cuenta).*", flags));
        MOTOR_LOGICO.put("GOTO_PAGOS", Pattern
                .compile(".*(pagos|facturas|recibos|debo|dinero|cuanto|gastado|transacciones|pagado|deuda).*", flags));

        // PRIORIDAD 5: INFORMACIÓN DE CLINICA
        MOTOR_LOGICO.put("FAQ_HORARIOS",
                Pattern.compile(".*(horarios|hora|abren|cierran|abierto|atencion|cuando abren).*", flags));
        MOTOR_LOGICO.put("FAQ_UBICACION",
                Pattern.compile(".*(donde estan|ubicacion|direccion|llegar|mapa|donde quedan).*", flags));
        MOTOR_LOGICO.put("FAQ_PRECIOS",
                Pattern.compile(".*(precio|costo|cuanto vale|cuanto cuesta|tarifas|cobran).*", flags));
        MOTOR_LOGICO.put("CONTACTO_SOPORTE",
                Pattern.compile(".*(telefono|llamar|contacto|whatsapp|ayuda|soporte|problema|comunicarme).*", flags));

        // PRIORIDAD 6: JUEGOS Y EXTRAS
        MOTOR_LOGICO.put("GOTO_JUEGO",
                Pattern.compile(".*(juego|jugar|diversion|mascota virtual|entrar al juego|aburrido).*", flags));

        // PRIORIDAD 7: CONVERSACIONAL
        MOTOR_LOGICO.put("SALUDO",
                Pattern.compile(".*(hola|hey|buenos dias|buenas tardes|buenas noches|que tal|nova|oe|alo).*", flags));
        MOTOR_LOGICO.put("GRACIAS",
                Pattern.compile(".*(gracias|buena|genial|perfecto|ok|entendido|chevere|excelente).*", flags));
        MOTOR_LOGICO.put("DESPEDIDA", Pattern.compile(".*(adios|chao|hasta luego|nos vemos|bye|me voy).*", flags));
    }

    @PostMapping("/think")
    public ResponseEntity<NovaResponse> pensar(@RequestBody NovaRequest request) {
        String msg = (request.getMensaje() != null) ? request.getMensaje().toLowerCase().trim() : "";
        String userName = (request.getContexto() != null && request.getContexto().containsKey("user"))
                ? request.getContexto().get("user")
                : "Usuario";
        // Primero intentar obtener userId del contexto del frontend
        String userId = (request.getContexto() != null && request.getContexto().containsKey("userId"))
                ? request.getContexto().get("userId")
                : null;

        // Si el frontend no lo envió (null o vacío), buscarlo desde Spring Security
        if (userId == null || userId.isEmpty()) {
            try {
                String email = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication().getName();
                if (email != null && !email.equals("anonymousUser")) {
                    Usuario uSec = usuarioService.findByEmail(email);
                    if (uSec != null) {
                        userId = uSec.getId();
                        if (userName.equals("Usuario")) userName = uSec.getNombre();
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Nova] No se pudo resolver usuario desde Security: " + ex.getMessage());
            }
        }

        // --- ENRIQUECIMIENTO DE CONTEXTO ---
        StringBuilder contextBuilder = new StringBuilder();
        if (userId != null) {
            usuarioService.findById(userId).ifPresent(u -> {
                contextBuilder.append("Usuario Info: Nombre: ").append(u.getNombre()).append(" ")
                        .append(u.getApellido()).append("\n");

                List<Mascota> mascotas = mascotaService.findByPropietarioId(u.getId());
                if (!mascotas.isEmpty()) {
                    contextBuilder.append("Mascotas del usuario:\n");
                    for (Mascota m : mascotas) {
                        contextBuilder.append("- ").append(m.getNombre()).append(" (").append(m.getEspecie())
                                .append(", ").append(m.getRaza()).append(")\n");
                    }
                } else {
                    contextBuilder.append("El usuario no tiene mascotas registradas.\n");
                }

                List<Cita> citas = citaService.findByUsuarioId(u.getId());
                if (!citas.isEmpty()) {
                    contextBuilder.append("Citas próximas del usuario:\n");
                    for (Cita c : citas) {
                        if (!"CANCELADA".equalsIgnoreCase(c.getEstado())
                                && !"COMPLETADA".equalsIgnoreCase(c.getEstado())) {
                            contextBuilder.append("- Fecha: ").append(c.getFechaHora()).append(", Estado: ")
                                    .append(c.getEstado()).append(", Mascota ID: ").append(c.getMascotaId())
                                    .append("\n");
                        }
                    }
                } else {
                    contextBuilder.append("El usuario no tiene citas próximas.\n");
                }
            });
        }

        // --- INTEGRACIÓN GROQ API ---
        if (groqApiKey != null && !groqApiKey.isEmpty() && !groqApiKey.startsWith("gsk_TU_API_KEY")) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

                String systemPrompt = "Eres Nova, una inteligente, amigable y muy servicial asistente virtual de la Clínica Veterinaria 'Clínica App'. "
                        + "Respondes de manera concisa y humana (máximo 3 oraciones por lo general). Siempre incluyes emojis.\n"
                        + "## INSTRUCCIÓN CRUCIAL: Responde siempre de forma amable. "
                        + "Tienes acceso COMPLETO al sistema mediante tus herramientas (puedes ver tu perfil, mascotas e historia médica del usuario)."
                        + "\nCuando el usuario te pregunte sobre clínicas o servicios, invoca 'get_clinicas' (que ya incluye todo) y muéstraselo en formato de lista. Al final, siempre pregunta '¿Necesitas ayuda sobre algo más?'.\n"
                        + "Fecha actual: " + fechaActual + "\n\n"
                        + "## CONTEXTO DEL USUARIO ACTUAL\n"
                        + (contextBuilder.length() > 0 ? contextBuilder.toString()
                                : "No se ha proporcionado contexto del usuario o el usuario es anónimo.")
                        + "\n\n"
                        + "## TAREAS Y CAPACIDADES\n"
                        + "Si el usuario pregunta por información de la clínica: Horario: L-V 8AM-8PM, S-D 9AM-2PM. Precios de consulta general desde $65.000 COP.\n"
                        + "Importante: Si es una emergencia, URGE que llamen al (555) 123-4567.\n"
                        + "Menciona los nombres de las mascotas si el usuario habla de ellas.";
                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));
                messages.add(Map.of("role", "user", "content", msg));

                Map<String, Object> finalMessage = executeGroqAgentLoop(messages, userId, 0);

                if (finalMessage != null) {
                    String aiContent = (String) finalMessage.get("content");
                    if (aiContent == null || aiContent.isBlank()) {
                        aiContent = "¡Claro! 🐶 He revisado la información solicitada. ¿Te puedo ayudar en algo más?";
                    }

                    NovaResponse r = new NovaResponse();
                    r.setIntent("GROQ_AI");
                    r.setRespuesta(aiContent);

                    if (finalMessage.containsKey("actionModal")) {
                        r.setActionModal((String) finalMessage.get("actionModal"));
                        r.setModalData(finalMessage.get("modalData"));
                    }
                    if (finalMessage.containsKey("redirectUrl")) {
                        r.setRedirectUrl((String) finalMessage.get("redirectUrl"));
                    }

                    // Combinar con lógica local para añadir sugerencias o URLs de redirección
                    // (navegación visual)
                    for (Map.Entry<String, Pattern> entry : MOTOR_LOGICO.entrySet()) {
                        if (entry.getValue().matcher(msg).matches()) {
                            NovaResponse rFallback = generarRespuesta(entry.getKey(), userName);
                            r.setData(rFallback.getData());
                            r.setSugerencias(rFallback.getSugerencias());
                            break;
                        }
                    }
                    if (r.getSugerencias() == null)
                        r.setSugerencias(Arrays.asList("📅 Mis Citas", "🐾 Mascotas", "📞 Ayuda"));
                    return ResponseEntity.ok(r);
                }
            } catch (Exception e) {
                System.err.println("Error llamando a Groq API: " + e.getMessage());
            }
        }

        String intent = "FALLBACK";
        for (Map.Entry<String, Pattern> entry : MOTOR_LOGICO.entrySet()) {
            if (entry.getValue().matcher(msg).matches()) {
                intent = entry.getKey();
                break;
            }
        }

        return ResponseEntity.ok(generarRespuesta(intent, userName));
    }

    private Map<String, Object> executeGroqAgentLoop(List<Map<String, Object>> messages, String userId, int depth) {
        if (depth > 3)
            return Map.of("role", "assistant", "content",
                    "Lo siento, tuve un pequeño problema procesando la solicitud. ¿Podrías repetirlo?");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("messages", messages);
        body.put("temperature", 0.4);

        // Solo en el primer llamado ponemos las tools disponibles
        if (depth == 0) {
            List<Map<String, Object>> tools = new ArrayList<>();
            tools.add(createTool("get_clinicas",
                    "Retorna la lista de todas las clínicas veterinarias y TODOS los servicios ofrecidos con sus precios. Úsalo cuando el usuario pregunte por clínicas, servicios o qué ofrece la aplicación.",
                    Map.of("type", "object", "properties", new HashMap<>())));
            tools.add(createTool("get_mi_perfil",
                    "Retorna la información personal del usuario: nombre, correo y estado de la cuenta. Úsalo cuando diga 've mi perfil', 'mis datos', 'quién soy'.",
                    Map.of("type", "object", "properties", new HashMap<>())));
            tools.add(createTool("get_mis_mascotas",
                    "Retorna la lista de mascotas del usuario con nombre, especie, raza y edad. Úsalo cuando diga 'mis mascotas', 'historial de mi mascota', 'mis animales'.",
                    Map.of("type", "object", "properties", new HashMap<>())));
            tools.add(createTool("agendar_cita",
                    "Agenda una cita veterinaria. Necesitas: clinicaId, mascotaId, servicioId, fechaHora (YYYY-MM-DDTHH:MM), motivo.",
                    Map.of("type", "object",
                            "properties", Map.of(
                                    "clinicaId", Map.of("type", "string"),
                                    "mascotaId", Map.of("type", "string"),
                                    "fechaHora", Map.of("type", "string", "description", "Formato: YYYY-MM-DDTHH:MM"),
                                    "servicioId", Map.of("type", "string"),
                                    "motivo", Map.of("type", "string")),
                            "required", Arrays.asList("clinicaId", "mascotaId", "fechaHora", "servicioId", "motivo"))));
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            Map<String, Object> response = restTemplate.postForObject(groqApiUrl, requestEntity, Map.class);

            if (response == null || !response.containsKey("choices")) {
                System.err.println("[Nova] Groq no retornó choices. Response: " + response);
                return Map.of("role", "assistant", "content", "Tuve un problema de conexión con mi núcleo 🤔. ¿Podrías repetirlo?");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices.isEmpty()) {
                return Map.of("role", "assistant", "content", "No obtuve respuesta de mi núcleo. Intenta de nuevo 🙏");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String finishReason = (String) firstChoice.get("finish_reason");

            System.out.println("[Nova] depth=" + depth + " finish_reason=" + finishReason);

            // Groq quiere llamar una herramienta
            if ("tool_calls".equals(finishReason) || message.containsKey("tool_calls")) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");

                // Ejecutar CADA herramienta localmente y acumular resultados
                StringBuilder toolResultsSummary = new StringBuilder();
                for (Map<String, Object> toolCall : toolCalls) {
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    String name = (String) function.get("name");
                    String argsJson = (String) function.get("arguments");
                    System.out.println("[Nova] Ejecutando herramienta: " + name + " args=" + argsJson);

                    String resultado = ejecutarHerramientaLocal(name, argsJson, userId);
                    System.out.println("[Nova] Resultado herramienta " + name + ": " + resultado.substring(0, Math.min(resultado.length(), 200)));
                    toolResultsSummary.append("=== ").append(name).append(" ===\n").append(resultado).append("\n\n");
                }

                // Armar un NUEVO llamado limpio a Groq para sintetizar la respuesta en lenguaje natural
                // (sin tools, para que no entre en loop)
                String systemSynthesis = "Eres Nova, asistente veterinario amigable. Usa emojis. Responde en español basándote en los datos recibidos. Sé conciso pero completo.";
                String userContext = messages.stream()
                        .filter(m -> "user".equals(m.get("role")))
                        .map(m -> (String) m.get("content"))
                        .findFirst().orElse("el usuario hizo una pregunta");

                List<Map<String, Object>> synthesisMessages = new ArrayList<>();
                synthesisMessages.add(Map.of("role", "system", "content", systemSynthesis));
                synthesisMessages.add(Map.of("role", "user", "content", userContext));
                synthesisMessages.add(Map.of("role", "system", "content",
                        "DATOS DEL SISTEMA (usa esto para responder al usuario de forma amable y clara):\n" + toolResultsSummary.toString()));
                synthesisMessages.add(Map.of("role", "user", "content",
                        "Con base en los datos anteriores, respóndeme de forma clara y amigable."));

                // Llamado limpio a Groq para síntesis — SIN tools
                return executeGroqAgentLoop(synthesisMessages, userId, depth + 1);
            }

            // Respuesta de texto final – retornarla directa
            return message;

        } catch (Exception e) {
            System.err.println("[Nova] ERROR en Groq depth=" + depth + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return Map.of("role", "assistant", "content", "Tuve un error interno (" + e.getClass().getSimpleName() + "). Intenta de nuevo 🙏");
        }
    }

    private Map<String, Object> createTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        Map<String, Object> func = new HashMap<>();
        func.put("name", name);
        func.put("description", description);
        func.put("parameters", parameters);
        tool.put("function", func);
        return tool;
    }

    private String ejecutarHerramientaLocal(String name, String argsStr, String currentUserId) {
        try {
            Map<String, Object> args = argsStr != null && !argsStr.trim().isEmpty()
                    ? mapper.readValue(argsStr, Map.class)
                    : new HashMap<>();

            if ("get_clinicas".equals(name)) {
                StringBuilder cliList = new StringBuilder();
                List<Clinica> clinicas = clinicaService.findAll();
                List<Servicio> serviciosGlob = servicioService.findAll();
                if (clinicas.isEmpty())
                    return "No hay clínicas activas.";

                cliList.append("== CLÍNICAS DISPONIBLES ==\n");
                for (Clinica c : clinicas) {
                    cliList.append("- Clínica: '").append(c.getNombre()).append("' (ID: ").append(c.getId())
                            .append("). Ubicación: ").append(c.getDireccion()).append("\n");
                }
                cliList.append("\n== SERVICIOS GENERALES OFRECIDOS EN ESTAS CLÍNICAS ==\n");
                for (Servicio s : serviciosGlob) {
                    cliList.append("- Servicio: '").append(s.getNombre()).append("' (ID: ").append(s.getId())
                            .append("). Precio: $").append(s.getCosto()).append("\n");
                }
                return cliList.toString();

            } else if ("get_mi_perfil".equals(name)) {
                if (currentUserId == null || currentUserId.isEmpty())
                    return "El usuario es anónimo o no ha iniciado sesión. No puedes ver su perfil.";
                Optional<Usuario> oUser = usuarioService.findById(currentUserId);
                if (oUser.isEmpty())
                    return "Usuario no encontrado en la base de datos.";
                Usuario u = oUser.get();
                return "DATOS DEL PERFIL DEL USUARIO:\nNombre Completo: " + u.getNombre() + " " + u.getApellido()
                        + "\nEmail: " + u.getEmail() + "\nEstado Cuenta: " + (u.isActivo() ? "Activa" : "Inactiva");

            } else if ("get_mis_mascotas".equals(name)) {
                if (currentUserId == null || currentUserId.isEmpty())
                    return "El usuario es anónimo. No tiene mascotas asociadas.";
                List<Mascota> mascotas = mascotaService.findByPropietarioId(currentUserId);
                if (mascotas.isEmpty())
                    return "El usuario no tiene ninguna mascota registrada actualmente en el sistema.";

                StringBuilder mList = new StringBuilder("MASCOTAS REGISTRADAS DEL USUARIO:\n");
                for (Mascota m : mascotas) {
                    mList.append("- [ID: ").append(m.getId()).append("] ").append(m.getNombre())
                            .append(". Especie: ").append(m.getEspecie())
                            .append(", Raza: ").append(m.getRaza())
                            .append(", Sexo: ").append(m.getSexo())
                            .append(", Edad: ").append(m.getEdad()).append(" años")
                            .append(", Fecha de Nacimiento: ").append(m.getFechaNacimiento())
                            .append("\n");
                }
                return mList.toString();

            } else if ("agendar_cita".equals(name)) {
                if (currentUserId == null)
                    return "Error: Usuario no identificado.";

                CitaDTO c = new CitaDTO();
                c.setUsuarioId(currentUserId);
                c.setClinicaId((String) args.get("clinicaId"));
                c.setMascotaId((String) args.get("mascotaId"));

                String fhStr = (String) args.get("fechaHora");
                try {
                    c.setFechaHora(LocalDateTime.parse(fhStr));
                } catch (Exception e) {
                    return "Error de Formato de Fecha, debe ser YYYY-MM-DDTHH:MM";
                }

                c.setServiciosIds(Arrays.asList((String) args.get("servicioId")));
                c.setMotivo((String) args.get("motivo"));
                c.setEstado("Pendiente");

                Cita citaGuardada = citaService.save(c);
                return "Cita agendada exitosamente. Estado marcado como Pendiente. Costo base asginado: $"
                        + citaGuardada.getCosto();
            }
        } catch (Exception e) {
            System.err.println("Error procesando tool " + name + ": " + e.getMessage());
            return "Error interno ejecutando la función: " + e.getMessage();
        }
        return "Función no reconocida.";
    }

    private NovaResponse generarRespuesta(String intent, String user) {
        NovaResponse r = new NovaResponse();
        r.setIntent(intent);
        Map<String, String> data = new HashMap<>();
        List<String> sugerencias = new ArrayList<>();
        String out = "";

        switch (intent) {
            case "GOTO_MASCOTAS":
                out = "¡Claro! 🐾 Aquí tienes la lista de tus mascotas. Puedes agregar nuevas o ver su historial.";
                data.put("redirectUrl", "/usuario/mis-mascotas");
                sugerencias = Arrays.asList("📅 Agendar Cita", "🩺 Historial Médico", "🎮 Jugar");
                break;

            case "GOTO_SOLICITAR_CITA":
                out = "¡Entendido! 📅 Te llevo a la sección para agendar una nueva cita con nuestros especialistas.";
                data.put("redirectUrl", "/usuario/citas/solicitar");
                break;

            case "GOTO_CANCELAR_CITA":
                out = "Para cancelar una cita, por favor revisa tus próximas citas y selecciona la opción de cancelar. 📅";
                data.put("redirectUrl", "/usuario/mis-citas");
                sugerencias = Arrays.asList("📅 Mis Citas", "📞 Soporte");
                break;

            case "GOTO_CITAS_LISTA":
                out = "📅 Revisando tus citas... Aquí puedes ver tus turnos programados y pasados.";
                data.put("redirectUrl", "/usuario/mis-citas");
                sugerencias = Arrays.asList("➕ Nueva Cita", "❌ Cancelar Cita");
                break;

            case "GOTO_PERFIL":
                out = "👤 Abriendo tu perfil, **" + user + "**. Aquí puedes actualizar tus datos.";
                data.put("redirectUrl", "/usuario/perfil");
                break;

            case "GOTO_PAGOS":
                out = "💳 Te redirijo al panel de finanzas. Podrás ver tus pagos, facturas y deudas pendientes.";
                data.put("redirectUrl", "/usuario/pagos");
                sugerencias = Arrays.asList("📅 Mis Citas", "📞 Contactar Soporte");
                break;

            case "GOTO_JUEGO":
                out = "🕹️ ¡Hora de divertirse! Entrando al juego de la mascota virtual. ¡Cuídala mucho!";
                data.put("redirectUrl", "/usuario/juego");
                break;

            case "SALUD_CONSEJO":
                out = "🩺 Esos síntomas requieren atención profesional. Te sugiero agendar una cita o visitar el historial de tu mascota. ¿Te ayudo con eso?";
                sugerencias = Arrays.asList("📅 Agendar ahora", "🚑 Urgencias", "📞 Llamar");
                break;

            case "EMERGENCIA":
                out = "🚨 **¡MANTÉN LA CALMA!** Por favor comunícate de inmediato con nuestra línea de emergencias: **(555) 123-4567** o acude a la clínica lo antes posible.";
                sugerencias = Arrays.asList("📞 Llamar ahora", "📍 Ver ubicación");
                break;

            case "HISTORIAL_MEDICO":
                out = "📁 Aquí puedes consultar el historial médico, vacunas y recetas de tus mascotas.";
                data.put("redirectUrl", "/usuario/mis-mascotas");
                sugerencias = Arrays.asList("📅 Agendar Cita", "🩺 Síntomas");
                break;

            case "FAQ_HORARIOS":
                out = "🕒 **Nuestros horarios:** Lunes a Viernes de 8:00 AM a 8:00 PM. Sábados y Domingos de 9:00 AM a 2:00 PM. ¡Urgencias 24/7!";
                sugerencias = Arrays.asList("📅 Agendar Cita", "📍 Ubicación");
                break;

            case "FAQ_UBICACION":
                out = "📍 **Ubicación:** Estamos en Av. Principal #123, Centro de la Ciudad. ¡Te esperamos!";
                sugerencias = Arrays.asList("🗺️ Abrir en Maps", "🕒 Horarios");
                break;

            case "FAQ_PRECIOS":
                out = "💲 Nuestros precios de consulta general inician en **$65.000 COP**. Las vacunas básicas desde **$45.000**. Contamos con especialistas desde **$150.000** y servicios de urgencias 24h. ¿Te gustaría agendar una valoración?";
                sugerencias = Arrays.asList("📅 Agendar Cita", "🩺 Ver Especialidades", "📞 Llamar");
                break;

            case "CONTACTO_SOPORTE":
                out = "📞 Puedes contactarnos llamando al **(555) 987-6543** o enviándonos un mensaje por WhatsApp. Nuestro equipo te ayudará con gusto.";
                sugerencias = Arrays.asList("💬 WhatsApp", "📧 Correo");
                break;

            case "SALUDO":
                out = "¡Hola **" + user
                        + "**! ✨ Soy Nova 🤖. Puedo ayudarte con citas, información, pagos y tus mascotas. ¿En qué te ayudo hoy?";
                sugerencias = Arrays.asList("📅 Agendar Cita", "🕒 Horarios", "📍 Ubicación", "🐾 Mis Mascotas");
                break;

            case "GRACIAS":
                out = "¡De nada! 🥰 Si necesitas algo más, solo dime. ¡Siempre lista para ayudar a tus peluditos y a ti!";
                sugerencias = Arrays.asList("📅 Citas", "🐾 Mis Mascotas");
                break;

            case "DESPEDIDA":
                out = "¡Hasta pronto, **" + user + "**! 👋 ¡Cuídate mucho y dale un abrazo a tus mascotas de mi parte!";
                break;

            default:
                out = "Hmm, no estoy completamente segura de lo que necesitas 🤔... Pero soy buena para encontrar tus **citas**, **perfil** o información de la **clínica**.";
                sugerencias = Arrays.asList("📅 Citas", "🕒 Horarios", "📍 Ubicación", "❓ Ayuda");
        }

        r.setRespuesta(out);
        r.setData(data);
        r.setSugerencias(sugerencias);
        return r;
    }

    public static class NovaRequest {
        private String mensaje;
        private Map<String, String> contexto;

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }

        public Map<String, String> getContexto() {
            return contexto;
        }

        public void setContexto(Map<String, String> contexto) {
            this.contexto = contexto;
        }
    }

    public static class NovaResponse {
        private String respuesta;
        private String intent;
        private Map<String, String> data;
        private List<String> sugerencias;
        private String actionModal;
        private Object modalData;
        private String redirectUrl;

        public String getActionModal() {
            return actionModal;
        }

        public void setActionModal(String actionModal) {
            this.actionModal = actionModal;
        }

        public Object getModalData() {
            return modalData;
        }

        public void setModalData(Object modalData) {
            this.modalData = modalData;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }

        public String getRespuesta() {
            return respuesta;
        }

        public void setRespuesta(String respuesta) {
            this.respuesta = respuesta;
        }

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public Map<String, String> getData() {
            return data;
        }

        public void setData(Map<String, String> data) {
            this.data = data;
        }

        public List<String> getSugerencias() {
            return sugerencias;
        }

        public void setSugerencias(List<String> sugerencias) {
            this.sugerencias = sugerencias;
        }
    }
}