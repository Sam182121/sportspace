package com.sportspace.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, String rol) { //rea el token con email, rol, fecha de emision y expiración firmado con clave secreta
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * Token temporal (5 min) usado SOLO para la pantalla "¿Cómo deseas ingresar?"
     * cuando el usuario tiene doble rol. No sirve para llamar a la API protegida.
     */
    public String generatePreToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("preAuth", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                .signWith(getKey())
                .compact();
    }

    public boolean isPreToken(String token) {
        try {
            return Boolean.TRUE.equals(getClaims(token).get("preAuth"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) { // lee datos del token
        return getClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return (String) getClaims(token).get("rol");
    }

    public boolean isTokenValid(String token) { // verifica y expiracion
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}