package com.clinicaapp.config;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    @Autowired private UsuarioRepository usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) return;

        Object principal = authentication.getPrincipal();

        String email = null;
        Map<String, Object> attributes = null;

        if (principal instanceof OidcUser) {
            OidcUser oidc = (OidcUser) principal;
            attributes = oidc.getClaims();
            Object e = attributes.get("email");
            if (e != null) email = e.toString();
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth = (OAuth2User) principal;
            attributes = oauth.getAttributes();
            Object e = attributes.get("email");
            if (e != null) email = e.toString();
        } else if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        }

        log.debug("Authentication success for principal type={}, email={}, attributes={}",
                principal != null ? principal.getClass().getName() : "null", email, attributes);

        boolean newUser = false;

        try {
            if (email != null) {
                Usuario u = usuarioRepository.findByEmail(email);
                if (u == null) {
                    log.info("No internal Usuario found for email={}, treating as new user", email);
                    newUser = true;
                } else {
                    // If no telephone (or other required fields) -> treat as new/incomplete
                    if (u.getTelefono() == null || u.getTelefono().isBlank()) {
                        log.info("Usuario {} found but profile incomplete (telefono missing)", email);
                        newUser = true;
                    }
                }
            } else {
                log.warn("Could not determine email from OAuth2 principal; defaulting to existing user path");
            }
        } catch (Exception ex) {
            log.error("Error checking internal usuario during OAuth2 success: {}", ex.getMessage());
        }

        if (newUser) {
            response.sendRedirect("/usuario/welcome");
        } else {
            response.sendRedirect("/usuario/dashboard");
        }
    }
}