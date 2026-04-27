package com.clinicaapp.service.impl;

import com.clinicaapp.dto.ClinicaDTO;
import com.clinicaapp.dto.AdminClinicaDTO;
import com.clinicaapp.dto.RegistroClinicaDTO;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.EstadoClinica;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.repository.UsuarioRepository;
import com.clinicaapp.repository.ServicioRepository;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.service.IClinicaService;
import com.clinicaapp.service.IEmailService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClinicaServiceImpl implements IClinicaService {

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private IEmailService emailService; // Inyecta el servicio de email aquí

    @Autowired
    private ServicioRepository servicioRepository;

    @Override
    public Clinica saveAdmin(AdminClinicaDTO adminDTO, MultipartFile imagenFile) {
        Clinica clinica;
        String usuarioId = adminDTO.getUsuarioAdminId();

        // 1. Si es una clínica nueva y vienen datos de dueño, creamos al usuario
        if (!StringUtils.hasText(adminDTO.getId()) && StringUtils.hasText(adminDTO.getEmailDuenio())) {
            Usuario nuevoAdmin = new Usuario();
            nuevoAdmin.setNombre(adminDTO.getNombreDuenio());
            nuevoAdmin.setApellido(adminDTO.getApellidoDuenio());
            nuevoAdmin.setEmail(adminDTO.getEmailDuenio());
            nuevoAdmin.setPassword(passwordEncoder.encode(adminDTO.getPasswordDuenio()));
            nuevoAdmin.setRole(Role.ROLE_CLINICA);
            nuevoAdmin.setActivo(true); // Super-Admin lo crea activo por defecto
            nuevoAdmin.setFechaCreacion(LocalDateTime.now());
            
            Usuario guardado = usuarioRepository.save(nuevoAdmin);
            usuarioId = guardado.getId();
        }

        // 2. Gestionar la clínica
        if (StringUtils.hasText(adminDTO.getId())) {
            clinica = clinicaRepository.findById(adminDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Clínica no encontrada"));
        } else {
            clinica = new Clinica();
            clinica.setEstado(EstadoClinica.APROBADA);
        }

        // 3. Poblar datos - Ignoramos el ID para que BeanUtils no sobrescriba el ID de MongoDB con un String vacío
        BeanUtils.copyProperties(adminDTO, clinica, "id");
        if (usuarioId != null) {
            clinica.setUsuarioAdminId(usuarioId);
        }

        // 4. Imagen
        if (imagenFile != null && !imagenFile.isEmpty()) {
            clinica.setImagenUrl(guardarImagen(imagenFile));
        }

        return clinicaRepository.save(clinica);
    }

    @Override
    public Clinica save(ClinicaDTO clinicaDTO, MultipartFile imagenFile) {
        Clinica clinica;
        if (StringUtils.hasText(clinicaDTO.getId())) {
            clinica = clinicaRepository.findById(clinicaDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Clínica no encontrada con ID: " + clinicaDTO.getId()));
        } else {
            clinica = new Clinica();
            clinica.setEstado(EstadoClinica.APROBADA); // Aprobación automática si la crea el Admin
        }
        
        BeanUtils.copyProperties(clinicaDTO, clinica, "id");

        // Si se subió un archivo, lo guardamos y actualizamos la URL
        if (imagenFile != null && !imagenFile.isEmpty()) {
            String urlImagen = guardarImagen(imagenFile);
            clinica.setImagenUrl(urlImagen);
        }
        // Si no se subió archivo, respetamos lo que ya tiene clinica (poblado por BeanUtils)

        return clinicaRepository.save(clinica);
    }

    @Override
    public Optional<Clinica> findById(String id) {
        return clinicaRepository.findById(id);
    }

    @Override
    public void deleteById(String id) {
        clinicaRepository.deleteById(id);
    }

    @Override
    public List<Clinica> findAll() {
        return clinicaRepository.findAll();
    }

    @Override
    public Page<Clinica> findAll(Pageable pageable) {
        return clinicaRepository.findByEstado(EstadoClinica.APROBADA, pageable);
    }

    @Override
    public Page<Clinica> search(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return findAll(pageable);
        }

        // 1. Buscar IDs de servicios que coincidan con la query
        Page<Servicio> serviciosMatching = servicioRepository.findByNombreContainingIgnoreCase(query,
                Pageable.unpaged());
        java.util.List<String> serviceIds = serviciosMatching.getContent().stream()
                .map(Servicio::getId)
                .collect(java.util.stream.Collectors.toList());

        if (!serviceIds.isEmpty()) {
            // Búsqueda combinada: Atributos de clínica O Servicios ofrecidos
            return clinicaRepository.findPublicWithServices(EstadoClinica.APROBADA, query, serviceIds, pageable);
        }

        // 2. Búsqueda por texto en nombre, dirección o descripción para clínicas
        // APROBADAS
        return clinicaRepository.findPublic(EstadoClinica.APROBADA, query, pageable);
    }

    @Override
    public Page<Clinica> findPaginated(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return clinicaRepository.findByNombreContainingIgnoreCase(keyword, pageable);
        } else {
            return clinicaRepository.findAll(pageable);
        }
    }

    // --- ESTE MÉTODO ES EL QUE TE FALTABA Y POR ESO DABA ERROR ---
    @Override
    public List<Clinica> buscarPorEstado(EstadoClinica estado) {
        return clinicaRepository.findByEstado(estado);
    }

    @Override
    public void registrarSolicitudClinica(RegistroClinicaDTO dto, MultipartFile imagenFile) {
        // 1. Crear el usuario (Dueño) DESACTIVADO
        Usuario nuevoAdmin = new Usuario();
        nuevoAdmin.setNombre(dto.getNombreDuenio());
        nuevoAdmin.setApellido(dto.getApellidoDuenio());
        nuevoAdmin.setEmail(dto.getEmailDuenio());
        nuevoAdmin.setPassword(passwordEncoder.encode(dto.getPassword()));
        nuevoAdmin.setRole(Role.ROLE_CLINICA);
        nuevoAdmin.setActivo(false); // IMPORTANTE: No puede entrar aún
        nuevoAdmin.setFechaCreacion(LocalDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(nuevoAdmin);

        // 2. Crear la clínica PENDIENTE
        Clinica clinica = new Clinica();
        clinica.setNombre(dto.getNombreClinica());
        clinica.setDireccion(dto.getDireccion());
        clinica.setEmail(dto.getEmailClinica());
        clinica.setTelefono(dto.getTelefonoClinica());
        clinica.setDescripcion(dto.getDescripcion());
        clinica.setEstado(EstadoClinica.PENDIENTE);
        clinica.setUsuarioAdminId(usuarioGuardado.getId());

        // Manejo de la Imagen
        if (imagenFile != null && !imagenFile.isEmpty()) {
            String urlImagen = guardarImagen(imagenFile);
            clinica.setImagenUrl(urlImagen);
        } else {
            // Imagen por defecto si no suben ninguna
            clinica.setImagenUrl("/img/clinica-vetzone.jpg");
        }

        clinicaRepository.save(clinica);
    }

    private String guardarImagen(MultipartFile file) {
        try {
            // Directorio donde se guardarán las imágenes (FUERA de src para visibilidad inmediata)
            String uploadDir = "uploads";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Nombre de archivo único para evitar colisiones
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Retornamos la URL relativa que ahora WebConfig sirve como recurso estático
            return "/uploads/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la imagen: " + e.getMessage());
        }
    }

    @Override
    public void aprobarClinica(String id) {
        Clinica clinica = clinicaRepository.findById(id).orElseThrow();
        Usuario user = usuarioRepository.findById(clinica.getUsuarioAdminId()).orElseThrow();

        clinica.setEstado(EstadoClinica.APROBADA);
        clinicaRepository.save(clinica);

        user.setActivo(true);
        usuarioRepository.save(user);

        // --- DISEÑO DE CORREO PREMIUM ---
        String subject = "¡Tu Clínica ha sido Aprobada! 🏥 - ClínicaApp";
        String loginUrl = "http://localhost:8080/login"; // Cambia esto por tu dominio real en producción

        String htmlBody = String.format(
                """
                        <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; padding: 40px 0;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05);">

                                <!-- Banner Superior con Gradiente -->
                                <div style="background: linear-gradient(135deg, #1e40af 0%%, #3b82f6 100%%); padding: 40px 20px; text-align: center; color: white;">
                                    <span style="font-size: 50px;">🎉</span>
                                    <h1 style="margin: 10px 0 0; font-size: 28px; font-weight: 800;">¡Clínica Aprobada!</h1>
                                    <p style="margin: 5px 0 0; opacity: 0.9; font-size: 16px;">Tu solicitud ha sido procesada con éxito</p>
                                </div>

                                <div style="padding: 40px; color: #334155;">
                                    <p style="font-size: 18px;">Hola <strong>%s</strong>,</p>
                                    <p style="line-height: 1.6; font-size: 16px;">
                                        Nos complace informarte que la clínica <strong>"%s"</strong> ha pasado nuestro proceso de revisión y ya se encuentra activa en nuestra plataforma.
                                    </p>

                                    <!-- Cuadro de Detalles -->
                                    <div style="background-color: #f8fafc; border-left: 4px solid #3b82f6; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                        <div style="margin-bottom: 10px;">
                                            <span style="color: #64748b; font-size: 14px; font-weight: 600;">Email de Acceso:</span><br>
                                            <strong style="color: #1e293b; font-size: 16px;">%s</strong>
                                        </div>
                                        <div>
                                            <span style="color: #64748b; font-size: 14px; font-weight: 600;">Tipo de cuenta:</span><br>
                                            <strong style="color: #1e293b; font-size: 16px;">Administrador de Clínica</strong>
                                        </div>
                                    </div>

                                    <p style="font-size: 16px; font-weight: 600; margin-bottom: 20px;">Desde ahora ya puedes:</p>

                                    <!-- Lista de beneficios con iconos -->
                                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 30px;">
                                        <tr>
                                            <td style="padding: 10px 0; width: 40px; font-size: 20px;">📅</td>
                                            <td style="padding: 10px 0; font-size: 15px; color: #475569;">Configurar tus horarios y servicios</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 10px 0; width: 40px; font-size: 20px;">👨‍⚕️</td>
                                            <td style="padding: 10px 0; font-size: 15px; color: #475569;">Gestionar historias clínicas digitales</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 10px 0; width: 40px; font-size: 20px;">💰</td>
                                            <td style="padding: 10px 0; font-size: 15px; color: #475569;">Controlar tus ingresos y citas en tiempo real</td>
                                        </tr>
                                    </table>

                                    <!-- Botón de Acción -->
                                    <div style="text-align: center; margin-top: 30px;">
                                        <a href="%s" style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); color: white; text-decoration: none; padding: 16px 40px; border-radius: 50px; font-weight: 700; font-size: 16px; display: inline-block; box-shadow: 0 4px 10px rgba(16, 185, 129, 0.3);">
                                            Acceder a mi Panel
                                        </a>
                                    </div>
                                </div>

                                <!-- Footer -->
                                <div style="background-color: #f1f5f9; padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                                    <p style="margin: 0; font-size: 14px; color: #64748b;">¿Necesitas ayuda? Escríbenos a <a href="mailto:soporte@clinicaapp.com" style="color: #3b82f6; text-decoration: none;">soporte@clinicaapp.com</a></p>
                                    <p style="margin: 15px 0 0; font-size: 12px; color: #94a3b8;">© 2024 ClínicaApp - Todos los derechos reservados</p>
                                </div>
                            </div>
                        </div>
                        """,
                user.getNombre(),
                clinica.getNombre(),
                user.getEmail(),
                loginUrl);

        emailService.sendSimpleMessage(user.getEmail(), subject, htmlBody);
    }

    @Override
    public void rechazarClinica(String id) {
        Clinica clinica = clinicaRepository.findById(id).orElseThrow();
        Usuario user = usuarioRepository.findById(clinica.getUsuarioAdminId()).orElseThrow();

        // --- DISEÑO DE CORREO DE RECHAZO PREMIUM ---
        String subject = "Actualización sobre tu solicitud ⚠️ - ClínicaApp";
        String soporteUrl = "mailto:soporte@clinicaapp.com"; // Link directo a tu correo

        String htmlBody = String.format(
                """
                        <div style="font-family: 'Segoe UI', Arial, sans-serif; background-color: #fdf2f2; padding: 40px 0;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05);">

                                <!-- Banner Superior con Gradiente Rojo -->
                                <div style="background: linear-gradient(135deg, #dc2626 0%%, #ef4444 100%%); padding: 40px 20px; text-align: center; color: white;">
                                    <span style="font-size: 50px;">⚠️</span>
                                    <h1 style="margin: 10px 0 0; font-size: 26px; font-weight: 800;">Estado de Solicitud</h1>
                                    <p style="margin: 5px 0 0; opacity: 0.9; font-size: 16px;">Información importante sobre tu registro</p>
                                </div>

                                <div style="padding: 40px; color: #334155;">
                                    <p style="font-size: 18px;">Hola <strong>%s</strong>,</p>
                                    <p style="line-height: 1.6; font-size: 16px;">
                                        Lamentamos informarte que, tras revisar detalladamente la solicitud de registro para la clínica <strong>"%s"</strong>, no hemos podido aprobarla en esta ocasión.
                                    </p>

                                    <!-- Cuadro de Notificación -->
                                    <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                        <p style="margin: 0; color: #991b1b; font-size: 15px; font-weight: 600;">
                                            Tu registro ha sido declinado por nuestro equipo de validación.
                                        </p>
                                    </div>

                                    <p style="font-size: 15px; color: #64748b; margin-bottom: 25px;">
                                        Esto puede deberse a información incompleta, falta de documentación necesaria o inconsistencias en los datos proporcionados durante el registro.
                                    </p>

                                    <p style="font-size: 16px; font-weight: 600; margin-bottom: 20px;">¿Qué puedes hacer ahora?</p>

                                    <!-- Lista de pasos a seguir -->
                                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 30px;">
                                        <tr>
                                            <td style="padding: 10px 0; width: 40px; font-size: 20px;">📧</td>
                                            <td style="padding: 10px 0; font-size: 15px; color: #475569;">Contactar a nuestro equipo para conocer los detalles específicos.</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 10px 0; width: 40px; font-size: 20px;">📝</td>
                                            <td style="padding: 10px 0; font-size: 15px; color: #475569;">Realizar una nueva solicitud asegurándote de que todos los datos sean correctos.</td>
                                        </tr>
                                    </table>

                                    <!-- Botón de Contacto -->
                                    <div style="text-align: center; margin-top: 30px;">
                                        <a href="%s" style="background: #334155; color: white; text-decoration: none; padding: 14px 35px; border-radius: 50px; font-weight: 700; font-size: 15px; display: inline-block;">
                                            Contactar a Soporte
                                        </a>
                                    </div>
                                </div>

                                <!-- Footer -->
                                <div style="background-color: #f8fafc; padding: 30px; text-align: center; border-top: 1px solid #f1f5f9;">
                                    <p style="margin: 0; font-size: 12px; color: #94a3b8;">
                                        Si crees que esto es un error, por favor responde a este correo para que un agente pueda revisar tu caso manualmente.
                                    </p>
                                    <p style="margin: 15px 0 0; font-size: 12px; color: #cbd5e1;">© 2024 ClínicaApp - Infraestructura de Salud Digital</p>
                                </div>
                            </div>
                        </div>
                        """,
                user.getNombre(),
                clinica.getNombre(),
                soporteUrl);

        emailService.sendSimpleMessage(user.getEmail(), subject, htmlBody);

        // Finalmente, borramos los registros para liberar el email y que puedan
        // intentar de nuevo
        clinicaRepository.delete(clinica);
        usuarioRepository.delete(user);
    }
}