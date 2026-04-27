package com.clinicaapp.service.impl;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.service.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Locale;

@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOidcUserService.class);

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private IEmailService emailService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(oidcUser.getClaims());

        log.debug("CustomOidcUserService.loadUser: registrationId={}, claims={}",
                userRequest.getClientRegistration().getRegistrationId(), attributes);

        // Processar/crear usuario interno
        Usuario usuarioInterno = processOidcUser(attributes);
        if (usuarioInterno == null) {
            // safety placeholder
            usuarioInterno = new Usuario();
            usuarioInterno.setId("unknown");
            usuarioInterno.setEmail((String) attributes.getOrDefault("email", "unknown@unknown"));
            usuarioInterno.setNombre((String) attributes.getOrDefault("given_name", "Usuario"));
            usuarioInterno.setApellido((String) attributes.getOrDefault("family_name", ""));
            usuarioInterno.setRole(Role.ROLE_USER);
        }

        // Añadir atributos útiles
        attributes.put("internal_user_id", usuarioInterno.getId());
        attributes.put("is_new_user", usuarioInterno.getTelefono() == null);

        // Crear autoridades
        Set<GrantedAuthority> authorities = new HashSet<>();
        String roleName = usuarioInterno.getRole() != null ? usuarioInterno.getRole().name() : Role.ROLE_USER.name();
        if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;
        authorities.add(new SimpleGrantedAuthority(roleName));

        // Devolver un DefaultOidcUser con autoridades y claims
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
    }

    private Usuario processOidcUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        if (email == null) {
            // Try alternative claim names
            email = (String) attributes.get("preferred_username");
        }

        if (email == null) {
            log.warn("OIDC user has no email claim: {}", attributes);
            return null;
        }

        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            log.info("Creando nuevo usuario desde OIDC: {}", email);
            
            // Extraer nombre y apellido de los claims de Google
            String givenName = (String) attributes.get("given_name");
            String familyName = (String) attributes.get("family_name");
            String name = (String) attributes.get("name");
            
            String nombre;
            String apellido;
            
            if (givenName != null) {
                nombre = givenName;
                apellido = familyName != null ? familyName : "";
            } else {
                // Fallback si no hay given_name
                if (name == null || name.isBlank()) {
                    name = (String) attributes.getOrDefault("preferred_username", email);
                }
                String[] nameParts = name.split("\\s+");
                nombre = nameParts.length > 0 ? nameParts[0] : "Usuario";
                apellido = (nameParts.length > 1) ? nameParts[nameParts.length - 1] : "";
            }

            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setRole(Role.ROLE_USER);
            usuario.setFechaCreacion(LocalDateTime.now()); // Agregar fecha de creación
            usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            Usuario saved = usuarioRepository.save(usuario);

            // ← EMAIL DE BIENVENIDA MEJORADO
            enviarEmailBienvenida(saved);

            return saved;
        }

        log.info("Usuario existente ha iniciado sesión vía OIDC: {}", email);
        return usuario;
    }
    
    // ← NUEVO MÉTODO: Email profesional de bienvenida
    private void enviarEmailBienvenida(Usuario usuario) {
        try {
            String fechaRegistro = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "ES"))
            );
            
            String emailSubject = "🎉 ¡Bienvenido a ClinicaApp! Registro Exitoso";
            
            String emailBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { margin: 0; padding: 0; background-color: #f4f4f4; }
                        .container { max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 50px 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .header h1 { margin: 0; font-size: 32px; }
                        .header p { margin: 10px 0 0 0; font-size: 18px; opacity: 0.95; }
                        .content { background: #ffffff; padding: 40px 30px; border-radius: 0 0 10px 10px; }
                        .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
                        .message { color: #555; line-height: 1.8; font-size: 16px; margin: 20px 0; }
                        .info-box { background: #e8eaf6; padding: 20px; margin: 25px 0; border-radius: 8px; border-left: 4px solid #667eea; }
                        .info-box p { margin: 8px 0; color: #3f51b5; font-size: 15px; }
                        .google-badge { display: inline-block; background: #fff; border: 1px solid #ddd; padding: 10px 20px; border-radius: 25px; font-size: 14px; color: #666; margin: 15px 0; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
                        .google-badge img { width: 18px; height: 18px; vertical-align: middle; margin-right: 8px; }
                        .btn { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 30px; font-weight: bold; font-size: 16px; margin-top: 25px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3); transition: transform 0.2s; }
                        .btn:hover { transform: translateY(-2px); }
                        .features { margin: 30px 0; }
                        .feature { display: flex; align-items: center; margin: 15px 0; color: #555; }
                        .feature-icon { font-size: 24px; margin-right: 15px; width: 30px; text-align: center; }
                        .tip-box { background: #fff3cd; padding: 20px; border-radius: 8px; border-left: 4px solid #ffc107; margin: 25px 0; }
                        .tip-box p { margin: 0; color: #856404; font-size: 15px; line-height: 1.6; }
                        .footer { text-align: center; color: #999; margin-top: 35px; font-size: 13px; line-height: 1.6; padding-top: 20px; border-top: 1px solid #eee; }
                        .footer a { color: #667eea; text-decoration: none; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 ¡Bienvenido/a!</h1>
                            <p>Tu cuenta ha sido creada exitosamente</p>
                        </div>
                        <div class="content">
                            <p class="greeting">Hola <strong>%s %s</strong>,</p>
                            
                            <div class="google-badge">
                                🔐 Registrado con Google
                            </div>
                            
                            <p class="message">Nos alegra tenerte como parte de la familia ClinicaApp. Tu registro se completó correctamente el <strong>%s</strong> usando tu cuenta de Google.</p>
                            
                            <div class="info-box">
                                <p>✅ <strong>Email registrado:</strong> %s</p>
                                <p>✅ <strong>Tipo de cuenta:</strong> Usuario</p>
                                <p>✅ <strong>Método de acceso:</strong> Google OAuth</p>
                            </div>
                            
                            <p class="message">Con tu cuenta podrás:</p>
                            
                            <div class="features">
                                <div class="feature">
                                    <span class="feature-icon">📅</span>
                                    <span>Agendar citas médicas en línea</span>
                                </div>
                                <div class="feature">
                                    <span class="feature-icon">💳</span>
                                    <span>Pagar tus servicios de forma segura</span>
                                </div>
                                <div class="feature">
                                    <span class="feature-icon">📋</span>
                                    <span>Ver tu historial de citas</span>
                                </div>
                                <div class="feature">
                                    <span class="feature-icon">🔔</span>
                                    <span>Recibir notificaciones y recordatorios</span>
                                </div>
                            </div>
                            
                            <div class="tip-box">
                                <p><strong>💡 Tip:</strong> Completa tu perfil agregando tu número de teléfono para recibir recordatorios por SMS de tus próximas citas.</p>
                            </div>
                            
                            <center>
                                <a href="http://localhost:8080/usuario/welcome" class="btn">Completar mi perfil</a>
                            </center>
                            
                            <div class="footer">
                                <p>¿Necesitas ayuda? Contáctanos en <a href="mailto:soporte@clinicaapp.com">soporte@clinicaapp.com</a></p>
                                <p>© 2024 ClinicaApp - Todos los derechos reservados</p>
                                <p style="font-size: 11px; color: #bbb; margin-top: 10px;">Este es un correo automático, por favor no respondas a este mensaje.</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                usuario.getNombre(),
                usuario.getApellido() != null ? usuario.getApellido() : "",
                fechaRegistro,
                usuario.getEmail()
            );
            
            emailService.sendSimpleMessage(usuario.getEmail(), emailSubject, emailBody);
            log.info("Email de bienvenida profesional enviado a {}", usuario.getEmail());
            
        } catch (Exception e) {
            log.error("Error al enviar email de bienvenida a {}: {}", usuario.getEmail(), e.getMessage());
            // No lanzamos excepción para que el registro no falle
        }
    }
}