package com.sportspace.service;

import com.sportspace.dto.request.LoginRequest;
import com.sportspace.dto.request.RegistroRequest;
import com.sportspace.dto.response.AuthResponse;
import com.sportspace.dto.response.UsuarioResponse;
import com.sportspace.entity.EstadoPropietario;
import com.sportspace.entity.Rol;
import com.sportspace.entity.Usuario;
import com.sportspace.exception.BadRequestException;
import com.sportspace.repository.UsuarioRepository;
import com.sportspace.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository     usuarioRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final VerificacionService   verificacionService;
    private final PreRegistroService    preRegistroService;
    private final SeguridadService      seguridadService;

    // LOGIN

    public AuthResponse login(LoginRequest request) {

        String ip = obtenerIpActual();

        // Verificar si la IP está bloqueada
        if (seguridadService.isIpBloqueada(ip)) {
            throw new BadRequestException(
                    "Tu dirección IP ha sido bloqueada por motivos de seguridad. " +
                            "Comunícate con el administrador.");
        }

        // ── FIX BUG: solo registrar intento fallido si el correo existe ──
        // Si el correo no existe, rechazar sin guardar nada en intentos fallidos.
        boolean correoExiste = usuarioRepository.existsByEmail(request.getEmail());
        if (!correoExiste) {
            throw new BadRequestException("Email o contraseña incorrectos");
        }

        // El correo existe → intentar autenticar; si falla es contraseña incorrecta
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            // Solo aquí registramos el intento fallido (correo real, clave incorrecta)
            seguridadService.registrarIntentoFallido(ip, request.getEmail());
            throw new BadRequestException("Email o contraseña incorrectos");
        }

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        // Cuenta bloqueada por seguridad
        if (Boolean.TRUE.equals(usuario.getBloqueadoPorSeguridad())) {
            throw new BadRequestException(
                    "Tu cuenta fue bloqueada por seguridad. " +
                            "Comunícate con soporte para reactivarla.");
        }

        // Cuenta pendiente / inactiva
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            if (usuario.getRol() == Rol.PROPIETARIO
                    && usuario.getEstadoPropietario() == EstadoPropietario.PENDIENTE) {
                throw new BadRequestException(
                        "Tu cuenta está pendiente de aprobación. El administrador la revisará pronto.");
            }
            throw new BadRequestException(
                    "Tu cuenta está inactiva. Comunícate con soporte en rondomnims9@gmail.com.");
        }

        // DOBLE ROL: si tiene CLIENTE y PROPIETARIO, pedir que elija
        boolean tieneCliente     = usuario.tieneRolCliente();
        boolean tienePropietario = usuario.tieneRolPropietario();

        if (tieneCliente && tienePropietario) {
            String preToken = jwtUtil.generatePreToken(usuario.getEmail());
            return AuthResponse.builder()
                    .requiereSeleccion(true)
                    .preToken(preToken)
                    .nombreMostrar(nombreMostrar(usuario))
                    .rolesDisponibles(java.util.List.of("CLIENTE", "PROPIETARIO"))
                    .build();
        }

        // Rol único → asegurar que "rol" (rol activo) sea consistente con el rol que tiene
        Rol rolEfectivo = tienePropietario ? Rol.PROPIETARIO
                : tieneCliente ? Rol.CLIENTE
                : usuario.getRol();
        if (usuario.getRol() != rolEfectivo) {
            usuario.setRol(rolEfectivo);
            usuario = usuarioRepository.save(usuario);
        }

        return construirRespuestaLogin(usuario, ip);
    }

    /**
     * Segundo paso del login cuando el usuario tiene doble rol: recibe el
     * preToken (5 min) generado en login() + el rol elegido, y entrega el
     * token completo de sesión para ese rol.
     */
    public AuthResponse seleccionarRol(com.sportspace.dto.request.SeleccionRolRequest request) {
        String preToken = request.getPreToken();

        if (!jwtUtil.isTokenValid(preToken) || !jwtUtil.isPreToken(preToken))
            throw new BadRequestException("Sesión de selección expirada. Vuelve a iniciar sesión.");

        String email = jwtUtil.extractEmail(preToken);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        Rol rolElegido;
        try {
            rolElegido = Rol.valueOf(request.getRol().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Rol inválido");
        }

        boolean puedeElegir = (rolElegido == Rol.CLIENTE && usuario.tieneRolCliente())
                || (rolElegido == Rol.PROPIETARIO && usuario.tieneRolPropietario());
        if (!puedeElegir)
            throw new BadRequestException("No tienes ese rol habilitado en tu cuenta");

        usuario.setRol(rolElegido);
        usuario = usuarioRepository.save(usuario);

        return construirRespuestaLogin(usuario, obtenerIpActual());
    }

    private AuthResponse construirRespuestaLogin(Usuario usuario, String ip) {
        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().name());

        seguridadService.registrarSesion(
                token,
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getEmail(),
                usuario.getRol().name(),
                ip
        );

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }

    private String nombreMostrar(Usuario u) {
        String primerNombre    = u.getNombres()   == null ? "" : u.getNombres().trim().split("\\s+")[0];
        String primerApellido  = u.getApellidos() == null ? "" : u.getApellidos().trim().split("\\s+")[0];
        return (primerNombre + " " + primerApellido).trim();
    }

    // REGISTRO

    @Transactional
    public UsuarioResponse registro(RegistroRequest request) {

        preRegistroService.verificarSoloEmailCompleto(request.getEmail());

        if (usuarioRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("El email ya está registrado");

        if (request.getNumeroDocumento() != null
                && usuarioRepository.existsByNumeroDocumento(request.getNumeroDocumento()))
            throw new BadRequestException("El número de documento ya está registrado");

        if (request.getTelefono() != null
                && usuarioRepository.existsByTelefono(request.getTelefono()))
            throw new BadRequestException("El teléfono ya está registrado");

        boolean activoInicial = (request.getRol() == Rol.CLIENTE);

        Usuario.UsuarioBuilder builder = Usuario.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .nacionalidad(request.getNacionalidad())
                .telefono(request.getTelefono())
                .rol(request.getRol())
                .esCliente(request.getRol() == Rol.CLIENTE)
                .esPropietario(request.getRol() == Rol.PROPIETARIO)
                .fechaNacimiento(request.getFechaNacimiento())
                .departamento(request.getDepartamento())
                .provincia(request.getProvincia())
                .distrito(request.getDistrito())
                .direccion(request.getDireccion())
                .ubigeo(request.getUbigeo())
                .emailVerificado(true)
                .telefonoVerificado(false)
                .activo(activoInicial);

        if (request.getRol() == Rol.PROPIETARIO) {
            builder.estadoPropietario(EstadoPropietario.PENDIENTE);
        }

        Usuario guardado = usuarioRepository.save(builder.build());

        try {
            preRegistroService.limpiarPorEmail(guardado.getEmail());
        } catch (Exception e) {
            log.warn("No se pudieron limpiar códigos preregistro para {}: {}",
                    guardado.getEmail(), e.getMessage());
        }

        return toResponse(guardado);
    }

    //  UTILIDADES

    private String obtenerIpActual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String ip = req.getRemoteAddr();
            // IPv6 localhost → normalizar a 127.0.0.1
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                return "127.0.0.1";
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .nombres(u.getNombres())
                .apellidos(u.getApellidos())
                .email(u.getEmail())
                .tipoDocumento(u.getTipoDocumento())
                .numeroDocumento(u.getNumeroDocumento())
                .nacionalidad(u.getNacionalidad())
                .telefono(u.getTelefono())
                .rol(u.getRol().name())
                .activo(u.getActivo())
                .fechaNacimiento(u.getFechaNacimiento())
                .departamento(u.getDepartamento())
                .provincia(u.getProvincia())
                .distrito(u.getDistrito())
                .direccion(u.getDireccion())
                .ubigeo(u.getUbigeo())
                .build();
    }
}