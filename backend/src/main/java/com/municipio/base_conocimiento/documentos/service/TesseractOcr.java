package com.municipio.base_conocimiento.documentos.service;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.proceso.EjecutorProcesos;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TesseractOcr implements MotorOcr {
    private final ConfiguracionDocumentos configuracion;
    private final EjecutorProcesos procesos;
    public TesseractOcr(ConfiguracionDocumentos configuracion, EjecutorProcesos procesos) {
        this.configuracion = configuracion; this.procesos = procesos;
    }
    @Override public String reconocer(Path imagen, Path carpeta, Duration timeout) throws IOException {
        Path base = carpeta.resolve("texto-" + UUID.randomUUID());
        List<String> comando = new ArrayList<>(List.of(configuracion.tesseract(), imagen.toString(), base.toString(),
                "-l", configuracion.idiomas(), "--psm", "3"));
        if (!configuracion.tessdata().isBlank()) comando.addAll(List.of("--tessdata-dir", configuracion.tessdata()));
        int codigo;
        try { codigo = procesos.ejecutar(comando, carpeta, timeout); }
        catch (DocumentoException e) {
            if (e.getCodigo().equals("PROCESADOR_NO_DISPONIBLE")) throw noDisponible();
            throw e;
        }
        if (codigo != 0) throw noDisponible();
        Path salida = Path.of(base + ".txt");
        if (!Files.isRegularFile(salida, LinkOption.NOFOLLOW_LINKS)) throw noDisponible();
        if (Files.size(salida) > configuracion.maxCaracteres() * 4L + 8192) {
            throw new DocumentoException(HttpStatus.PAYLOAD_TOO_LARGE, "LIMITE_CARACTERES", "El texto reconocido supera el límite permitido.");
        }
        return Files.readString(salida, StandardCharsets.UTF_8);
    }
    private DocumentoException noDisponible() {
        return new DocumentoException(HttpStatus.SERVICE_UNAVAILABLE, "OCR_NO_DISPONIBLE", "No se pudo utilizar el OCR local. Revisá Tesseract y los idiomas configurados.");
    }
}
