package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.documentos.proceso.ProtocoloWorker.*;
import com.municipio.base_conocimiento.documentos.worker.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class MotorExtraccionTest {
    @TempDir Path raiz;
    final ObjectMapper json = new ObjectMapper();
    Limites limites(String extension) { return new Limites(extension, 10, 3, 40000, 6000000, 33554432, 120); }
    Path carpeta(String extension, byte[] bytes) throws IOException {
        Path carpeta = Files.createTempDirectory(raiz, "fixture-");
        Files.write(carpeta.resolve("entrada." + extension), bytes); return carpeta;
    }
    @Test void pdfYDocxConservanTextoOrdenYReferencias() throws Exception {
        var motor = new MotorExtraccion();
        var pdf = motor.extraer(carpeta("pdf", DocumentosFixtures.pdf("Primera pagina E17", "Segunda pagina E18")), limites("pdf"));
        assertThat(pdf.paginas()).isEqualTo(2);
        assertThat(pdf.partes()).extracting(Parte::pagina).containsExactly(1, 2);
        assertThat(pdf.partes().getFirst().texto()).contains("Primera pagina E17");
        var docx = motor.extraer(carpeta("docx", DocumentosFixtures.docx("Reiniciar aplicación E17")), limites("docx"));
        assertThat(docx.paginas()).isNull();
        assertThat(docx.partes().getFirst().texto()).contains("Reiniciar aplicación E17", "Primera celda", "Segunda celda", "Portal de ayuda");
    }
    @Test void preparaImagenesNormalizadasParaOcrSinConvertirTextoEnImagen() throws Exception {
        var motor = new MotorExtraccion();
        for (String extension : List.of("jpg", "jpeg")) {
            Path carpeta = carpeta(extension, DocumentosFixtures.imagen("jpg"));
            var resultado = motor.extraer(carpeta, limites(extension));
            assertThat(resultado.partes().getFirst().imagen()).isEqualTo("ocr-1.png");
            assertThat(Files.size(carpeta.resolve("ocr-1.png"))).isPositive();
        }
        Path carpeta = carpeta("pdf", DocumentosFixtures.escaneado(1));
        assertThat(motor.extraer(carpeta, limites("pdf")).partes().getFirst().imagen()).isEqualTo("ocr-1.png");
        assertThat(motor.extraer(carpeta("pdf", DocumentosFixtures.pdf((String) null)), limites("pdf")).partes()).isEmpty();
    }
    @Test void aplicaLimitesSinDevolverUnDocumentoTruncado() throws Exception {
        var motor = new MotorExtraccion();
        Path paginas = carpeta("pdf", DocumentosFixtures.pdf("Una", "Dos"));
        assertThatThrownBy(() -> motor.extraer(paginas, new Limites("pdf", 1, 1, 40000, 6000000, 33554432, 120))).hasMessage("LIMITE_PAGINAS");
        for (String extension : List.of("pdf", "docx")) {
            byte[] bytes = extension.equals("pdf") ? DocumentosFixtures.pdf("Texto demasiado largo E17") : DocumentosFixtures.docx("Texto demasiado largo E17");
            Path carpeta = carpeta(extension, bytes);
            assertThatThrownBy(() -> motor.extraer(carpeta, new Limites(extension, 10, 3, 5, 6000000, 33554432, 120))).hasMessage("LIMITE_CARACTERES");
        }
        Path ocr = carpeta("pdf", DocumentosFixtures.escaneado(2));
        assertThatThrownBy(() -> motor.extraer(ocr, new Limites("pdf", 10, 1, 40000, 6000000, 33554432, 120))).hasMessage("LIMITE_OCR");
        for (String extension : List.of("pdf", "jpg")) {
            Path carpeta = carpeta(extension, extension.equals("pdf") ? DocumentosFixtures.escaneado(1) : DocumentosFixtures.imagen("jpg"));
            assertThatThrownBy(() -> motor.extraer(carpeta, new Limites(extension, 10, 3, 40000, 100, 33554432, 120))).hasMessage("LIMITE_PIXELES");
        }
        Path disco = carpeta("jpg", DocumentosFixtures.imagen("jpg"));
        assertThatThrownBy(() -> motor.extraer(disco, new Limites("jpg", 10, 3, 40000, 6000000, 10, 120))).isInstanceOf(LimiteDocumento.class).hasMessage("LIMITE_TEMPORAL");
    }
    @Test void rechazaFormatosFalsosYCifrados() throws Exception {
        var motor = new MotorExtraccion();
        Path falsa = carpeta("jpg", DocumentosFixtures.imagen("png"));
        assertThatThrownBy(() -> motor.extraer(falsa, limites("jpg"))).isInstanceOf(IOException.class);
        Path basura = carpeta("jpg", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> motor.extraer(basura, limites("jpg"))).isInstanceOf(IOException.class);
        Path protegido = carpeta("pdf", DocumentosFixtures.protegido(""));
        assertThatThrownBy(() -> motor.extraer(protegido, limites("pdf"))).hasMessage("DOCUMENTO_PROTEGIDO");
        Path otro = carpeta("exe", new byte[]{1});
        assertThatThrownBy(() -> motor.extraer(otro, limites("exe"))).isInstanceOf(IOException.class);
    }
    @Test void workerDevuelveUnProtocoloAcotadoSinExponerExcepciones() throws Exception {
        for (String caso : List.of("valido", "limite", "formato", "cifrado", "danado")) {
            byte[] datos = switch (caso) {
                case "valido", "limite" -> DocumentosFixtures.pdf("Reiniciar aplicación");
                case "formato" -> new byte[]{1, 2};
                case "cifrado" -> DocumentosFixtures.protegido("reader-test");
                default -> "%PDF-1.7\ncontenido invalido\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            };
            Path carpeta = carpeta("pdf", datos);
            Limites limites = caso.equals("limite") ? new Limites("pdf", 10, 3, 2, 6000000, 33554432, 120) : limites("pdf");
            Files.write(carpeta.resolve("config.json"), json.writeValueAsBytes(limites));
            ExtractorWorker.main(new String[]{carpeta.toString()});
            var resultado = json.readValue(Files.readAllBytes(carpeta.resolve("resultado.json")), Resultado.class);
            String error = switch (caso) {
                case "valido" -> null; case "limite" -> "LIMITE_CARACTERES"; case "formato" -> "FORMATO_INVALIDO";
                case "cifrado" -> "DOCUMENTO_PROTEGIDO"; default -> "DOCUMENTO_INVALIDO";
            };
            assertThat(resultado.error()).isEqualTo(error);
        }
    }
}
