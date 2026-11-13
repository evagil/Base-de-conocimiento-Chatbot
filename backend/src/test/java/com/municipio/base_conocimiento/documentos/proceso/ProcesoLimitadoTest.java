package com.municipio.base_conocimiento.documentos.proceso;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcesoLimitadoTest {
    @TempDir Path carpeta;
    private List<String> comando(String modo) {
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        return List.of(java, "-Xmx64m", "-cp", System.getProperty("java.class.path"), ProcesoPrueba.class.getName(), modo);
    }
    @Test void drenaLaSalidaYNoPropagaSecretosDelEntorno() throws Exception {
        var proceso = new ProcesoLimitado();
        assertThat(proceso.ejecutar(comando("salida"), carpeta, Duration.ofSeconds(15))).isZero();
        assertThat(proceso.ejecutar(comando("entorno"), carpeta, Duration.ofSeconds(15))).isZero();
        assertThat(Files.readString(carpeta.resolve("entorno.txt"))).doesNotContain("DB_PASSWORD", "JWT_SECRET", "JAVA_TOOL_OPTIONS", "MAVEN_OPTS", "BC_TEST_DB_URL");
        assertThat(proceso.ejecutar(comando("error"), carpeta, Duration.ofSeconds(15))).isEqualTo(7);
    }
    @Test void timeoutMataElProcesoSinEsperarSuFinalizacionNatural() throws Exception {
        var proceso = new ProcesoLimitado();
        long inicio = System.nanoTime();
        assertThatThrownBy(() -> proceso.ejecutar(comando("esperar"), carpeta, Duration.ofMillis(800)))
                .isInstanceOfSatisfying(DocumentoException.class, e -> assertThat(e.getCodigo()).isEqualTo("DOCUMENTO_TIMEOUT"));
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(10));
        if (Files.exists(carpeta.resolve("pid.txt"))) {
            long pid = Long.parseLong(Files.readString(carpeta.resolve("pid.txt")));
            assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        }
        assertThatThrownBy(() -> proceso.ejecutar(comando("esperar"), carpeta, Duration.ZERO)).isInstanceOf(DocumentoException.class);
        assertThatThrownBy(() -> proceso.ejecutar(List.of(carpeta.resolve("no-existe").toString()), carpeta, Duration.ofSeconds(1)))
                .isInstanceOfSatisfying(DocumentoException.class, e -> assertThat(e.getCodigo()).isEqualTo("PROCESADOR_NO_DISPONIBLE"));
    }
    @Test void conservaLaInterrupcionDelHiloYTerminaElHijo() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new ProcesoLimitado().ejecutar(comando("esperar"), carpeta, Duration.ofSeconds(10)))
                    .isInstanceOfSatisfying(DocumentoException.class, e -> assertThat(e.getCodigo()).isEqualTo("PROCESAMIENTO_INTERRUMPIDO"));
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }
}
