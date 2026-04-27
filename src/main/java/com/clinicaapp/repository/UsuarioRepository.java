package com.clinicaapp.repository;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    // Buscar un usuario por su email (útil para login)
    Usuario findByEmail(String email);

    // Buscar todos los usuarios con un rol específico (Paginado)
    Page<Usuario> findByRole(Role role, Pageable pageable);

    // Buscar usuarios por nombre, apellido o email (Paginado)
    Page<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String nombre, String apellido, String email, Pageable pageable);

    // Búsqueda combinada: Rol + Keyword (Paginado)
    Page<Usuario> findByRoleAndNombreContainingIgnoreCaseOrRoleAndApellidoContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            Role role1, String nombre, Role role2, String apellido, Role role3, String email, Pageable pageable);

    List<Usuario> findByRole(Role role);

    // Buscar usuarios por nombre o apellido (ignorando mayúsculas/minúsculas)
    List<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(String nombre, String apellido);

    Usuario findByResetPasswordToken(String token);

    List<Usuario> findByFacialLoginHabilitadoTrue();
}