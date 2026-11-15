package com.municipio.base_conocimiento.documentos.proceso;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.worker.ExtractorWorker;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ExtractorAislado {
    private final ConfiguracionDocumentos configuracion;
    private final EjecutorProcesos procesos;
    private final ObjectMapper json;
    public ExtractorAislado(ConfiguracionDocumentos configuracion, EjecutorProcesos procesos, ObjectMapper json) {
        this.configuracion = configuracion; this.procesos = procesos; this.json = json;
    }
    public ProtocoloWorker.Resultado extraer(Path directorio, String extension, Duration restante) throws IOException {
        var limites = new ProtocoloWorker.Limites(extension, configuracion.maxPaginas(), configuracion.maxOcrPaginas(),
                configuracion.maxCaracteres(), configuracion.maxPixeles(), configuracion.maxTemporalBytes(), configuracion.dpi());
        Files.write(directorio.resolve("config.json"), json.writeValueAsBytes(limites));
        List<String> comando = comando(directorio);
        int codigo = procesos.ejecutar(comando, directorio, restante);
        Path resultado = directorio.resolve("resultado.json");
        if (codigo != 0 || !Files.isRegularFile(resultado, LinkOption.NOFOLLOW_LINKS)
                || Files.size(resultado) > configuracion.maxCaracteres() * 8L + 8192) {
            throw new DocumentoException(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENTO_NO_PROCESABLE", "No se pudo procesar el archivo dentro de los límites seguros.");
        }
        return json.readValue(Files.readAllBytes(resultado), ProtocoloWorker.Resultado.class);
    }
    List<String> comando(Path directorio) throws IOException {
        return comando(directorio, ExtractorWorker.class);
    }
    public void ejecutarWorker(Path directorio, Class<?> worker, Duration timeout) throws IOException {
        if (procesos.ejecutar(comando(directorio, worker), directorio, timeout) != 0) {
            throw new IOException("El procesador aislado no pudo completar la operación.");
        }
    }
    private List<String> comando(Path directorio, Class<?> worker) throws IOException {
        String classpath = String.join(File.pathSeparator, Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(p -> Path.of(p).toAbsolutePath().normalize().toString()).toList());
        boolean empaquetado = false;
        for (String entrada : classpath.split(Pattern.quote(File.pathSeparator))) {
            if (entrada.endsWith(".jar") && Files.isRegularFile(Path.of(entrada))) {
                try (ZipFile jar = new ZipFile(entrada)) {
                    if (jar.getEntry("BOOT-INF/classes/") != null) { empaquetado = true; break; }
                }
            }
        }
        List<String> comando = new ArrayList<>();
        String java = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        comando.add(Path.of(System.getProperty("java.home"), "bin", java).toString());
        comando.add("-Xmx256m");
        comando.add("-XX:MaxMetaspaceSize=128m");
        comando.add("-XX:ActiveProcessorCount=2");
        comando.add("-XX:+ExitOnOutOfMemoryError");
        comando.add("-Djava.awt.headless=true");
        comando.add("-Djavax.xml.accessExternalDTD=");
        comando.add("-Djavax.xml.accessExternalSchema=");
        comando.add("-Djdk.xml.maxElementDepth=256");
        comando.add("-Djava.io.tmpdir=" + directorio);
        comando.add("-Dpdfbox.fontcache=" + directorio);
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argumento -> argumento.startsWith("-javaagent:") && argumento.contains("org.jacoco.agent")).forEach(comando::add);
        if (empaquetado) comando.add("-Dloader.main=" + worker.getName());
        comando.addAll(List.of("-cp", classpath, empaquetado ? "org.springframework.boot.loader.launch.PropertiesLauncher" : worker.getName(), directorio.toString()));
        return comando;
    }
}
