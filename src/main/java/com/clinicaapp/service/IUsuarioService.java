package com.clinicaapp.service;

import com.clinicaapp.dto.UsuarioRegistroDTO;
import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;
import java.util.Optional;

import com.clinicaapp.model.Usuario;

public interface IUsuarioService extends UserDetailsService {
    // Guarda un nuevo usuario (generalmente con rol USER)
    Usuario save(UsuarioRegistroDTO registroDTO);

    // Crea un usuario con un rol específico (para ser usado por un admin)
    Usuario createUsuarioWithRole(UsuarioRegistroDTO registroDTO, Role role);

    Optional<Usuario> findById(String id);
    List<Usuario> findAllUsers();
    List<Usuario> findByRole(Role role);
    Usuario findByEmail(String email);

    // Actualiza el perfil de un usuario
    Usuario update(String id, UsuarioRegistroDTO usuarioDTO);
    Usuario update(String id, UsuarioRegistroDTO usuarioDTO, Role role);

    void deleteById(String id);

    void actualizarDatosContacto(String usuarioId, String nuevoEmail, String nuevoTelefono);

    void createPasswordResetTokenForUser(Usuario user, String token);
    Usuario validatePasswordResetToken(String token);
    void changeUserPassword(Usuario user, String newPassword);

    void saveExisting(Usuario usuario); 

    Usuario loginFacial(java.util.List<Double> descriptorActual);
    void registrarRostro(String usuarioId, List<Double> descriptor);

    org.springframework.data.domain.Page<Usuario> findPaginated(String keyword, Role role, org.springframework.data.domain.Pageable pageable);
}