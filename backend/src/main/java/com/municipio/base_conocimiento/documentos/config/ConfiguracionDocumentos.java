package com.municipio.base_conocimiento.documentos.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.documentos")
public record ConfiguracionDocumentos(boolean habilitada, Path temporales, String tesseract,
        String tessdata, String idiomas, Duration timeout, long maxArchivoBytes,
        int maxPaginas, int maxOcrPaginas, int maxCaracteres, long maxPixeles,
        long maxTemporalBytes, int dpi) {
    public ConfiguracionDocumentos {
        if (temporales == null || tesseract == null || tesseract.isBlank() || tesseract.length() > 512
                || tessdata == null || idiomas == null || !idiomas.matches("[a-z]{3}(\\+[a-z]{3}){0,2}")
                || timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofSeconds(120)) > 0
                || maxArchivoBytes < 1024 || maxArchivoBytes > 10485760
                || maxPaginas < 1 || maxPaginas > 50 || maxOcrPaginas < 1 || maxOcrPaginas > maxPaginas
                || maxCaracteres < 256 || maxCaracteres > 200000 || maxPixeles < 10000 || maxPixeles > 12000000
                || maxTemporalBytes < maxArchivoBytes + maxCaracteres * 8L + 8192 || maxTemporalBytes > 134217728
                || dpi < 72 || dpi > 200) {
            throw new IllegalArgumentException("La configuración de extracción documental contiene límites o valores inválidos.");
        }
        temporales = temporales.toAbsolutePath().normalize();
        if (temporales.getParent() == null) throw new IllegalArgumentException("La carpeta temporal debe ser específica de la aplicación.");
    }
}
