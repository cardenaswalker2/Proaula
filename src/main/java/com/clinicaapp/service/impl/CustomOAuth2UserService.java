package com.clinicaapp.service.impl;

import com.clinicaapp.model.Usuario;
import com.clinicaapp.model.enums.Role;
import com.clinicaapp.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());

        log.debug("loadUser: clientRegistration={}, attributes={}", userRequest.getClientRegistration().getRegistrationId(), attributes);

        // 1. Procesar al usuario (buscarlo o crearlo en nuestra BD)
        Usuario usuarioInterno = processOAuth2User(attributes);
        if (usuarioInterno == null) {
            log.warn("processOAuth2User returned null for attributes={}", attributes);
            // Avoid returning null principal: create a minimal placeholder user to avoid NPEs
            Usuario placeholder = new Usuario();
            placeholder.setId("unknown");
            placeholder.setEmail((String) attributes.getOrDefault("email", "unknown@unknown"));
            placeholder.setNombre((String) attributes.getOrDefault("given_name", "Usuario"));
            placeholder.setApellido((String) attributes.getOrDefault("family_name", ""));
            placeholder.setRole(Role.ROLE_USER);
            usuarioInterno = placeholder;
        }

        // 2. Añadir información personalizada a los atributos de la sesión
        attributes.put("internal_user_id", usuarioInterno.getId());
        attributes.put("is_new_user", usuarioInterno.getTelefono() == null); // Asumimos que un usuario es nuevo si no ha completado su teléfono

        // 3. Crear el conjunto de autoridades (roles) de nuestra aplicación
    Set<GrantedAuthority> authorities = new HashSet<>();
    // Ensure authority uses ROLE_ prefix as Spring expects
    String roleName = usuarioInterno.getRole() != null ? usuarioInterno.getRole().name() : Role.ROLE_USER.name();
    if (!roleName.startsWith("ROLE_")) roleName = "ROLE_" + roleName;
    authorities.add(new SimpleGrantedAuthority(roleName));

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 4. Devolver un nuevo Principal de seguridad que incluye nuestros roles y atributos
        return new DefaultOAuth2User(authorities, attributes, userNameAttributeName);
    }

    private Usuario processOAuth2User(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            log.info("Creando nuevo usuario desde OAuth2: {}", email);
            
            String name = (String) attributes.get("name");
            if (name == null || name.isBlank()) name = (String) attributes.get("login");
            
            String[] nameParts = name.split("\\s+");
            String nombre = nameParts[0];
            String apellido = (nameParts.length > 1) ? nameParts[nameParts.length - 1] : "";

            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setRole(Role.ROLE_USER);
            usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Contraseña aleatoria
            // Campos como 'telefono' se dejan nulos para que los complete en el "onboarding"
            
            return usuarioRepository.save(usuario);
        }
        log.info("Usuario existente ha iniciado sesión vía OAuth2: {}", email);
        return usuario;
    }
}