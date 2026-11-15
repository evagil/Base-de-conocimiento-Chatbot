package com.municipio.base_conocimiento.archivos.privacidad;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.municipio.base_conocimiento.documentos.ConfiguracionesDocumentos;
import com.municipio.base_conocimiento.documentos.proceso.ExtractorAislado;
import com.municipio.base_conocimiento.documentos.service.GestorTemporales;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.nio.file.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class ProtectorImagenesTest {
 @TempDir Path raiz;
 byte[] png={(byte)0x89,0x50,0x4e,0x47,13,10,26,10,1};
 @Test void cacheaSoloLaCopiaProtegidaYNoEntregaElOriginalSiFalla()throws Exception{
  var config=ConfiguracionesDocumentos.crear(raiz.resolve("temp"),true,Duration.ofSeconds(60),40000);var extractor=mock(ExtractorAislado.class);
  doAnswer(i->{Path dir=i.getArgument(0);Files.write(dir.resolve("protegida.png"),png);return null;}).when(extractor).ejecutarWorker(any(),eq(ProteccionImagenWorker.class),any());
  var protector=new ProtectorImagenes(config,new GestorTemporales(config),extractor,new ObjectMapper(),raiz.resolve("storage").toString());
  byte[] original={1,2,3};assertThat(protector.proteger(original)).isEqualTo(png);assertThat(protector.proteger(original)).isEqualTo(png);verify(extractor).ejecutarWorker(any(),any(),any());assertThat(original).containsExactly(1,2,3);
  doThrow(new java.io.IOException("dato privado")).when(extractor).ejecutarWorker(any(),any(),any());
  assertThatThrownBy(()->protector.proteger(new byte[]{4})).isInstanceOfSatisfying(ArchivoInvalidoException.class,e->assertThat(e.getMessage()).doesNotContain("dato privado"));
  assertThatThrownBy(()->protector.proteger(new byte[0])).isInstanceOf(ArchivoInvalidoException.class);
 }
}
