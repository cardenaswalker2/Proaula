package com.clinicaapp.controller;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.service.INotificacionService;
import com.clinicaapp.service.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private INotificacionService notificacionService;

    @Autowired
    private IUsuarioService usuarioService;

    @ModelAttribute
    public void addNotificationsToModel(Model model, Principal principal) {
        // Inicializamos valores por defecto para evitar errores en Thymeleaf
        model.addAttribute("notificacionesUnreadCount", 0L);
        model.addAttribute("listaNotificaciones", new java.util.ArrayList<>());

        if (principal != null) {
            Usuario usuario = getLoggedUser(principal);
            if (usuario != null) {
                model.addAttribute("notificacionesUnreadCount", notificacionService.getConteoNoLeidas(usuario.getId()));
                model.addAttribute("listaNotificaciones", notificacionService.getUltimasNotificaciones(usuario.getId(), 5));
            }
        }
    }

    private Usuario getLoggedUser(Principal principal) {
        if (principal == null) return null;

        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            Object p = auth.getPrincipal();

            if (p instanceof OAuth2User) {
                OAuth2User oauthUser = (OAuth2User) p;
                Object emailObj = oauthUser.getAttributes().get("email");
                if (emailObj != null) {
                    return usuarioService.findByEmail(emailObj.toString());
                }
            }

            if (p instanceof UserDetails) {
                String username = ((UserDetails) p).getUsername();
                return usuarioService.findByEmail(username);
            }
        }

        try {
            return usuarioService.findByEmail(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
