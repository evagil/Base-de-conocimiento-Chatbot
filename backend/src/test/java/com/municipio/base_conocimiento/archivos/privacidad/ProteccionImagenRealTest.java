package com.municipio.base_conocimiento.archivos.privacidad;
import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.documentos.ConfiguracionesDocumentos;
import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.proceso.*;
import com.municipio.base_conocimiento.documentos.service.GestorTemporales;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

@EnabledIfEnvironmentVariable(named="BC_TEST_TESSERACT",matches=".+")
class ProteccionImagenRealTest {
 @TempDir Path raiz;
 @Test void workerAisladoYOcrRealOcultanDatosSinteticosYReutilizanLaVista()throws Exception{
  var base=ConfiguracionesDocumentos.crear(raiz.resolve("temp"),true,Duration.ofSeconds(60),40000);
  var config=new ConfiguracionDocumentos(true,base.temporales(),System.getenv("BC_TEST_TESSERACT"),System.getenv().getOrDefault("BC_TEST_TESSDATA",""),"spa+eng",base.timeout(),base.maxArchivoBytes(),base.maxPaginas(),base.maxOcrPaginas(),base.maxCaracteres(),base.maxPixeles(),base.maxTemporalBytes(),base.dpi());
  var json=new ObjectMapper();var servicio=new ProtectorImagenes(config,new GestorTemporales(config),new ExtractorAislado(config,new ProcesoLimitado(),json),json,raiz.resolve("storage").toString());
  var imagen=new BufferedImage(1000,400,BufferedImage.TYPE_INT_RGB);var g=imagen.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,1000,400);g.setColor(Color.BLACK);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,28));
  g.drawString("Contribuyente: PERSONA DEMO",20,55);g.drawString("Proveedor: EMPRESA DEMO",20,115);g.drawString("Importe: $ 1234,56",20,175);g.drawString("Presionar Guardar",20,280);g.setColor(Color.BLUE);g.fillRect(800,300,50,30);g.dispose();
  var salida=new ByteArrayOutputStream();ImageIO.write(imagen,"png",salida);byte[] original=salida.toByteArray();
  long inicio=System.nanoTime();byte[] bytes=servicio.proteger(original);long primera=(System.nanoTime()-inicio)/1_000_000;
  var protegida=ImageIO.read(new ByteArrayInputStream(bytes));assertThat(protegida.getWidth()).isEqualTo(1000);
  assertThat(protegida.getRGB(400,40)).isEqualTo(new Color(190,194,199).getRGB());assertThat(protegida.getRGB(810,310)).isEqualTo(Color.BLUE.getRGB());
  inicio=System.nanoTime();assertThat(servicio.proteger(original)).isEqualTo(bytes);long cache=(System.nanoTime()-inicio)/1_000_000;
  Files.write(Path.of("..",".local","imagen-protegida-ensayo.png"),bytes);
  System.out.printf("PRIVACIDAD_IMAGEN primeraMs=%d cacheMs=%d%n",primera,cache);
 }
}
