package com.clinicaapp.config;

import com.clinicaapp.service.IUsuarioService;
import com.clinicaapp.service.impl.CustomOAuth2UserService;
import com.clinicaapp.service.impl.CustomOidcUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private IUsuarioService usuarioService;
    @Autowired
    private CustomAuthenticationSuccessHandler formLoginSuccessHandler;
    @Autowired
    private CustomOAuth2SuccessHandler oauth2LoginSuccessHandler;
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    private CustomOidcUserService customOidcUserService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(usuarioService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Mantener deshabilitado para que los fetch de JS funcionen sin problemas

                .authorizeHttpRequests(auth -> auth
                        // --- 1. RUTAS PÚBLICAS CRÍTICAS ---
                        .requestMatchers("/clinica/registrar-clinica/**").permitAll()

                        // Recursos estáticos y páginas principales
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/index",
                                "/buscar-clinicas", // Ruta exacta
                                "/buscar-clinicas/**", // Incluye subrutas y parámetros de búsqueda
                                "/detalle-clinica/**",
                                "/pago-exitoso",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/webjars/**",
                                "/api/**",
                                "/models/**",
                                "/forgot-password",
                                "/reset-password",
                                "/login",
                                "/registro",
                                "/error",
                                "/clinica/registro-exitoso",
                                "/publico/**",
                                "/uploads/**"
                            )
                        .permitAll()

                        // --- NUEVO: PERMITIR LOGIN FACIAL (Esto es lo que faltaba) ---
                        .requestMatchers(HttpMethod.POST,
                                "/registro",
                                "/forgot-password",
                                "/reset-password",
                                "/auth/login-facial" // <-- Permitir el POST del login facial
                        ).permitAll()

                        // Permitir que el sistema de IA cargue modelos si están en tu servidor local
                        .requestMatchers("/models/**").permitAll()

                        // --- 2. RUTAS PROTEGIDAS ---

                        // Registro de rostro (Debe estar logueado para registrar su cara)
                        .requestMatchers("/auth/registrar-rostro").authenticated()

                        // Administración Global
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                        // Recepción
                        .requestMatchers("/recepcion/**").hasAuthority("ROLE_RECEPCIONISTA")

                        // Clínicas
                        .requestMatchers("/clinica/**").hasAuthority("ROLE_CLINICA")

                        // Usuarios Clientes
                        .requestMatchers("/usuario/**").authenticated()

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(formLoginSuccessHandler)
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .successHandler(oauth2LoginSuccessHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll())
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?expired"));

        return http.build();
    }
}