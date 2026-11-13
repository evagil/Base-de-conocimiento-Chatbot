package com.municipio.base_conocimiento.documentos.proceso;

import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProcesoLimitado implements EjecutorProcesos {
    @Override public int ejecutar(List<String> comando, Path directorio, Duration timeout) {
        if (timeout.isNegative() || timeout.isZero()) throw timeout();
        Process proceso;
        try {
            ProcessBuilder builder = new ProcessBuilder(comando).directory(directorio.toFile()).redirectErrorStream(true);
            Set<String> permitidas = Set.of("PATH", "SYSTEMROOT", "WINDIR", "USERPROFILE", "HOME", "LANG", "LC_ALL", "LD_LIBRARY_PATH");
            builder.environment().keySet().removeIf(nombre -> !permitidas.contains(nombre.toUpperCase(Locale.ROOT)));
            for (String variable : List.of("TEMP", "TMP", "TMPDIR")) builder.environment().put(variable, directorio.toString());
            builder.environment().put("OMP_THREAD_LIMIT", "2");
            builder.environment().put("OMP_NUM_THREADS", "2");
            proceso = builder.start();
            proceso.getOutputStream().close();
        } catch (IOException e) {
            throw new DocumentoException(HttpStatus.SERVICE_UNAVAILABLE, "PROCESADOR_NO_DISPONIBLE", "No se pudo iniciar el procesador local. Revisá su instalación.");
        }
        try (InputStream salida = proceso.getInputStream()) {
            Thread drenar = Thread.ofVirtual().start(() -> {
                try { salida.transferTo(OutputStream.nullOutputStream()); } catch (IOException ignored) { }
            });
            try {
                if (!proceso.waitFor(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS)) throw timeout();
                return proceso.exitValue();
            } finally { terminar(proceso); drenar.interrupt(); }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentoException(HttpStatus.SERVICE_UNAVAILABLE, "PROCESAMIENTO_INTERRUMPIDO", "Se interrumpió el procesamiento del archivo.");
        } catch (IOException e) {
            throw new DocumentoException(HttpStatus.SERVICE_UNAVAILABLE, "PROCESADOR_NO_DISPONIBLE", "No se pudo completar el procesamiento local.");
        } finally {
            terminar(proceso);
        }
    }
    private void terminar(Process proceso) {
        if (proceso.isAlive()) {
                proceso.descendants().forEach(ProcessHandle::destroyForcibly);
                proceso.destroyForcibly();
                try { proceso.onExit().get(5, TimeUnit.SECONDS); } catch (Exception ignored) { }
        }
    }
    private DocumentoException timeout() {
        return new DocumentoException(HttpStatus.GATEWAY_TIMEOUT, "DOCUMENTO_TIMEOUT", "El archivo tardó demasiado en procesarse. Probá con un documento más pequeño.");
    }
}
