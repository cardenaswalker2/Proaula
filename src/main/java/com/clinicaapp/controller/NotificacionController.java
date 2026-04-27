package com.clinicaapp.controller;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.service.INotificacionService;
import com.clinicaapp.service.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private INotificacionService notificacionService;

    @Autowired
    private IUsuarioService usuarioService;

    @PostMapping("/marcar-leida/{id}")
    public ResponseEntity<Void> marcarLeida(@PathVariable String id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(Principal principal) {
        Usuario usuario = getLoggedUser(principal);
        if (usuario != null) {
            notificacionService.marcarTodasComoLeidas(usuario.getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }

    private Usuario getLoggedUser(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            Object p = auth.getPrincipal();
            if (p instanceof OAuth2User) {
                Object emailObj = ((OAuth2User) p).getAttributes().get("email");
                if (emailObj != null) return usuarioService.findByEmail(emailObj.toString());
            }
            if (p instanceof UserDetails) {
                return usuarioService.findByEmail(((UserDetails) p).getUsername());
            }
        }
        return usuarioService.findByEmail(principal.getName());
    }
}
