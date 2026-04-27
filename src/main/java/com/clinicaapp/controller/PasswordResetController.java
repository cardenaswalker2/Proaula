package com.clinicaapp.controller;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.service.IEmailService;
import com.clinicaapp.service.IUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class PasswordResetController {

    @Autowired private IUsuarioService usuarioService;
    @Autowired private IEmailService emailService;

    // Muestra el formulario para introducir el email
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    // Procesa el email y envía el correo de reseteo
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String userEmail, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Usuario user = usuarioService.findByEmail(userEmail);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "No se encontró ninguna cuenta con ese correo electrónico.");
            return "redirect:/forgot-password";
        }

        String token = UUID.randomUUID().toString();
        usuarioService.createPasswordResetTokenForUser(user, token);

        // Construir la URL del enlace de reseteo
        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String resetUrl = appUrl + "/reset-password?token=" + token;

        // Enviar el correo
        String subject = "Restablecimiento de Contraseña - ClínicaApp";
        String body = "<h1>Solicitud de Restablecimiento de Contraseña</h1>" +
                      "<p>Hola " + user.getNombre() + ",</p>" +
                      "<p>Para restablecer tu contraseña, haz clic en el siguiente enlace:</p>" +
                      "<a href=\"" + resetUrl + "\">Restablecer mi Contraseña</a>" +
                      "<p>Si no solicitaste esto, puedes ignorar este correo.</p>";
        
        emailService.sendSimpleMessage(user.getEmail(), subject, body);

        redirectAttributes.addFlashAttribute("success", "Se ha enviado un enlace de restablecimiento a tu correo.");
        return "redirect:/forgot-password";
    }

    // Muestra el formulario para la nueva contraseña si el token es válido
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        Usuario user = usuarioService.validatePasswordResetToken(token);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "El enlace de restablecimiento es inválido o ha expirado.");
            return "redirect:/login";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // Procesa el cambio de contraseña
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        
        Usuario user = usuarioService.validatePasswordResetToken(token);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "El enlace de restablecimiento es inválido o ha expirado.");
            return "redirect:/login";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/reset-password?token=" + token;
        }

        usuarioService.changeUserPassword(user, newPassword);

        redirectAttributes.addFlashAttribute("success", "¡Tu contraseña ha sido cambiada exitosamente! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}