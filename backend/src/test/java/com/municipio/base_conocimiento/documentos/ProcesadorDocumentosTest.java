package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.proceso.*;
import com.municipio.base_conocimiento.documentos.proceso.ProtocoloWorker.*;
import com.municipio.base_conocimiento.documentos.service.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class ProcesadorDocumentosTest {
    @TempDir Path carpeta;
    ConfiguracionDocumentos configuracion;
    ExtractorAislado extractor;
    MotorOcr ocr;
    ProcesadorDocumentos procesador;
    @BeforeEach void preparar() throws Exception {
        configuracion = ConfiguracionesDocumentos.crear(carpeta.resolve("privado"), true, Duration.ofSeconds(60), 256);
        extractor = mock(ExtractorAislado.class); ocr = mock(MotorOcr.class);
        when(extractor.extraer(any(), anyString(), any())).thenReturn(new Resultado(1, List.of(new Parte(1, "Texto E17", null)), null));
        procesador = servicio(configuracion);
    }
    ProcesadorDocumentos servicio(ConfiguracionDocumentos configuracion) {
        return new ProcesadorDocumentos(configuracion, new GestorTemporales(configuracion), extractor, ocr);
    }
    MockMultipartFile archivo(String nombre) { return new MockMultipartFile("archivo", nombre, "text/plain", new byte[]{1, 2, 3}); }
    void codigo(String codigo, org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(DocumentoException.class, e -> assertThat(e.getCodigo()).isEqualTo(codigo));
    }
    void sinRestos() throws IOException {
        if (Files.exists(configuracion.temporales())) try (var archivos = Files.list(configuracion.temporales())) { assertThat(archivos.toList()).isEmpty(); }
    }
    @Test void respondeSinRutasInternasYSiempreEliminaLaCopiaTemporal() throws Exception {
        var resultado = procesador.extraer(archivo("manual;seguro.PDF"));
        assertThat(resultado.nombre()).isEqualTo("manual;seguro.PDF");
        assertThat(resultado.tipoMime()).isEqualTo("application/pdf");
        assertThat(resultado.secciones().getFirst().texto()).isEqualTo("Texto E17");
        assertThat(resultado.caracteres()).isEqualTo(9);
        assertThat(resultado.ocrUsado()).isFalse(); sinRestos();
        when(extractor.extraer(any(), anyString(), any())).thenReturn(new Resultado(null, List.of(new Parte(null, "Tabla y párrafos", null)), null));
        assertThat(procesador.extraer(archivo("manual.docx")).paginas()).isNull(); sinRestos();
    }
    @Test void validaOcrNormalizaTextoYConservaPagina() throws Exception {
        when(extractor.extraer(any(), anyString(), any())).thenAnswer(invocacion -> {
            Path raiz = invocacion.getArgument(0); Files.write(raiz.resolve("ocr-1.png"), new byte[]{1});
            return new Resultado(1, List.of(new Parte(1, "", "ocr-1.png")), null);
        });
        when(ocr.reconocer(any(), any(), any())).thenReturn("\r\nERROR\u0000 E17\rAPLICACIO\u0301N\tOK\r\n");
        var resultado = procesador.extraer(archivo("captura.jpeg"));
        assertThat(resultado.tipoMime()).isEqualTo("image/jpeg");
        assertThat(resultado.ocrUsado()).isTrue();
        assertThat(resultado.secciones().getFirst().texto()).isEqualTo("ERROR E17\nAPLICACIÓN\tOK");
        assertThat(resultado.secciones().getFirst().metodo()).isEqualTo("OCR"); sinRestos();
        when(ocr.reconocer(any(), any(), any())).thenReturn(" ");
        codigo("DOCUMENTO_SIN_TEXTO", () -> procesador.extraer(archivo("captura.jpg"))); sinRestos();
        when(ocr.reconocer(any(), any(), any())).thenReturn(null);
        codigo("DOCUMENTO_INVALIDO", () -> procesador.extraer(archivo("captura.jpg"))); sinRestos();
    }
    @Test void rechazaArchivosYFeatureDeshabilitadaAntesDeCrearTemporales() throws Exception {
        codigo("DOCUMENTOS_DESHABILITADOS", () -> servicio(ConfiguracionesDocumentos.crear(carpeta, false, Duration.ofSeconds(60), 256)).extraer(archivo("a.pdf")));
        codigo("ARCHIVO_VACIO", () -> procesador.extraer(null));
        codigo("ARCHIVO_VACIO", () -> procesador.extraer(new MockMultipartFile("archivo", new byte[0])));
        for (String nombre : Arrays.asList(null, "sinextension", ".pdf", "../manual.pdf", "C:\\manual.pdf", "x\u0000.pdf", "x".repeat(260)+".pdf")) {
            codigo("NOMBRE_ARCHIVO_INVALIDO", () -> procesador.extraer(archivo(nombre)));
        }
        for (String nombre : List.of("manual.doc", "archivo.exe", "imagen.png")) codigo("FORMATO_INVALIDO", () -> procesador.extraer(archivo(nombre)));
        codigo("LIMITE_ARCHIVO", () -> procesador.extraer(new MockMultipartFile("archivo", "grande.pdf", "application/pdf", new byte[10485761])));
        verifyNoInteractions(extractor); sinRestos();
    }
    @Test void validaElTamanoRealDelStreamYLimpiaDespuesDeIo() throws Exception {
        var pequena = new ConfiguracionDocumentos(true, configuracion.temporales(), "tesseract", "", "spa", Duration.ofSeconds(60),
                1024, 10, 3, 256, 6000000, 33554432, 120);
        var archivo = new MockMultipartFile("archivo", "falso.pdf", "application/pdf", new byte[2048]) {
            @Override public long getSize() { return 1; }
        };
        codigo("LIMITE_ARCHIVO", () -> servicio(pequena).extraer(archivo)); sinRestos();
        when(extractor.extraer(any(), anyString(), any())).thenThrow(new IOException("detalle interno"));
        codigo("DOCUMENTO_ERROR_LOCAL", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
    }
    @Test void rechazaProtocolosInvalidosYNoLeeRutasAjenas() throws Exception {
        List<Resultado> invalidos = Arrays.asList(null, new Resultado(1, null, null),
                new Resultado(-1, List.of(), null), new Resultado(99, List.of(), null),
                new Resultado(1, Arrays.asList((Parte)null), null), new Resultado(1, List.of(new Parte(0,"Texto",null)), null),
                new Resultado(null, List.of(new Parte(1,"Texto",null)), null), new Resultado(1, List.of(new Parte(2,"Texto",null)), null),
                new Resultado(1, List.of(new Parte(1,null,null)), null),
                new Resultado(1, List.of(new Parte(1,"", "../ajeno.png")), null), new Resultado(1, List.of(new Parte(1,"", "ocr-1.png")), null),
                new Resultado(1, Collections.nCopies(11,new Parte(1,"Texto",null)),null));
        for (Resultado resultado : invalidos) {
            when(extractor.extraer(any(), anyString(), any())).thenReturn(resultado);
            codigo("DOCUMENTO_INVALIDO", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
        }
        verifyNoInteractions(ocr);
    }
    @Test void propagaLimitesYFallasSinDevolverResultadosParciales() throws Exception {
        for (String codigo : List.of("FORMATO_INVALIDO", "DOCUMENTO_PROTEGIDO", "LIMITE_ARCHIVO", "LIMITE_PAGINAS", "LIMITE_OCR", "LIMITE_CARACTERES", "LIMITE_PIXELES", "LIMITE_TEMPORAL", "DOCUMENTO_INVALIDO")) {
            when(extractor.extraer(any(), anyString(), any())).thenReturn(new Resultado(null,List.of(),codigo));
            codigo(codigo, () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
        }
        when(extractor.extraer(any(), anyString(), any())).thenReturn(new Resultado(1,List.of(new Parte(1,"x".repeat(257),null)),null));
        codigo("LIMITE_CARACTERES", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
        when(extractor.extraer(any(), anyString(), any())).thenReturn(new Resultado(1,List.of(new Parte(1," ",null)),null));
        codigo("DOCUMENTO_SIN_TEXTO", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
        var corto = servicio(ConfiguracionesDocumentos.crear(configuracion.temporales(),true,Duration.ofNanos(1),256));
        codigo("DOCUMENTO_TIMEOUT", () -> corto.extraer(archivo("manual.pdf"))); sinRestos();
    }
    @Test void limitaConcurrenciaYLiberaElTurnoAlFallar() throws Exception {
        CountDownLatch iniciado = new CountDownLatch(1), liberar = new CountDownLatch(1);
        when(extractor.extraer(any(), anyString(), any())).thenAnswer(invocacion -> {
            iniciado.countDown(); liberar.await(5, TimeUnit.SECONDS);
            throw new DocumentoException(HttpStatus.BAD_REQUEST,"FALLO_PRUEBA","Prueba");
        });
        var primera = CompletableFuture.runAsync(() -> procesador.extraer(archivo("manual.pdf")));
        try {
            assertThat(iniciado.await(3, TimeUnit.SECONDS)).isTrue();
            codigo("DOCUMENTO_OCUPADO", () -> procesador.extraer(archivo("manual.pdf")));
        } finally { liberar.countDown(); }
        assertThatThrownBy(() -> primera.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(DocumentoException.class);
        doReturn(new Resultado(1,List.of(new Parte(1,"Recuperado",null)),null)).when(extractor).extraer(any(), anyString(), any());
        assertThat(procesador.extraer(archivo("manual.pdf")).secciones().getFirst().texto()).isEqualTo("Recuperado"); sinRestos();
    }

    @Test void aplicaElPresupuestoTemporalYElMaximoDePaginasOcr() throws Exception {
        when(extractor.extraer(any(), anyString(), any())).thenAnswer(invocacion -> {
            Path raiz = invocacion.getArgument(0);
            try (var datos = new RandomAccessFile(raiz.resolve("grande.bin").toFile(), "rw")) { datos.setLength(configuracion.maxTemporalBytes() + 1); }
            return new Resultado(1, List.of(new Parte(1, "Texto", null)), null);
        });
        codigo("LIMITE_TEMPORAL", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
        doAnswer(invocacion -> {
            Path raiz = invocacion.getArgument(0);
            var partes = new ArrayList<Parte>();
            for (int i = 1; i <= 4; i++) { Files.write(raiz.resolve("ocr-" + i + ".png"), new byte[]{1}); partes.add(new Parte(i,"","ocr-" + i + ".png")); }
            return new Resultado(4, partes, null);
        }).when(extractor).extraer(any(), anyString(), any());
        when(ocr.reconocer(any(), any(), any())).thenReturn("Texto");
        codigo("LIMITE_OCR", () -> procesador.extraer(archivo("manual.pdf"))); sinRestos();
    }
}
