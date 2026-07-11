package com.sportspace.service;

import com.sportspace.dto.response.FactilizaResponse;
import com.sportspace.dto.response.ReniecResponse;
import com.sportspace.exception.BadRequestException;
import com.sportspace.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReniecService {

    private final RestTemplate restTemplate;


    @Value("${factiliza.api.url}")
    private String apiUrl;

    @Value("${factiliza.api.token}")
    private String apiToken;

    // CONSULTA DNI
    public ReniecResponse consultarPorDni(String dni) {
        if (dni == null || !dni.matches("\\d{8}")) {
            throw new BadRequestException("El DNI debe tener exactamente 8 dígitos numéricos");
        }

        log.info("Consultando Factiliza DNI: {}", dni);

        FactilizaResponse resp = llamarFactiliza("/dni/info/" + dni, "DNI");

        FactilizaResponse.DataFactiliza d = resp.getData();

        // Armar apellidos completos paterno + materno
        String apellidos = joinNonBlank(d.getApellidoPaterno(), d.getApellidoMaterno());

        return ReniecResponse.builder()
                .numeroDocumento(d.getNumero())
                .tipoDocumento("DNI")
                .nombres(d.getNombres())
                .apellidos(apellidos)
                .nombreCompleto(d.getNombreCompleto())
                .fechaNacimiento(nullIfBlank(d.getFechaNacimiento()))
                .departamento(nullIfBlank(d.getDepartamento()))
                .provincia(nullIfBlank(d.getProvincia()))
                .distrito(nullIfBlank(d.getDistrito()))
                .direccion(nullIfBlank(d.getDireccion()))
                .ubigeo(nullIfBlank(d.getUbigeoReniec()))
                // Para peruanos la nacionalidad siempre es PERUANA
                .nacionalidad("PERUANA")
                .build();
    }

    // CONSULTA CE
    public ReniecResponse consultarPorCe(String ce) {
        if (ce == null || ce.isBlank()) {
            throw new BadRequestException("Ingresa el número de Carnet de Extranjería");
        }
        // El C.E. puede tener letras y números, sin longitud fija
        if (ce.length() < 5 || ce.length() > 12) {
            throw new BadRequestException("El Carnet de Extranjería debe tener entre 5 y 12 caracteres");
        }

        log.info("Consultando Factiliza C.E.: {}", ce);

        FactilizaResponse resp = llamarFactiliza("/cee/info/" + ce, "C.E.");

        FactilizaResponse.DataFactiliza d = resp.getData();

        String apellidos = joinNonBlank(d.getApellidoPaterno(), d.getApellidoMaterno());

        // Nombre completo: Factiliza C.E. no siempre lo devuelve, lo armamos nosotros
        String nombreCompleto = (apellidos + " " + (d.getNombres() != null ? d.getNombres() : "")).trim();

        return ReniecResponse.builder()
                .numeroDocumento(d.getNumero())
                .tipoDocumento("CE")
                .nombres(d.getNombres())
                .apellidos(apellidos)
                .nombreCompleto(nombreCompleto)
                // C.E. nunca devuelve estos campos → el frontend muestra los controles editables
                .fechaNacimiento(null)
                .departamento(null)
                .provincia(null)
                .distrito(null)
                .direccion(null)
                .ubigeo(null)
                // Nacionalidad: el usuario la ingresa manualmente (es extranjero)
                .nacionalidad(null)
                .build();
    }

    // LLAMA API Y BUSCA ERRORES DE API
    private FactilizaResponse llamarFactiliza(String path, String tipoDoc) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken); // Authorization: Bearer <token>
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<FactilizaResponse> response = restTemplate.exchange(
                    apiUrl + path,
                    HttpMethod.GET,
                    entity,
                    FactilizaResponse.class
            );

            FactilizaResponse body = response.getBody();

            if (body == null || body.getData() == null) {
                throw new ResourceNotFoundException(
                        tipoDoc + " no encontrado. Verifica el número ingresado.");
            }

            log.info("Factiliza OK → {}", body.getData().getNombreCompleto());
            return body;

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(
                    tipoDoc + " no encontrado. Verifica el número ingresado.");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Token de Factiliza inválido");
            throw new BadRequestException("Error de autenticación con la API. Contacta al administrador.");
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error Factiliza: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            throw new BadRequestException("Error al consultar la API. Intenta nuevamente.");
        }
    }


    // Devuelve null si el string está vacío o en blanco
    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // Une dos strings con espacio, ignorando los que están vacíos
    private String joinNonBlank(String a, String b) {
        String sa = (a != null ? a.trim() : "");
        String sb = (b != null ? b.trim() : "");
        if (sa.isEmpty()) return sb;
        if (sb.isEmpty()) return sa;
        return sa + " " + sb;
    }
}