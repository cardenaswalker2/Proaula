package com.clinicaapp.config;

import com.clinicaapp.model.enums.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) return;

        // ESTE HANDLER ES PARA LOGIN CON FORMULARIO
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            String authorityName = grantedAuthority.getAuthority();

            // 1. Redirección para ADMINISTRADOR
            if (authorityName.equals(Role.ROLE_ADMIN.name())) {
                response.sendRedirect("/admin/dashboard");
                return;
            } 
            
            // 2. Redirección para RECEPCIONISTA
            else if (authorityName.equals(Role.ROLE_RECEPCIONISTA.name())) {
                response.sendRedirect("/recepcion/dashboard");
                return;
            } 
            
            // 3. NUEVA: Redirección para CLÍNICA
            else if (authorityName.equals(Role.ROLE_CLINICA.name())) {
                response.sendRedirect("/clinica/dashboard");
                return;
            } 
            
            // 4. Redirección para USUARIO / CLIENTE
            else if (authorityName.equals(Role.ROLE_USER.name())) {
                response.sendRedirect("/usuario/dashboard");
                return;
            }
        }
        
        // Redirección por defecto si no coincide ningún rol específico
        response.sendRedirect("/");
    }
}