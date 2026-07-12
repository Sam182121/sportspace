package com.sportspace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

	registry.addRedirectViewController("/", "/login");

        // Auth
        registry.addViewController("/login")
                .setViewName("forward:/auth/login/login.html");
        registry.addViewController("/crear-cuenta")
                .setViewName("forward:/auth/crear-cuenta/crear-cuenta.html");
        registry.addViewController("/recuperar-contrasena")
                .setViewName("forward:/auth/recuperar-contrasena/recuperar-contrasena.html");
        registry.addViewController("/reset-password")
                .setViewName("forward:/auth/reset-password/reset-password.html");
        // Bloqueo de cuenta — llega desde el correo de seguridad tras cambiar contraseña
        registry.addViewController("/bloquear-cuenta")
                .setViewName("forward:/auth/bloquear-cuenta/bloquear-cuenta.html");

        // Panel admin
        registry.addViewController("/admin/dashboard")
                .setViewName("forward:/admin/dashboard/dashboard.html");
        registry.addViewController("/admin/usuarios")
                .setViewName("forward:/admin/usuarios/usuarios.html");
        registry.addViewController("/admin/canchas")
                .setViewName("forward:/admin/canchas/canchas.html");
        registry.addViewController("/admin/reservas")
                .setViewName("forward:/admin/reservas/reservas.html");
        registry.addViewController("/admin/propietarios")
                .setViewName("forward:/admin/propietarios/propietarios.html");
        registry.addViewController("/admin/pagos")
                .setViewName("forward:/admin/pagos/pagos.html");
        registry.addViewController("/admin/reportes")
                .setViewName("forward:/admin/reportes/reportes.html");
        registry.addViewController("/admin/estadisticas")
                .setViewName("forward:/admin/estadisticas/estadisticas.html");
        registry.addViewController("/admin/seguridad")
                .setViewName("forward:/admin/seguridad/seguridad.html");
        registry.addViewController("/admin/configuracion")
                .setViewName("forward:/admin/configuracion/configuracion.html");

        // Panel propietario
        registry.addViewController("/propietario/dashboard")
                .setViewName("forward:/propietario/dashboard/dashboard.html");
        registry.addViewController("/propietario/canchas")
                .setViewName("forward:/propietario/canchas/canchas.html");
        registry.addViewController("/propietario/reservas")
                .setViewName("forward:/propietario/reservas/reservas.html");
        registry.addViewController("/propietario/horarios")
                .setViewName("forward:/propietario/horarios/horarios.html");
        registry.addViewController("/propietario/clientes")
                .setViewName("forward:/propietario/clientes/clientes.html");
        registry.addViewController("/propietario/pagos")
                .setViewName("forward:/propietario/pagos/pagos.html");

        // Panel cliente
        registry.addViewController("/cliente/dashboard")
                .setViewName("forward:/cliente/dashboard/dashboard.html");
        registry.addViewController("/cliente/buscar")
                .setViewName("forward:/cliente/buscar/buscar.html");
        registry.addViewController("/cliente/reservas")
                .setViewName("forward:/cliente/reservas/reservas.html");
    }
}