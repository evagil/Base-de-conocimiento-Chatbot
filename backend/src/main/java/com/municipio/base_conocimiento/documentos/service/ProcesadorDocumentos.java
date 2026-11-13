package com.municipio.base_conocimiento.documentos.service;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.proceso.ExtractorAislado;
import com.municipio.base_conocimiento.documentos.worker.*;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;

@Service
public class ProcesadorDocumentos {
    private final ConfiguracionDocumentos configuracion;
    private final GestorTemporales temporales;
    private final ExtractorAislado extractor;
    private final MotorOcr ocr;
    private final Semaphore turno = new Semaphore(1);
    public ProcesadorDocumentos(ConfiguracionDocumentos configuracion, GestorTemporales temporales, ExtractorAislado extractor, MotorOcr ocr) {
        this.configuracion = configuracion; this.temporales = temporales; this.extractor = extractor; this.ocr = ocr;
    }
    public DocumentoExtraido extraer(MultipartFile archivo) {
        if (!configuracion.habilitada()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENTOS_DESHABILITADOS", "La extracción documental está deshabilitada.");
        String extension = validar(archivo);
        if (!turno.tryAcquire()) throw error(HttpStatus.TOO_MANY_REQUESTS, "DOCUMENTO_OCUPADO", "Se está procesando otro archivo. Intentá nuevamente en unos instantes.");
        long limite = System.nanoTime() + configuracion.timeout().toNanos();
        try (var trabajo = temporales.crear()) {
            Path carpeta = trabajo.carpeta();
            try (InputStream entrada = archivo.getInputStream(); OutputStream salida = new SalidaLimitada(
                    Files.newOutputStream(carpeta.resolve("entrada." + extension), StandardOpenOption.CREATE_NEW), configuracion.maxArchivoBytes(), "LIMITE_ARCHIVO")) {
                entrada.transferTo(salida);
            }
            var resultado = extractor.extraer(carpeta, extension, restante(limite));
            espacio(carpeta);
            if (resultado == null || resultado.partes() == null || resultado.partes().size() > configuracion.maxPaginas()) throw invalido();
            if (resultado.error() != null) throw traducir(resultado.error());
            if (resultado.paginas() != null && (resultado.paginas() < 0 || resultado.paginas() > configuracion.maxPaginas())) throw invalido();
            List<DocumentoExtraido.Seccion> secciones = new ArrayList<>();
            int caracteres = 0, paginasOcr = 0;
            for (var parte : resultado.partes()) {
                if (parte == null || parte.texto() == null || (parte.pagina() != null && (parte.pagina() < 1
                        || resultado.paginas() == null || parte.pagina() > resultado.paginas()))) throw invalido();
                String texto = parte.texto();
                String metodo = "TEXTO";
                if (parte.imagen() != null) {
                    if (++paginasOcr > configuracion.maxOcrPaginas()) throw traducir("LIMITE_OCR");
                    if (!parte.imagen().matches("ocr-[0-9]+\\.png")) throw invalido();
                    Path imagen = carpeta.resolve(parte.imagen()).normalize();
                    if (!imagen.getParent().equals(carpeta) || !Files.isRegularFile(imagen, LinkOption.NOFOLLOW_LINKS)) throw invalido();
                    texto = ocr.reconocer(imagen, carpeta, restante(limite));
                    espacio(carpeta);
                    Files.delete(imagen);
                    metodo = "OCR";
                }
                if (texto == null) throw invalido();
                texto = normalizar(texto);
                caracteres += texto.length();
                if (caracteres > configuracion.maxCaracteres()) throw traducir("LIMITE_CARACTERES");
                if (!texto.isBlank()) secciones.add(new DocumentoExtraido.Seccion(parte.pagina(), texto, metodo));
            }
            restante(limite);
            if (secciones.isEmpty()) throw error(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENTO_SIN_TEXTO", "No se encontró texto utilizable en el archivo.");
            String mime = extension.equals("pdf") ? "application/pdf" : extension.equals("docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "image/jpeg";
            return new DocumentoExtraido(archivo.getOriginalFilename(), mime, resultado.paginas(), List.copyOf(secciones), caracteres, paginasOcr > 0);
        } catch (LimiteDocumento e) { throw traducir(e.getMessage()); }
        catch (JacksonException e) { throw invalido(); }
        catch (IOException e) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENTO_ERROR_LOCAL", "No se pudo completar el procesamiento del archivo. Intentá nuevamente o consultá con administración."); }
        finally { turno.release(); }
    }
    private String validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "ARCHIVO_VACIO", "Seleccioná un archivo con contenido.");
        if (archivo.getSize() > configuracion.maxArchivoBytes()) throw traducir("LIMITE_ARCHIVO");
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.length() > 255 || nombre.lastIndexOf('.') <= 0
                || nombre.codePoints().anyMatch(Character::isISOControl) || nombre.matches(".*[\\\\/:*?\"<>|].*")) {
            throw error(HttpStatus.BAD_REQUEST, "NOMBRE_ARCHIVO_INVALIDO", "El nombre del archivo no es válido.");
        }
        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("pdf", "docx", "jpg", "jpeg").contains(extension)) throw traducir("FORMATO_INVALIDO");
        return extension;
    }
    private String normalizar(String texto) {
        StringBuilder limpio = new StringBuilder();
        texto.replace("\r\n", "\n").replace('\r', '\n').codePoints()
                .filter(c -> c == '\n' || c == '\t' || !Character.isISOControl(c)).forEach(limpio::appendCodePoint);
        return Normalizer.normalize(limpio.toString(), Normalizer.Form.NFC).strip();
    }
    private void espacio(Path carpeta) throws IOException {
        long total = 0;
        try (var archivos = Files.walk(carpeta)) {
            var entradas = archivos.limit(4097).toList();
            if (entradas.size() > 4096) throw traducir("LIMITE_TEMPORAL");
            for (Path archivo : entradas) {
                if (Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) total += Files.size(archivo);
                if (total > configuracion.maxTemporalBytes()) throw traducir("LIMITE_TEMPORAL");
            }
        }
    }
    private Duration restante(long limite) {
        long nanos = limite - System.nanoTime();
        if (nanos <= 0) throw error(HttpStatus.GATEWAY_TIMEOUT, "DOCUMENTO_TIMEOUT", "El archivo tardó demasiado en procesarse.");
        return Duration.ofNanos(nanos);
    }
    private DocumentoException traducir(String codigo) {
        return switch (codigo) {
            case "FORMATO_INVALIDO" -> error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, codigo, "El archivo debe ser PDF, DOCX o JPG/JPEG y coincidir con su contenido real.");
            case "DOCUMENTO_PROTEGIDO" -> error(HttpStatus.UNPROCESSABLE_ENTITY, codigo, "El PDF está protegido. Utilizá una copia sin cifrado que tengas permiso de consultar.");
            case "LIMITE_ARCHIVO", "LIMITE_PAGINAS", "LIMITE_OCR", "LIMITE_CARACTERES", "LIMITE_PIXELES", "LIMITE_TEMPORAL" ->
                error(HttpStatus.PAYLOAD_TOO_LARGE, codigo, "El archivo supera un límite de tamaño, páginas, imágenes o texto. Probá con una parte más pequeña.");
            default -> invalido();
        };
    }
    private DocumentoException invalido() { return error(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENTO_INVALIDO", "El archivo está dañado o no se pudo extraer su contenido de forma segura."); }
    private DocumentoException error(HttpStatus status, String codigo, String mensaje) { return new DocumentoException(status, codigo, mensaje); }
}
