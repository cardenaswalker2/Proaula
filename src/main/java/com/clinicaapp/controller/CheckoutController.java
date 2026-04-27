package com.clinicaapp.controller;

import com.clinicaapp.model.Cita;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.repository.CitaRepository;
import com.clinicaapp.service.*;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    @Value("${stripe.api.key.public}")
    private String stripePublicKey;

    @Autowired private ICitaService citaService;
    @Autowired private IStripeService stripeService;
    @Autowired private IServicioService servicioService;
    @Autowired private IUsuarioService usuarioService;
    @Autowired private IEmailService emailService;
    @Autowired private ISmsService smsService;
    @Autowired private CitaRepository citaRepository;

    @GetMapping("/usuario/checkout/{citaId}")
    public String checkout(@PathVariable String citaId, Model model) {
        log.info("--- PASO A: INICIANDO CHECKOUT PARA CITA ID: {} ---", citaId);

        Cita cita = citaService.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada con ID: " + citaId));

        if ("PAGADO".equals(cita.getEstadoPago())) {
            return "redirect:/usuario/mis-citas?yaPagada";
        }
        
        // Obtener todos los servicios para la descripción
        List<String> nombresServicios = new java.util.ArrayList<>();
        if (cita.getServiciosIds() != null) {
            for (String sId : cita.getServiciosIds()) {
                servicioService.findById(sId).ifPresent(s -> nombresServicios.add(s.getNombre()));
            }
        }
        String descripcion = "Cita para: " + (nombresServicios.isEmpty() ? "Servicios Veteronarios" : String.join(", ", nombresServicios));

        try {
            long amountInCents = (long) (cita.getCosto() * 100);

            PaymentIntent paymentIntent = stripeService.createPaymentIntent(amountInCents, "usd", descripcion);
            
            log.info("PaymentIntent creado en Stripe con ID: {}", paymentIntent.getId());

            citaService.addPaymentIntentToCita(citaId, paymentIntent.getId());
            
            log.info("ID de PaymentIntent guardado en la Cita en la BD. Redirigiendo a la página de pago.");

            model.addAttribute("clientSecret", paymentIntent.getClientSecret());
            model.addAttribute("stripePublicKey", stripePublicKey);
            model.addAttribute("monto", cita.getCosto());
            model.addAttribute("descripcionPago", descripcion);
            model.addAttribute("moneda", "USD");

            return "checkout";

        } catch (StripeException e) {
            log.error("Error de Stripe al crear PaymentIntent: {}", e.getMessage());
            return "redirect:/usuario/mis-citas?errorPago=" + e.getMessage();
        }
    }

    @GetMapping("/pago-exitoso")
    public String pagoExitoso(@RequestParam("payment_intent") String paymentIntentId, Model model) {
        log.info("--- INICIO PROCESO DE PAGO EXITOSO ---");
        log.info("Payment Intent ID recibido: {}", paymentIntentId);
        
        try {
            citaService.markCitaAsPaid(paymentIntentId);
            log.info("Cita marcada como PAGADA en la base de datos.");
        } catch (Exception e) {
            log.error("ERROR CRÍTICO: No se pudo marcar la cita como pagada. Causa: {}", e.getMessage(), e);
            model.addAttribute("error", "Tu pago fue exitoso, pero hubo un problema al confirmar tu cita. Por favor, contacta a soporte.");
            model.addAttribute("paymentIntentId", paymentIntentId);
            return "pago_exitoso";
        }

        Optional<Cita> citaOpt = citaRepository.findByPaymentIntentId(paymentIntentId);

        if (citaOpt.isPresent()) {
            Cita cita = citaOpt.get();
            log.info("Cita encontrada en la BD con ID: {}", cita.getId());

            Optional<Usuario> usuarioOpt = usuarioService.findById(cita.getUsuarioId());
            List<String> nombresServiciosNotificacion = new java.util.ArrayList<>();
            if (cita.getServiciosIds() != null) {
                for (String sId : cita.getServiciosIds()) {
                    servicioService.findById(sId).ifPresent(s -> nombresServiciosNotificacion.add(s.getNombre()));
                }
            }
            String nombreServicio = nombresServiciosNotificacion.isEmpty() ? "Servicio" : String.join(", ", nombresServiciosNotificacion);

            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                log.info("Usuario ({}) encontrado para la notificación de servicios: {}.", usuario.getEmail(), nombreServicio);

                // --- Enviar SMS ---
                try {
                    if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank()) {
                        String smsMessage = String.format("ClinicaApp: Su cita para '%s' ha sido agendada y pagada con éxito.", nombreServicio);
                        smsService.sendSms(usuario.getTelefono(), smsMessage);
                        log.info("INTENTO DE ENVÍO de SMS de confirmación a {}", usuario.getTelefono());
                    } else {
                        log.warn("No se envió SMS: el usuario {} no tiene un número de teléfono registrado.", usuario.getEmail());
                    }
                } catch (Exception e) {
                    log.error("Fallo al enviar SMS de confirmación a {}: {}", usuario.getTelefono(), e.getMessage());
                }

                // --- Enviar Correo Mejorado ---
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' h:mm a", new Locale("es", "ES"));
                    String fechaFormateada = cita.getFechaHora() != null ? cita.getFechaHora().format(formatter) : "Fecha por confirmar";
                    
                    String emailSubject = "✅ ¡Pago Confirmado! Tu cita en ClinicaApp";
                    
                    String emailBody = String.format("""
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { margin: 0; padding: 0; background-color: #f4f4f4; }
                                .container { max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
                                .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 30px; text-align: center; border-radius: 10px 10px 0 0; }
                                .header h1 { margin: 0; font-size: 28px; }
                                .content { background: #ffffff; padding: 40px 30px; border-radius: 0 0 10px 10px; }
                                .greeting { font-size: 18px; color: #333; margin-bottom: 20px; }
                                .info-box { background: #f8f9fa; padding: 25px; margin: 20px 0; border-left: 5px solid #667eea; border-radius: 8px; }
                                .info-box p { margin: 10px 0; font-size: 16px; color: #555; }
                                .info-box strong { color: #333; }
                                .status { display: inline-block; background: #fff3cd; color: #856404; padding: 8px 16px; border-radius: 20px; font-size: 14px; font-weight: bold; }
                                .message { margin: 25px 0; color: #666; line-height: 1.6; }
                                .btn { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 30px; font-weight: bold; margin-top: 20px; }
                                .footer { text-align: center; color: #999; margin-top: 30px; font-size: 13px; line-height: 1.6; }
                                .footer a { color: #667eea; text-decoration: none; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>💳 ¡Pago Recibido!</h1>
                                </div>
                                <div class="content">
                                    <p class="greeting">Hola <strong>%s</strong>,</p>
                                    <p>Tu pago ha sido procesado exitosamente. Aquí están los detalles de tu cita:</p>
                                    
                                    <div class="info-box">
                                        <p>🏥 <strong>Servicio:</strong> %s</p>
                                        <p>📅 <strong>Fecha y hora:</strong> %s</p>
                                        <p>💰 <strong>Monto pagado:</strong> $%.2f USD</p>
                                        <p>📍 <strong>Estado:</strong> <span class="status">⏳ Pendiente de confirmación</span></p>
                                    </div>
                                    
                                    <p class="message">Tu cita está siendo revisada por nuestro equipo. Te enviaremos otro correo en cuanto sea confirmada oficialmente.</p>
                                    
                                    <center>
                                        <a href="http://localhost:8080/usuario/mis-citas" class="btn">Ver mis citas</a>
                                    </center>
                                    
                                    <div class="footer">
                                        <p>¿Necesitas ayuda? Escríbenos a <a href="mailto:soporte@clinicaapp.com">soporte@clinicaapp.com</a></p>
                                        <p>© 2024 ClinicaApp - Todos los derechos reservados</p>
                                    </div>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                        usuario.getNombre(),
                        nombreServicio,
                        fechaFormateada,
                        cita.getCosto()
                    );
                    
                    emailService.sendSimpleMessage(usuario.getEmail(), emailSubject, emailBody);
                    log.info("Email de confirmación de pago enviado exitosamente a {}", usuario.getEmail());
                    
                } catch (Exception e) {
                    log.error("Fallo al enviar email de confirmación a {}: {}", usuario.getEmail(), e.getMessage());
                }

            } else {
                log.error("No se pudieron enviar notificaciones: No se encontró el Usuario o el Servicio asociado a la Cita ID {}", cita.getId());
            }
        } else {
            log.error("ERROR GRAVE: La cita con Payment Intent ID {} fue marcada como pagada pero no se pudo encontrar para enviar notificaciones.", paymentIntentId);
        }
        
        model.addAttribute("paymentIntentId", paymentIntentId);
        return "pago_exitoso";
    }

    @PostMapping("/stripe/webhook")
    @ResponseBody
    public String handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Webhook de Stripe recibido.");
        return "{\"status\":\"received\"}";
    }
}