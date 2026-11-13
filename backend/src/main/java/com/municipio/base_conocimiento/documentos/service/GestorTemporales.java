package com.municipio.base_conocimiento.documentos.service;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GestorTemporales {
    private final ConfiguracionDocumentos configuracion;
    private final Set<Path> activos = ConcurrentHashMap.newKeySet();
    public GestorTemporales(ConfiguracionDocumentos configuracion) { this.configuracion = configuracion; }

    public Trabajo crear() throws IOException {
        Path raiz = raiz();
        Path carpeta = Files.createTempDirectory(raiz, "consulta-").toAbsolutePath().normalize();
        if (!carpeta.getParent().equals(raiz)) throw new IOException("Directorio temporal inválido.");
        proteger(carpeta);
        activos.add(carpeta);
        return new Trabajo(carpeta);
    }
    private Path raiz() throws IOException {
        Path ruta = configuracion.temporales();
        Files.createDirectories(ruta);
        if (Files.isSymbolicLink(ruta)) throw new IOException("La carpeta temporal no puede ser un enlace.");
        proteger(ruta);
        return ruta.toRealPath();
    }
    private void proteger(Path ruta) throws IOException {
        if (Files.getFileAttributeView(ruta, PosixFileAttributeView.class) != null) {
            Files.setPosixFilePermissions(ruta, PosixFilePermissions.fromString("rwx------"));
        } else {
            AclFileAttributeView acl = Files.getFileAttributeView(ruta, AclFileAttributeView.class);
            if (acl == null) throw new IOException("El sistema de archivos no permite proteger los temporales.");
            acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(ruta))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT).build()));
        }
    }
    private void borrar(Path carpeta) throws IOException {
        Path destino = carpeta.toAbsolutePath().normalize();
        if (!destino.getParent().equals(raiz()) || !destino.getFileName().toString().startsWith("consulta-")) {
            throw new IOException("Se rechazó una ruta ajena a los temporales de la aplicación.");
        }
        Files.walkFileTree(destino, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path archivo, BasicFileAttributes attrs) throws IOException {
                Files.delete(archivo); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path directorio, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directorio); return FileVisitResult.CONTINUE;
            }
        });
    }
    @Scheduled(fixedDelay = 60000)
    public void limpiarVencidos() {
        if (!configuracion.habilitada() || !Files.isDirectory(configuracion.temporales(), LinkOption.NOFOLLOW_LINKS)) return;
        try (var carpetas = Files.list(raiz())) {
            for (Path carpeta : carpetas.limit(100).toList()) {
                if (!activos.contains(carpeta) && carpeta.getFileName().toString().startsWith("consulta-")
                        && Files.isDirectory(carpeta, LinkOption.NOFOLLOW_LINKS)
                        && Files.getLastModifiedTime(carpeta).toInstant().isBefore(Instant.now().minus(Duration.ofMinutes(10)))) borrar(carpeta);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(GestorTemporales.class).warn("No se pudo completar la limpieza temporal; se reintentará.");
        }
    }
    public final class Trabajo implements AutoCloseable {
        private final Path carpeta;
        private Trabajo(Path carpeta) { this.carpeta = carpeta; }
        public Path carpeta() { return carpeta; }
        @Override public void close() throws IOException {
            try { borrar(carpeta); } finally { activos.remove(carpeta); }
        }
    }
}
