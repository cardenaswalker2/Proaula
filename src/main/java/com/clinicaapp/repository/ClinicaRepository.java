package com.clinicaapp.repository;

import com.clinicaapp.model.Clinica;
import com.clinicaapp.model.enums.EstadoClinica; // <-- IMPORTANTE: Faltaba este import
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicaRepository extends MongoRepository<Clinica, String> {

    // --- 1. LO QUE TE FALTABA PARA EL FLUJO DE APROBACIÓN ---

    // Para que el Admin vea la lista de solicitudes pendientes
    List<Clinica> findByEstado(EstadoClinica estado);

    // Para buscar una clínica por el ID del usuario dueño
    Clinica findByUsuarioAdminId(String usuarioAdminId);

    // Para verificar si un email de clínica ya existe
    Clinica findByEmail(String email);

    // --- 2. MÉTODOS DE BÚSQUEDA EXISTENTES (MANTENEMOS ESTOS) ---

    // Búsqueda paginada por nombre o dirección
    Page<Clinica> findByNombreContainingIgnoreCaseOrDireccionContainingIgnoreCase(String nombre, String direccion,
            Pageable pageable);

    // Búsqueda paginada solo por nombre
    Page<Clinica> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Búsqueda por servicios
    List<Clinica> findByServiciosOfrecidosContaining(String servicioId);

    // Búsqueda simple por nombre (lista)
    List<Clinica> findByNombreContainingIgnoreCase(String nombre);

    // --- 3. BÚSQUEDA PÚBLICA (SOLO APROBADAS) ---
    
    Page<Clinica> findByEstado(EstadoClinica estado, Pageable pageable);

    @Query("{ 'estado': ?0, '$or': [ { 'nombre': { '$regex': ?1, '$options': 'i' } }, { 'direccion': { '$regex': ?1, '$options': 'i' } }, { 'descripcion': { '$regex': ?1, '$options': 'i' } } ] }")
    Page<Clinica> findPublic(EstadoClinica estado, String query, Pageable pageable);

    @Query("{ 'estado': ?0, '$or': [ { 'nombre': { '$regex': ?1, '$options': 'i' } }, { 'direccion': { '$regex': ?1, '$options': 'i' } }, { 'descripcion': { '$regex': ?1, '$options': 'i' } }, { 'serviciosOfrecidos': { '$in': ?2 } } ] }")
    Page<Clinica> findPublicWithServices(EstadoClinica estado, String query, List<String> servicioIds, Pageable pageable);

    Page<Clinica> findByEstadoAndServiciosOfrecidosIn(EstadoClinica estado, List<String> servicioIds, Pageable pageable);
}