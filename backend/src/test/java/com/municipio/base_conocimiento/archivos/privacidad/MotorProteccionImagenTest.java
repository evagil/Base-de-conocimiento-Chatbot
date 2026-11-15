package com.municipio.base_conocimiento.archivos.privacidad;
import static org.assertj.core.api.Assertions.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorProteccionImagenTest {
 final MotorProteccionImagen motor=new MotorProteccionImagen();
 static final String CABECERA="level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n";
 String palabra(int linea,int orden,int x,int y,String texto){return "5\t1\t1\t1\t"+linea+"\t"+orden+"\t"+x+"\t"+y+"\t60\t15\t90\t"+texto+"\n";}
 @Test void ocultaIdentidadesImportesConSimboloSeparadoYDecimalesSinModificarElOriginal(){
  String tsv=CABECERA+palabra(1,1,10,10,"Razón")+palabra(1,2,80,10,"Social:")+palabra(1,3,150,10,"EMPRESA")
    +palabra(2,1,10,90,"Importe")+palabra(2,2,100,90,"$")+palabra(2,3,170,90,"1000")
    +palabra(3,1,10,130,"Cantidad")+palabra(3,2,100,130,"123,45")+palabra(4,1,10,170,"Guardar");
  var palabras=motor.leerTsv(tsv,300,200);assertThat(palabras.get(0).linea()).isEqualTo(palabras.get(1).linea());
  var original=new BufferedImage(300,200,BufferedImage.TYPE_INT_RGB);var g=original.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,300,200);g.dispose();
  var resultado=motor.ocultar(original,motor.detectar(palabras,300,200));
  assertThat(resultado.getRGB(170,15)).isNotEqualTo(original.getRGB(170,15));assertThat(resultado.getRGB(180,95)).isNotEqualTo(original.getRGB(180,95));
  assertThat(resultado.getRGB(110,135)).isNotEqualTo(original.getRGB(110,135));assertThat(resultado.getRGB(30,175)).isEqualTo(original.getRGB(30,175));
 }
 @Test void ocultaValoresDeColumnasMonetariasYAceptaLimitesDeImagen(){
  String tsv=CABECERA+palabra(1,1,0,0,"Total")+palabra(2,1,0,35,"1000")+palabra(3,1,200,75,"Guardar");
  assertThat(motor.detectar(motor.leerTsv(tsv,300,100),300,100)).anySatisfy(z->assertThat(z.y()).isBetween(30,35));
  String dolares=CABECERA+palabra(1,1,10,10,"USD")+palabra(1,2,150,10,"1000");
  assertThat(motor.detectar(motor.leerTsv(dolares,300,100),300,100)).anySatisfy(z->assertThat(z.x()).isBetween(140,150));
  String bloquesSeparados=CABECERA+palabra(1,1,10,10,"$")+palabra(2,1,150,11,"1000");
  assertThat(motor.detectar(motor.leerTsv(bloquesSeparados,300,100),300,100)).anySatisfy(z->assertThat(z.x()).isBetween(140,150));
  assertThat(motor.detectar(List.of(),300,100)).isEmpty();
 }
 @Test void rechazaOcrCorruptoOTamanoFueraDeLimites(){
  for(String tsv:List.of("malo",CABECERA+"5\tmal\n",CABECERA+palabra(1,1,290,0,"Dato"),CABECERA+palabra(1,1,-1,0,"Dato"),CABECERA+palabra(1,1,1,0,"Dato").replace("\t60\t15\t","\ta\t15\t"),"x".repeat(2097153))){assertThatThrownBy(()->motor.leerTsv(tsv,300,100)).isInstanceOf(IllegalArgumentException.class);}
 }
 @Test void reconoceRotulosPluralesYSeDisponeDeLectorWebp(){
  String tsv=CABECERA+palabra(1,1,10,10,"Apellidos")+palabra(1,2,80,10,"Nombres")+palabra(1,3,150,10,"DEMO");
  assertThat(motor.detectar(motor.leerTsv(tsv,300,100),300,100)).isNotEmpty();
  assertThat(javax.imageio.ImageIO.getImageReadersByMIMEType("image/webp").hasNext()).isTrue();
 }
}
