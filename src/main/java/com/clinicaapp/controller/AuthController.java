package com.clinicaapp.controller;

import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.service.IUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class AuthController {

    @Autowired
    private IUsuarioService usuarioService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuarioDto", new UsuarioRegistroDTO());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("usuarioDto") UsuarioRegistroDTO registroDTO, Model model) {
        try {
            usuarioService.save(registroDTO);
            return "redirect:/login?registroExitoso";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar: " + e.getMessage());
            return "registro";
        }
    }

    // --- LOGIN FACIAL CON SEGURIDAD REFORZADA ---
    @PostMapping("/auth/login-facial")
@ResponseBody
public ResponseEntity<?> autenticarFacial(@RequestBody List<Double> descriptor, 
                                          HttpServletRequest request, 
                                          HttpServletResponse response) {
    
    // El servicio ahora usa el umbral de 0.38
    Usuario usuario = usuarioService.loginFacial(descriptor);

    if (usuario != null) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                usuario.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok().body(Map.of(
            "status", "success", 
            "redirect", "/usuario/dashboard" 
        ));
    } else {
        // Respuesta de error clara
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(Map.of("status", "error", "mensaje", "Identidad no verificada. Acceso denegado."));
    }
}

    @PostMapping("/auth/registrar-rostro")
    @ResponseBody
    public ResponseEntity<?> guardarRostro(@RequestBody Map<String, Object> payload) {
        try {
            String email = (String) payload.get("email");
            List<Double> descriptor = (List<Double>) payload.get("descriptor");
            usuarioService.registrarRostro(usuarioService.findByEmail(email).getId(), descriptor);
            return ResponseEntity.ok().body(Map.of("mensaje", "Rostro guardado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}