package com.clinicaapp.repository;

import com.clinicaapp.model.Cita;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends MongoRepository<Cita, String> {
    
    // Búsquedas por IDs de relación
    List<Cita> findByUsuarioId(String userId);
    List<Cita> findByClinicaId(String clinicaId);
    List<Cita> findByMascotaId(String mascotaId);
    
    
    // Búsquedas por estados
    List<Cita> findByEstado(String estado);
    List<Cita> findByUsuarioIdAndEstado(String userId, String estado);
    List<Cita> findByEstadoAndEstadoPago(String estado, String estadoPago);
    List<Cita> findByEstadoInAndEstadoPago(List<String> estados, String estadoPago);

    // Búsqueda para pasarela de pagos (Stripe)
    Optional<Cita> findByPaymentIntentId(String paymentIntentId);

    List<Cita> findByFechaHoraBetweenAndEstadoAndEstadoPago(
    LocalDateTime inicio, LocalDateTime fin, String estado, String estadoPago

    
);

    // NOTA: Se eliminó 'findByClinicaEmail' porque causa error al no existir 
    // esa propiedad en el modelo Cita. El filtrado por email se hace en el Service.
}