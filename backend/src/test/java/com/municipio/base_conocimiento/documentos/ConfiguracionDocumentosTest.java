package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfiguracionDocumentosTest {
    private ConfiguracionDocumentos configurar(int opcion) {
        return new ConfiguracionDocumentos(true, opcion == 1 ? null : opcion == 2 ? Path.of("/").toAbsolutePath().getRoot() : Path.of("target/docs"),
                opcion == 3 ? null : opcion == 4 ? " " : opcion == 5 ? "x".repeat(513) : "tesseract",
                opcion == 6 ? null : "", opcion == 7 ? null : opcion == 8 ? "../spa" : "spa+eng",
                opcion == 9 ? null : Duration.ofSeconds(opcion == 10 ? -1 : opcion == 11 ? 0 : opcion == 12 ? 121 : 60),
                opcion == 13 ? 1023 : opcion == 14 ? 10485761 : 10485760,
                opcion == 15 ? 0 : opcion == 16 ? 51 : 10, opcion == 17 ? 0 : opcion == 18 ? 11 : 3,
                opcion == 19 ? 255 : opcion == 20 ? 200001 : 40000,
                opcion == 21 ? 9999 : opcion == 22 ? 12000001 : 6000000,
                opcion == 23 ? 1000 : opcion == 24 ? 134217729 : 33554432,
                opcion == 25 ? 71 : opcion == 26 ? 201 : 120);
    }
    @Test void validaTodosLosLimitesAntesDeProcesar() {
        assertThat(configurar(0).temporales()).isAbsolute().isNormalized();
        for (int opcion = 1; opcion <= 26; opcion++) {
            int indice = opcion;
            assertThatThrownBy(() -> configurar(indice)).as("configuración inválida %d", opcion).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void limitaEscriturasIndividualesYEnBloqueSinTruncarElLimite() throws Exception {
        var contenido = new java.io.ByteArrayOutputStream();
        try (var salida = new com.municipio.base_conocimiento.documentos.worker.SalidaLimitada(contenido, 3, "LIMITE")) {
            salida.write(65); salida.write(new byte[]{66,67},0,2);
            assertThatThrownBy(() -> salida.write(68)).isInstanceOf(com.municipio.base_conocimiento.documentos.worker.LimiteDocumento.class);
            assertThatThrownBy(() -> salida.write(new byte[]{68,69},0,2)).isInstanceOf(com.municipio.base_conocimiento.documentos.worker.LimiteDocumento.class);
        }
        assertThat(contenido.toString(java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("ABC");
    }
}
