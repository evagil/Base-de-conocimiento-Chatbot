package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.documentos.service.GestorTemporales;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GestorTemporalesTest {
    @TempDir Path carpeta;
    @Test void creaDirectoriosPrivadosYEliminaTodoAlCerrar() throws Exception {
        var gestor = new GestorTemporales(ConfiguracionesDocumentos.crear(carpeta.resolve("privado"), true, Duration.ofSeconds(60), 40000));
        Path trabajo;
        try (var temporal = gestor.crear()) {
            trabajo = temporal.carpeta();
            Files.createDirectories(trabajo.resolve("subdirectorio"));
            Files.writeString(trabajo.resolve("subdirectorio/texto.txt"), "Documento sintético");
            var posix = Files.getFileAttributeView(trabajo, PosixFileAttributeView.class);
            if (posix != null) assertThat(Files.getPosixFilePermissions(trabajo)).isEqualTo(PosixFilePermissions.fromString("rwx------"));
            else assertThat(Files.getFileAttributeView(trabajo, AclFileAttributeView.class).getAcl())
                    .allSatisfy(permiso -> assertThat(permiso.principal()).isEqualTo(Files.getOwner(trabajo)));
        }
        assertThat(Files.exists(trabajo)).isFalse();
    }
    @Test void limpiaRestosVencidosPeroConservaTrabajosActivosYCarpetasAjenas() throws Exception {
        var configuracion = ConfiguracionesDocumentos.crear(carpeta, true, Duration.ofSeconds(60), 40000);
        var gestor = new GestorTemporales(configuracion);
        Path antigua = Files.createDirectory(carpeta.resolve("consulta-antigua"));
        Files.writeString(antigua.resolve("resto.txt"), "Sintético");
        Files.setLastModifiedTime(antigua, FileTime.from(Instant.now().minus(Duration.ofMinutes(20))));
        Path reciente = Files.createDirectory(carpeta.resolve("consulta-reciente"));
        Path ajena = Files.createDirectory(carpeta.resolve("otra-carpeta"));
        Files.setLastModifiedTime(ajena, FileTime.from(Instant.now().minus(Duration.ofMinutes(20))));
        try (var activo = gestor.crear()) {
            Files.setLastModifiedTime(activo.carpeta(), FileTime.from(Instant.now().minus(Duration.ofMinutes(20))));
            gestor.limpiarVencidos();
            assertThat(Files.exists(activo.carpeta())).isTrue();
        }
        assertThat(Files.exists(antigua)).isFalse();
        assertThat(Files.exists(reciente)).isTrue();
        assertThat(Files.exists(ajena)).isTrue();
        new GestorTemporales(ConfiguracionesDocumentos.crear(carpeta, false, Duration.ofSeconds(60), 40000)).limpiarVencidos();
        new GestorTemporales(ConfiguracionesDocumentos.crear(carpeta.resolve("inexistente"), true, Duration.ofSeconds(60), 40000)).limpiarVencidos();
    }
}
