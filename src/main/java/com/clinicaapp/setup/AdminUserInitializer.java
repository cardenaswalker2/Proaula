package com.clinicaapp.setup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.Servicio;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.model.enums.EstadoClinica; // Importante para el estado
import com.clinicaapp.repository.ClinicaRepository;
import com.clinicaapp.service.IClinicaService;
import com.clinicaapp.service.IServicioService;
import com.clinicaapp.service.IUsuarioService;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private IClinicaService clinicaService;
    @Autowired
    private IServicioService servicioService;
    @Autowired
    private ClinicaRepository clinicaRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createDefaultUsers();
        List<Servicio> servicios = createDefaultServices();
        createClinicasReales(servicios);
    }

    private void createDefaultUsers() {
        if (usuarioService.findByEmail("admin@clinica.app") == null) {
            log.info("Creando usuario admin por defecto...");
            usuarioService.createUsuarioWithRole(
                    new UsuarioRegistroDTO("Admin", "Principal", "admin@clinica.app", "admin123", "999999999"),
                    Role.ROLE_ADMIN);
        }
        if (usuarioService.findByEmail("recepcion@clinica.app") == null) {
            log.info("Creando usuario recepcionista por defecto...");
            usuarioService.createUsuarioWithRole(
                    new UsuarioRegistroDTO("Recepcionista", "Uno", "recepcion@clinica.app", "recep123", "888888888"),
                    Role.ROLE_RECEPCIONISTA);
        }
        if (usuarioService.findByEmail("cliente@clinica.app") == null) {
            log.info("Creando usuario cliente de prueba...");
            usuarioService.save(
                    new UsuarioRegistroDTO("Usuario", "De Prueba", "cliente@clinica.app", "cliente123",
                            "+573005722844"));
        }
    }

    private List<Servicio> createDefaultServices() {
        if (!servicioService.findAll().isEmpty()) {
            return servicioService.findAll();
        }
        log.info("Creando servicios por defecto...");
        List<Servicio> servicios = List.of(
                crearServicio("Consulta General", "Revisión completa de la salud de la mascota.", 65000.0),
                crearServicio("Consulta Especialista", "Atención por cardiólogo, neurólogo u oncólogo.", 150000.0),
                crearServicio("Urgencia Vital 24h", "Atención inmediata por emergencia médica.", 120000.0),
                crearServicio("Control Post-Operatorio", "Seguimiento médico tras cirugía.", 40000.0),
                crearServicio("Plan Vacunación Inicial", "Paquete completo para cachorros y gatitos.", 110000.0),
                crearServicio("Vacunación Rabia", "Refuerzo anual obligatorio de rabia.", 35000.0),
                crearServicio("Vacunación Triple Felina", "Protección contra virus respiratorios y panleucopenia.", 45000.0),
                crearServicio("Vacunación Pentavalente", "Protección canina contra 5 virus principales.", 55000.0),
                crearServicio("Desparasitación Integral", "Eliminación de parásitos internos y externos.", 55000.0),
                crearServicio("Limpieza Dental Proh", "Eliminación de sarro mediante ultrasonido.", 180000.0),
                crearServicio("Ecografía Diagnóstica", "Imagen de alta resolución para órganos internos.", 130000.0),
                crearServicio("Radiografía Digital", "Estudio de imagen rápido para huesos y tórax.", 95000.0),
                crearServicio("Laboratorio Completo", "Hemograma y bioquímica sanguínea detallada.", 115000.0),
                crearServicio("Cirugía Tejidos Blandos", "Procedimientos quirúrgicos generales.", 450000.0),
                crearServicio("Esterilización Perro", "Cirugía programada (Ovariohisterectomía/Orquiectomía).", 350000.0),
                crearServicio("Esterilización Gato", "Cirugía programada mínimamente invasiva.", 190000.0),
                crearServicio("Hospitalización Día", "Monitoreo profesional y fluidoterapia continua.", 110000.0),
                crearServicio("Estética Veterinaria", "Baño medicado, corte y limpieza de oídos.", 45000.0),
                crearServicio("Microchip ID", "Identificación electrónica internacional.", 60000.0),
                crearServicio("Cremación Humanitaria", "Tratamiento digno final con cenizas.", 450000.0));
        return servicios;
    }

    private Servicio crearServicio(String nombre, String descripcion, double costo) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDescripcion(descripcion);
        s.setCosto(costo);
        return servicioService.save(s);
    }

    private void createClinicasReales(List<Servicio> servicios) {
        // MUY IMPORTANTE: Si ya hay datos, no hace nada.
        // Para probar esto, debes borrar la colección 'clinicas' en tu MongoDB.
        if (clinicaRepository.count() > 0) {
            log.info("Clínicas ya existentes, omitiendo creación.");
            return;
        }

        List<String> todosLosIds = servicios.stream()
                .map(Servicio::getId)
                .collect(Collectors.toList());

        List<String[]> datos = List.of(
                new String[] {
                        "Clínica Veterinaria Mascotas 24 Horas",
                        "Calle 29 #18B-05, Manga",
                        "+57 300 7335890",
                        "mascotas24horas@clinicaapp.com",
                        "/img/clinica-mascotas-24h.jpg"
                },
                new String[] {
                        "LAKma Clínica Veterinaria 24 Horas",
                        "Diagonal 21 #47-75, El Bosque",
                        "+57 310 3625564",
                        "lakma@clinicaapp.com",
                        "/img/lakma-clinica-24h.jpg"
                },
                new String[] {
                        "Mascoclinica Clínica Veterinaria",
                        "Transversal 54 #27-161, Nuevo Bosque II Etapa",
                        "+57 605 6431008",
                        "mascoclinica@clinicaapp.com",
                        "/img/mascoclinica.jpg"
                },
                new String[] {
                        "Consultorio Veterinario Huellas y Colas",
                        "Diagonal 31 #78A-92, La Plazuela",
                        "+57 311 3974333",
                        "huellasycolas@clinicaapp.com",
                        "/img/huellas-y-colas.jpg"
                },
                new String[] {
                        "Clínica Veterinaria Crespo",
                        "Calle 70 #3-83, Crespo",
                        "+57 311 6731114",
                        "vetcrespo@clinicaapp.com",
                        "/img/clinica-crespo.png"
                },
                new String[] {
                        "Centro Veterinario Torices",
                        "Carrera 17 #51-37, Torices",
                        "+57 605 6786328",
                        "vetorices@clinicaapp.com",
                        "/img/centro-torices.png"
                },
                new String[] {
                        "BlueVet Center",
                        "Avenida Pedro de Heredia #50A-156",
                        "+57 300 6327400",
                        "bluevetcenter@clinicaapp.com",
                        "/img/bluevet-center.jpg"
                },
                new String[] {
                        "Clínica Veterinaria Vetzone",
                        "Transversal 51B #21C-101, Alto Bosque",
                        "+57 313 7090969",
                        "vetzone@clinicaapp.com",
                        "/img/clinica-vetzone.jpg"
                },
                new String[] {
                        "Centro Veterinario Canis",
                        "Transversal 54 #65-05 Local 03, Las Delicias",
                        "+57 300 8932414",
                        "centrocanis@clinicaapp.com",
                        "/img/centro-canis.jpg"
                },
                new String[] {
                        "Clínica Veterinaria Pet City Cartagena",
                        "Carrera 78 #29-03, Santa Monica",
                        "+57 605 6487158",
                        "petcity@clinicaapp.com",
                        "/img/pet-city-cartagena.png"
                },
                new String[] {
                        "Veterinaria K-Ninos",
                        "Calle 32 #71-45, Barrio 11 de Noviembre",
                        "+57 313 5832603",
                        "kninosvet@clinicaapp.com",
                        "/img/veterinaria-k-ninos.png"
                },
                new String[] {
                        "Veterinaria Garras y Patas",
                        "Calle 31 Diagonal 5 #21-18, Olaya Herrera",
                        "+57 315 1612981",
                        "garrasypatas@clinicaapp.com",
                        "/img/garras-y-patas.jpg"
                },
                new String[] {
                        "Consultorio Veterinario Cachorritos",
                        "Carrera 17 #43-12, Paseo Bolívar",
                        "+57 605 6581275",
                        "cachorritos@clinicaapp.com",
                        "/img/consultorio-cachorritos.jpg"
                },
                new String[] {
                        "Consultorio Veterinario Dr. Juan Carlos Salinas",
                        "Carrera 17 #34-210 Local 7, Torices",
                        "+57 605 6660722",
                        "drjcsalinas@clinicaapp.com",
                        "/img/dr-juan-carlos-salinas.png"
                },
                new String[] {
                        "Centro Veterinario La Mundial",
                        "Carrera 68 Manzana E Lote 10, El Carmelo",
                        "+57 304 6153969",
                        "lamundial@clinicaapp.com",
                        "/img/centro-la-mundial.jpg"
                });

        log.info("Verificando/Insertando clínicas...");

        // SI YA EXISTEN, VAMOS A ACTUALIZAR SUS IMÁGENES SI SON DE UNSPLASH
        if (clinicaRepository.count() > 0) {
            log.info("Clínicas ya existentes, verificando si necesitan actualizar sus fotos o personalización...");
            for (Clinica c : clinicaRepository.findAll()) {
                boolean modificado = false;
                
                // 1. Actualizar imagen si es de Unsplash (vieja)
                if (c.getImagenUrl() != null && c.getImagenUrl().contains("unsplash.com")) {
                    for (String[] d : datos) {
                        if (c.getNombre().equalsIgnoreCase(d[0])) {
                            c.setImagenUrl(d[4]);
                            modificado = true;
                            break;
                        }
                    }
                }
                
                // 2. Poblar/Actualizar personalización (Forzamos actualización para corregir fotos de animales)
                c.setEquipoMedico(generarEquipoMedicoAleatorio());
                c.setGaleriaFotos(generarGaleriaAleatoria());
                modificado = true;
                
                if (modificado) {
                    clinicaRepository.save(c);
                    log.info("Datos personalizados corregidos y actualizados para: {}", c.getNombre());
                }
            }
            return;
        }

        for (String[] d : datos) {
            String nombre = d[0];
            String direccion = d[1];
            String telefono = d[2];
            String email = d[3];
            String imagenUrl = d[4];

            // 1. Verificar si el usuario ya existe, si no, crearlo
            Usuario usuario = usuarioService.findByEmail(email);
            if (usuario == null) {
                UsuarioRegistroDTO userDTO = new UsuarioRegistroDTO(
                        nombre, "Sede Cartagena", email, "clinica123", telefono);
                usuarioService.createUsuarioWithRole(userDTO, Role.ROLE_CLINICA);
                // Lo buscamos de nuevo para obtener el ID generado por MongoDB
                usuario = usuarioService.findByEmail(email);
            }

            // 2. Crear la clínica vinculándola al usuario encontrado/creado
            Clinica c = new Clinica();
            c.setNombre(nombre);
            c.setDireccion(direccion);
            c.setTelefono(telefono);
            c.setEmail(email);
            c.setDescripcion("Clínica veterinaria profesional con acceso al sistema.");
            c.setServiciosOfrecidos(todosLosIds);
            c.setImagenUrl(imagenUrl);
            
            // --- PERSONALIZACIÓN ---
            c.setEquipoMedico(generarEquipoMedicoAleatorio());
            c.setGaleriaFotos(generarGaleriaAleatoria());
            // ------------------------

            c.setUsuarioAdminId(usuario.getId());
            c.setEstado(EstadoClinica.APROBADA);

            clinicaRepository.save(c);
        }

        log.info("✅ {} clínicas inicializadas correctamente.", datos.size());
    }

    private List<Map<String, String>> generarEquipoMedicoAleatorio() {
        List<Map<String, String>> equipo = new ArrayList<>();
        String[][] poolDoctores = {
            {"Dr. Ricardo Alzate", "Director Médico", "15 años de experiencia", "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=400&q=80"},
            {"Dra. Claudia Ortiz", "Cirujana Jefe", "12 años de experiencia", "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&q=80"},
            {"Dr. Sergio Méndez", "Especialista en Urgencias", "10 años de experiencia", "https://images.unsplash.com/photo-1537368910025-700350fe46c7?w=400&q=80"},
            {"Dra. Valentina Ríos", "Medicina Interna", "8 años de experiencia", "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=400&q=80"},
            {"Dr. Fernando Ruiz", "Traumatólogo", "11 años de experiencia", "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&q=80"},
            {"Dra. Isabella Santos", "Cardióloga", "7 años de experiencia", "https://images.unsplash.com/photo-1527613426441-4da17471b66d?w=400&q=80"},
            {"Dr. Javier Morales", "Dermatólogo", "9 años de experiencia", "https://images.unsplash.com/photo-1471017851983-fc49d89c57c2?w=400&q=80"},
            {"Dra. Beatriz Luna", "Oftalmóloga", "6 años de experiencia", "https://images.unsplash.com/photo-1504813184591-01592fd03cfd?w=400&q=80"},
            {"Dr. Andres Silva", "Anestesiólogo", "13 años de experiencia", "https://images.unsplash.com/photo-1537151625747-7ae77de5118b?w=400&q=80"}
        };

        List<Integer> indices = new ArrayList<>(IntStream.range(0, poolDoctores.length).boxed().collect(Collectors.toList()));
        Collections.shuffle(indices);

        for (int i = 0; i < 3; i++) {
            String[] d = poolDoctores[indices.get(i)];
            Map<String, String> doc = new HashMap<>();
            doc.put("nombre", d[0]);
            doc.put("especialidad", d[1]);
            doc.put("experiencia", d[2]);
            doc.put("fotoUrl", d[3]);
            equipo.add(doc);
        }
        return equipo;
    }

    private List<String> generarGaleriaAleatoria() {
        String[] poolFotos = {
            "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&q=80", // Sala de cirugía
            "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=800&q=80", // Instalación médica
            "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=800&q=80", // Consultorio vacio
            "https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=800&q=80", // Tecnología médica
            "https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?w=800&q=80", // Detalle médico
            "https://images.unsplash.com/photo-1597764690523-15bea4c581c9?w=800&q=80", // Sala de espera profesional
            "https://images.unsplash.com/photo-1579684385127-1ef15d508118?w=800&q=80", // Microscopio / Laboratorio
            "https://images.unsplash.com/photo-1504813184591-01592fd03cfd?w=800&q=80"  // Equipamiento
        };
        List<String> galeria = new ArrayList<>(Arrays.asList(poolFotos));
        Collections.shuffle(galeria);
        return galeria.subList(0, 4);
    }
}