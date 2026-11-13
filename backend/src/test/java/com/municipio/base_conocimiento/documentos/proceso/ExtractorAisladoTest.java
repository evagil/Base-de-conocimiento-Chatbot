package com.municipio.base_conocimiento.documentos.proceso;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.documentos.*;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import java.nio.file.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class ExtractorAisladoTest {
    @TempDir Path carpeta;
    @Test void lanzaUnaJvmRealConMemoriaLimitada() throws Exception {
        var config = ConfiguracionesDocumentos.crear(carpeta,true,Duration.ofSeconds(120),40000);
        var extractor = new ExtractorAislado(config,new ProcesoLimitado(),new ObjectMapper());
        Files.write(carpeta.resolve("entrada.pdf"),DocumentosFixtures.pdf("ERROR E17 REINICIAR APLICACION"));
        var resultado = extractor.extraer(carpeta,"pdf",Duration.ofSeconds(120));
        assertThat(resultado.error()).isNull();
        assertThat(resultado.partes().getFirst().texto()).contains("ERROR E17");
        assertThat(extractor.comando(carpeta)).contains("-Xmx256m","-XX:MaxMetaspaceSize=128m","-Djavax.xml.accessExternalDTD=");
    }
    @Test void rechazaFallosYResultadosAusentesOExcesivos() throws Exception {
        var config = ConfiguracionesDocumentos.crear(carpeta,true,Duration.ofSeconds(60),256);
        var procesos = mock(EjecutorProcesos.class);
        var extractor = new ExtractorAislado(config,procesos,new ObjectMapper());
        when(procesos.ejecutar(anyList(),any(),any())).thenReturn(1);
        assertThatThrownBy(() -> extractor.extraer(carpeta,"pdf",Duration.ofSeconds(1))).isInstanceOf(DocumentoException.class);
        when(procesos.ejecutar(anyList(),any(),any())).thenReturn(0);
        assertThatThrownBy(() -> extractor.extraer(carpeta,"pdf",Duration.ofSeconds(1))).isInstanceOf(DocumentoException.class);
        Files.writeString(carpeta.resolve("resultado.json"),"x".repeat(11000));
        assertThatThrownBy(() -> extractor.extraer(carpeta,"pdf",Duration.ofSeconds(1))).isInstanceOf(DocumentoException.class);
    }
}
