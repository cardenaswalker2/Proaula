package com.clinicaapp.service.impl;

import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.service.IEmailService;
import com.clinicaapp.service.IUsuarioService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private IEmailService emailService; // ← AGREGADO PARA EMAILS

    

    @Override
    public Usuario save(UsuarioRegistroDTO registroDTO) {
        if (usuarioRepository.findByEmail(registroDTO.getEmail()) != null) {
            throw new RuntimeException("El email ya está registrado: " + registroDTO.getEmail());
        }
        Usuario usuario = new Usuario();
        BeanUtils.copyProperties(registroDTO, usuario);
        usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));
        usuario.setRole(Role.ROLE_USER);
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // ← ENVIAR EMAIL DE BIENVENIDA
        enviarEmailBienvenida(usuarioGuardado);

        return usuarioGuardado;
    }

    @Override
    public Usuario createUsuarioWithRole(UsuarioRegistroDTO registroDTO, Role role) {
        if (usuarioRepository.findByEmail(registroDTO.getEmail()) != null) {
            throw new RuntimeException("El email ya está registrado: " + registroDTO.getEmail());
        }
        Usuario usuario = new Usuario();
        BeanUtils.copyProperties(registroDTO, usuario);
        usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));
        usuario.setRole(role);
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // ← ENVIAR EMAIL DE BIENVENIDA
        enviarEmailBienvenida(usuarioGuardado);

        return usuarioGuardado;
    }

    // ← NUEVO MÉTODO PARA ENVIAR EMAIL DE BIENVENIDA
    private void enviarEmailBienvenida(Usuario usuario) {
        try {
            String fechaRegistro = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "ES")));

            String emailSubject = "🎉 ¡Bienvenido a ClinicaApp! Registro Exitoso";

            String emailBody = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    body { margin: 0; padding: 0; background-color: #f4f4f4; }
                                    .container { max-width: 600px; margin: 0 auto; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
                                    .header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); color: white; padding: 50px 30px; text-align: center; border-radius: 10px 10px 0 0; }
                                    .header h1 { margin: 0; font-size: 32px; }
                                    .header p { margin: 10px 0 0 0; font-size: 18px; opacity: 0.95; }
                                    .content { background: #ffffff; padding: 40px 30px; border-radius: 0 0 10px 10px; }
                                    .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
                                    .message { color: #555; line-height: 1.8; font-size: 16px; margin: 20px 0; }
                                    .info-box { background: #e8f5e9; padding: 20px; margin: 25px 0; border-radius: 8px; border-left: 4px solid #11998e; }
                                    .info-box p { margin: 8px 0; color: #2e7d32; font-size: 15px; }
                                    .btn { display: inline-block; background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); color: white; padding: 15px 40px; text-decoration: none; border-radius: 30px; font-weight: bold; font-size: 16px; margin-top: 25px; box-shadow: 0 4px 15px rgba(17, 153, 142, 0.3); }
                                    .features { margin: 30px 0; }
                                    .feature { display: flex; align-items: center; margin: 15px 0; color: #555; }
                                    .feature-icon { font-size: 24px; margin-right: 15px; width: 30px; text-align: center; }
                                    .footer { text-align: center; color: #999; margin-top: 35px; font-size: 13px; line-height: 1.6; padding-top: 20px; border-top: 1px solid #eee; }
                                    .footer a { color: #11998e; text-decoration: none; }
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

                                        <p class="message">Nos alegra tenerte como parte de la familia ClinicaApp. Tu registro se completó correctamente el <strong>%s</strong>.</p>

                                        <div class="info-box">
                                            <p>✅ <strong>Email registrado:</strong> %s</p>
                                            <p>✅ <strong>Tipo de cuenta:</strong> Usuario</p>
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

                                        <center>
                                            <a href="http://localhost:8080/login" class="btn">Iniciar Sesión</a>
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
                    usuario.getEmail());

            emailService.sendSimpleMessage(usuario.getEmail(), emailSubject, emailBody);
            log.info("Email de bienvenida enviado exitosamente a {}", usuario.getEmail());

        } catch (Exception e) {
            log.error("Error al enviar email de bienvenida a {}: {}", usuario.getEmail(), e.getMessage());
            // No lanzamos excepción para que el registro no falle si el email no se envía
        }
    }

    @Override
    public Optional<Usuario> findById(String id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public List<Usuario> findAllUsers() {
        return usuarioRepository.findAll();
    }

    @Override
    public List<Usuario> findByRole(Role role) {
        return usuarioRepository.findByRole(role);
    }

    @Override
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public Usuario update(String id, UsuarioRegistroDTO usuarioDTO, Role role) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para actualizar con ID: " + id));
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setTelefono(usuarioDTO.getTelefono());
        usuario.setRole(role);
        if (StringUtils.hasText(usuarioDTO.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario update(String id, UsuarioRegistroDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para actualizar con ID: " + id));
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        usuario.setTelefono(usuarioDTO.getTelefono());
        if (StringUtils.hasText(usuarioDTO.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public void deleteById(String id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado para eliminar con ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Buscar al usuario en la base de datos por email
        Usuario usuario = usuarioRepository.findByEmail(email);

        // 2. Si no existe, lanzamos error de credenciales
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario o contraseña inválidos.");
        }

        // 3. NUEVO: Verificar si la cuenta está activa (controlado por el Admin)
        if (!usuario.isActivo()) {
            // Esta excepción es capturada automáticamente por Spring Security
            throw new org.springframework.security.authentication.DisabledException(
                    "Tu cuenta ha sido suspendida. Por favor, contacta al administrador.");
        }

        // 4. Retornar el objeto UserDetails con los parámetros de estado
        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.isActivo(), // enabled (Si es false, no deja entrar)
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                mapRolesToAuthorities(usuario.getRole()));
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Role role) {
        return Arrays.asList(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public void actualizarDatosContacto(String usuarioId, String nuevoEmail, String nuevoTelefono) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado para actualizar contacto con ID: " + usuarioId));

        if (!usuario.getEmail().equalsIgnoreCase(nuevoEmail)) {
            if (usuarioRepository.findByEmail(nuevoEmail) != null) {
                throw new IllegalStateException(
                        "El nuevo email '" + nuevoEmail + "' ya está registrado por otro usuario.");
            }
            usuario.setEmail(nuevoEmail);
        }

        usuario.setTelefono(nuevoTelefono);

        usuarioRepository.save(usuario);
        log.info("Datos de contacto actualizados para el usuario ID {}", usuarioId);
    }

    @Override
    public void createPasswordResetTokenForUser(Usuario user, String token) {
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(user);
        log.info("Token de reseteo creado para el usuario {}", user.getEmail());
    }

    @Override
    public Usuario validatePasswordResetToken(String token) {
        Usuario user = usuarioRepository.findByResetPasswordToken(token);

        if (user == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            return null;
        }

        return user;
    }

    @Override
    public void changeUserPassword(Usuario user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        usuarioRepository.save(user);
        log.info("Contraseña cambiada exitosamente para el usuario {}", user.getEmail());
    }

    @Override
    public void saveExisting(Usuario usuario) {
        // Simplemente guardamos el objeto.
        // Como el objeto ya tiene un ID, MongoDB/JPA sabe que es una actualización.
        usuarioRepository.save(usuario);
    }

    @Override
    public void registrarRostro(String usuarioId, List<Double> descriptor) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setDescriptorFacial(descriptor);
        usuario.setFacialLoginHabilitado(true);
        usuarioRepository.save(usuario);
        log.info("Rostro registrado exitosamente para el usuario: {}", usuario.getEmail());
    }

    public Usuario loginFacial(List<Double> descriptorEntrante) {
    List<Usuario> usuarios = usuarioRepository.findAll();
    
    Usuario mejorCandidato = null;
    double menorDistancia = 1.0; 

    // UMBRAL DE SEGURIDAD MÁXIMA
    // 0.38 es el "punto dulce" donde nadie más entrará, pero tú sí.
    double UMBRAL_ESTRICTO = 0.38; 

    for (Usuario u : usuarios) {
        if (u.getDescriptorFacial() != null && u.isFacialLoginHabilitado()) {
            double distancia = calcularDistanciaEuclidiana(descriptorEntrante, u.getDescriptorFacial());
            
            // Log para que veas en la consola quién intenta entrar y con qué distancia
            System.out.println("Comparando con: " + u.getNombre() + " | Distancia: " + distancia);

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejorCandidato = u;
            }
        }
    }

    // SOLO SI LA COINCIDENCIA ES CASI PERFECTA (distancia < 0.38)
    if (mejorCandidato != null && menorDistancia < UMBRAL_ESTRICTO) {
        return mejorCandidato;
    }

    return null; // Nadie entra si no es idéntico
}

// Método matemático que NO debe fallar
private double calcularDistanciaEuclidiana(List<Double> v1, List<Double> v2) {
    double suma = 0;
    for (int i = 0; i < v1.size(); i++) {
        suma += Math.pow(v1.get(i) - v2.get(i), 2);
    }
    return Math.sqrt(suma);
}

    @Override
    public org.springframework.data.domain.Page<Usuario> findPaginated(String keyword, Role role, org.springframework.data.domain.Pageable pageable) {
        if (role != null && StringUtils.hasText(keyword)) {
            // Filtrado por Rol AND Keyword
            return usuarioRepository.findByRoleAndNombreContainingIgnoreCaseOrRoleAndApellidoContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
                role, keyword, role, keyword, role, keyword, pageable);
        } else if (role != null) {
            // Filtrado solo por Rol
            return usuarioRepository.findByRole(role, pageable);
        } else if (StringUtils.hasText(keyword)) {
            // Filtrado solo por Keyword
            return usuarioRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrEmailContainingIgnoreCase(
                keyword, keyword, keyword, pageable);
        } else {
            // Sin filtros, devolver todos paginados
            return usuarioRepository.findAll(pageable);
        }
    }
}