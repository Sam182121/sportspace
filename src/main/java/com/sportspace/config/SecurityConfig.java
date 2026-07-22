package com.sportspace.config;

import com.sportspace.security.JwtAuthFilter;
import com.sportspace.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Archivos estáticos y carpetas completas
                        .requestMatchers(
                                "/auth/**",
                                "/admin/**",
                                "/propietario/**",
                                "/cliente/**",
                                "/shared.css",
                                "/shared/**",
                                "/api.js",
                                "/*.ico",
                                "/*.png",
                                "/cloudinary.js"


                        ).permitAll()

                        // URLs limpias — publicas
                        .requestMatchers(
				"/",
                                "/login",
                                "/crear-cuenta",
                                "/recuperar-contrasena",
                                "/reset-password",
                                "/bloquear-cuenta"
                        ).permitAll()

                        // URLs limpias — panel admin
                        .requestMatchers(
                                "/admin/dashboard",
                                "/admin/usuarios",
                                "/admin/canchas",
                                "/admin/reservas",
                                "/admin/propietarios",
                                "/admin/pagos",
                                "/admin/reportes",
                                "/admin/estadisticas",
                                "/admin/seguridad",
                                "/admin/configuracion"
                        ).permitAll()

                        // URLs limpias — panel propietario
                        .requestMatchers(
                                "/propietario/dashboard",
                                "/propietario/canchas",
                                "/propietario/reservas",
                                "/propietario/horarios",
                                "/propietario/clientes",
                                "/propietario/pagos"
                        ).permitAll()

                        // URLs limpias — panel cliente
                        .requestMatchers(
                                "/cliente/dashboard",
                                "/cliente/buscar",
                                "/cliente/reservas"
                        ).permitAll()

                        // Verificación de email
                        .requestMatchers(
                                "/api/auth/verificar-email",
                                "/api/auth/reenviar-codigo-email"
                        ).permitAll()

                        // API publica (sin token)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/reniec/**").permitAll()
                        .requestMatchers("/api/ubigeo/**").permitAll()
                        .requestMatchers("/api/nacionalidades/**").permitAll()
                        .requestMatchers("/api/canchas/publico/**").permitAll()
                        .requestMatchers("/api/usuarios/publico/**").permitAll()

                        // API protegida por rol
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/propietario/**").hasRole("PROPIETARIO")
                        .requestMatchers("/api/cliente/**").hasRole("CLIENTE")

                        // Modulo pagos
                        // El @PreAuthorize en cada endpoint ya controla el rol.
                        // Aqui solo indicamos que necesitan autenticacion.
                        .requestMatchers("/api/pagos/**").authenticated()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        // Se agrega PATCH para los endpoints de confirmar/rechazar/reembolso
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}