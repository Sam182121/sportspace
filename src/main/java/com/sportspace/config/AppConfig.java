package com.sportspace.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // LLAMADA EXTERNA
        RestTemplate restTemplate = new RestTemplate();

        // JACSON LIBRERIA CAMBIA TEXTO JSON A OBJETO JAVA , VICEVERSA
        // PARA EVITAR LANZE ERROR CUANDO SPRING RECIBE TEXTO PLANO
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();

        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_OCTET_STREAM, // DATOS
                MediaType.TEXT_PLAIN, // TXT SIMPLE
                MediaType.TEXT_HTML, // WEBS
                new MediaType("application", "*"),
                MediaType.ALL
        ));

        // Limpiar conversores existentes y agregar el nuevo primero
        restTemplate.getMessageConverters().clear();
        restTemplate.getMessageConverters().add(converter);

        // DEVUELVE OBJETO CONFIGURADO PARA SPRING GUARDA PARA USARLO
        return restTemplate;
    }
}