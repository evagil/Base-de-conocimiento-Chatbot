package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.proceso.EjecutorProcesos;
import com.municipio.base_conocimiento.documentos.service.TesseractOcr;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

class TesseractOcrTest {
    @TempDir Path carpeta;
    @Test void pasaArgumentosSeparadosYLeeUtf8ConDatosConfigurados() throws Exception {
        var config = new ConfiguracionDocumentos(true,carpeta,"tesseract","datos idioma","spa+eng",Duration.ofSeconds(60),10485760,10,3,256,6000000,33554432,120);
        var procesos = mock(EjecutorProcesos.class);
        when(procesos.ejecutar(anyList(), any(), any())).thenAnswer(invocacion -> {
            List<String> comando = invocacion.getArgument(0);
            assertThat(comando).containsSubsequence("-l","spa+eng","--psm","3","--tessdata-dir","datos idioma");
            Files.writeString(Path.of(comando.get(2)+".txt"),"Aplicación E17"); return 0;
        });
        assertThat(new TesseractOcr(config,procesos).reconocer(carpeta.resolve("imagen.png"),carpeta,Duration.ofSeconds(5))).isEqualTo("Aplicación E17");
    }
    @Test void manejaMotorAusenteIdiomasInvalidosYSalidaExcesiva() throws Exception {
        var config = ConfiguracionesDocumentos.crear(carpeta,true,Duration.ofSeconds(60),256);
        var procesos = mock(EjecutorProcesos.class);
        var ocr = new TesseractOcr(config,procesos);
        when(procesos.ejecutar(anyList(),any(),any())).thenReturn(1);
        assertThatThrownBy(() -> ocr.reconocer(carpeta.resolve("imagen.png"),carpeta,Duration.ofSeconds(1))).isInstanceOfSatisfying(DocumentoException.class,e -> assertThat(e.getCodigo()).isEqualTo("OCR_NO_DISPONIBLE"));
        doReturn(0).when(procesos).ejecutar(anyList(),any(),any());
        assertThatThrownBy(() -> ocr.reconocer(carpeta.resolve("imagen.png"),carpeta,Duration.ofSeconds(1))).isInstanceOf(DocumentoException.class);
        doAnswer(invocacion -> { List<String> comando=invocacion.getArgument(0); Files.writeString(Path.of(comando.get(2)+".txt"),"x".repeat(10000)); return 0; }).when(procesos).ejecutar(anyList(),any(),any());
        assertThatThrownBy(() -> ocr.reconocer(carpeta.resolve("imagen.png"),carpeta,Duration.ofSeconds(1))).isInstanceOfSatisfying(DocumentoException.class,e -> assertThat(e.getCodigo()).isEqualTo("LIMITE_CARACTERES"));
        for (String codigo : List.of("PROCESADOR_NO_DISPONIBLE","DOCUMENTO_TIMEOUT")) {
            doThrow(new DocumentoException(HttpStatus.SERVICE_UNAVAILABLE,codigo,"Prueba")).when(procesos).ejecutar(anyList(),any(),any());
            assertThatThrownBy(() -> ocr.reconocer(carpeta.resolve("imagen.png"),carpeta,Duration.ofSeconds(1))).isInstanceOfSatisfying(DocumentoException.class,e ->
                    assertThat(e.getCodigo()).isEqualTo(codigo.equals("PROCESADOR_NO_DISPONIBLE") ? "OCR_NO_DISPONIBLE" : codigo));
        }
    }
}
