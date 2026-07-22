package com.sportspace.service;

import com.sportspace.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Guarda archivos (fotos de canchas, vouchers de pago) en el DISCO del servidor
 * En la base de datos solo se guarda la URL/ruta relativa del archivo
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> TIPOS_IMAGEN = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    /** Guarda una foto de cancha. Devuelve la URL pública ("/uploads/canchas/xxx.jpg"). */
    public String guardarFotoCancha(MultipartFile file) {
        return guardar(file, "canchas", TIPOS_IMAGEN);
    }

    /** Guarda un voucher de pago SOLO imágenes. */
    public String guardarVoucher(MultipartFile file) {
        return guardar(file, "vouchers", TIPOS_IMAGEN);
    }

    private String guardar(MultipartFile file, String subcarpeta, List<String> tiposPermitidos) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("El archivo está vacío");

        if (file.getSize() > MAX_SIZE)
            throw new BadRequestException("El archivo supera el tamaño máximo de 5 MB");

        String contentType = file.getContentType();
        if (contentType == null || !tiposPermitidos.contains(contentType))
            throw new BadRequestException("Solo se permiten imágenes (JPG, PNG o WEBP). PDF, videos u otros archivos no están permitidos.");

        try {
            Path carpeta = Paths.get(uploadDir, subcarpeta).toAbsolutePath().normalize();
            Files.createDirectories(carpeta);

            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String nombreArchivo = UUID.randomUUID() + extension;

            Path destino = carpeta.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + subcarpeta + "/" + nombreArchivo;
        } catch (IOException e) {
            throw new BadRequestException("Error al guardar el archivo: " + e.getMessage());
        }
    }

    /** Elimina un archivo físico a partir de su URL pública */
    public void eliminar(String url) {
        if (url == null || url.isBlank() || !url.startsWith("/uploads/")) return;
        try {
            Path ruta = Paths.get(uploadDir, url.substring("/uploads/".length())).toAbsolutePath().normalize();
            Files.deleteIfExists(ruta);
        } catch (IOException ignored) {
            // no interrumpe el flujo si no se pudo borrar el archivo físico
        }
    }
}