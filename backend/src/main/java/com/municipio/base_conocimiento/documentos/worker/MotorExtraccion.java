package com.municipio.base_conocimiento.documentos.worker;

import com.municipio.base_conocimiento.archivos.service.*;
import com.municipio.base_conocimiento.documentos.proceso.ProtocoloWorker.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class MotorExtraccion {
    public Resultado extraer(Path carpeta, Limites limites) throws IOException {
        Path entrada = carpeta.resolve("entrada." + limites.extension());
        if (limites.extension().equals("pdf") || limites.extension().equals("docx")) {
            new DetectorDocumento().detectar(Files.readAllBytes(entrada), entrada.getFileName().toString());
        }
        return switch (limites.extension()) {
            case "pdf" -> pdf(entrada, carpeta, limites);
            case "docx" -> docx(entrada, limites);
            case "jpg", "jpeg" -> imagen(entrada, carpeta, limites);
            default -> throw new IOException("Formato no admitido.");
        };
    }
    private Resultado docx(Path entrada, Limites limites) throws IOException {
        ZipSecureFile.setMaxEntrySize(10L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
        ZipSecureFile.setMinInflateRatio(0.01);
        try (InputStream datos = Files.newInputStream(entrada); XWPFDocument documento = new XWPFDocument(datos);
             XWPFWordExtractor extractor = new XWPFWordExtractor(documento)) {
            String texto = extractor.getText();
            comprobarTexto(texto, limites.maxCaracteres());
            return new Resultado(null, List.of(new Parte(null, texto, null)), null);
        }
    }
    private Resultado pdf(Path entrada, Path carpeta, Limites limites) throws IOException {
        try (PDDocument documento = Loader.loadPDF(entrada.toFile())) {
            if (documento.isEncrypted()) throw new LimiteDocumento("DOCUMENTO_PROTEGIDO");
            if (documento.getNumberOfPages() > limites.maxPaginas()) throw new LimiteDocumento("LIMITE_PAGINAS");
            List<Parte> partes = new ArrayList<>();
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setSortByPosition(true);
            PDFRenderer renderer = new PDFRenderer(documento);
            int caracteres = 0, ocr = 0;
            for (int pagina = 1; pagina <= documento.getNumberOfPages(); pagina++) {
                extractor.setStartPage(pagina); extractor.setEndPage(pagina);
                String texto = extractor.getText(documento);
                caracteres += texto.length();
                comprobarTexto(texto, limites.maxCaracteres());
                if (caracteres > limites.maxCaracteres()) throw new LimiteDocumento("LIMITE_CARACTERES");
                if (!texto.isBlank()) partes.add(new Parte(pagina, texto, null));
                else if (documento.getPage(pagina - 1).hasContents()) {
                    if (++ocr > limites.maxOcrPaginas()) throw new LimiteDocumento("LIMITE_OCR");
                    var caja = documento.getPage(pagina - 1).getCropBox();
                    double ancho = Math.ceil(caja.getWidth() * limites.dpi() / 72.0);
                    double alto = Math.ceil(caja.getHeight() * limites.dpi() / 72.0);
                    if (!Double.isFinite(ancho * alto) || ancho <= 0 || alto <= 0 || ancho * alto > limites.maxPixeles()) throw new LimiteDocumento("LIMITE_PIXELES");
                    BufferedImage imagen = renderer.renderImageWithDPI(pagina - 1, limites.dpi(), ImageType.RGB);
                    String nombre = "ocr-" + pagina + ".png";
                    escribirImagen(imagen, carpeta.resolve(nombre), carpeta, limites);
                    partes.add(new Parte(pagina, "", nombre));
                }
            }
            return new Resultado(documento.getNumberOfPages(), partes, null);
        }
    }
    private Resultado imagen(Path entrada, Path carpeta, Limites limites) throws IOException {
        try (var datos = new FileImageInputStream(entrada.toFile())) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(datos);
            if (!lectores.hasNext()) throw new IOException("Imagen inválida.");
            ImageReader lector = lectores.next();
            try {
                lector.setInput(datos, true, true);
                if (!lector.getFormatName().equalsIgnoreCase("JPEG")) throw new IOException("Formato incorrecto.");
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles <= 0 || pixeles > limites.maxPixeles()) throw new LimiteDocumento("LIMITE_PIXELES");
                if (!new DetectorImagen().detectar(Files.readAllBytes(entrada)).mime().equals("image/jpeg")) throw new IOException("Imagen inválida.");
                BufferedImage imagen = lector.read(0);
                escribirImagen(imagen, carpeta.resolve("ocr-1.png"), carpeta, limites);
                return new Resultado(1, List.of(new Parte(1, "", "ocr-1.png")), null);
            } finally { lector.dispose(); }
        }
    }
    private void escribirImagen(BufferedImage imagen, Path salida, Path carpeta, Limites limites) throws IOException {
        long usados = 0;
        try (var archivos = Files.list(carpeta)) {
            for (Path archivo : archivos.toList()) usados += Files.size(archivo);
        }
        long disponible = limites.maxTemporalBytes() - usados - limites.maxCaracteres() * 8L - 8192;
        try (var bytes = new SalidaLimitada(Files.newOutputStream(salida, StandardOpenOption.CREATE_NEW), disponible, "LIMITE_TEMPORAL");
             var datos = new MemoryCacheImageOutputStream(bytes)) {
            ImageIO.write(imagen, "png", datos);
        }
    }
    private void comprobarTexto(String texto, int maximo) {
        if (texto.length() > maximo) throw new LimiteDocumento("LIMITE_CARACTERES");
    }
}
