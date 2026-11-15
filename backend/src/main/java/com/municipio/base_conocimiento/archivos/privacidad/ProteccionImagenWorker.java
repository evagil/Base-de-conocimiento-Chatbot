package com.municipio.base_conocimiento.archivos.privacidad;

import com.municipio.base_conocimiento.documentos.proceso.ProcesoLimitado;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;
import tools.jackson.databind.ObjectMapper;

/** JVM limitada: no inicia Spring ni accede a bases de datos o a Ollama. */
public final class ProteccionImagenWorker {
 public record Config(String tesseract,String tessdata,String idiomas,long maxPixeles){}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).toAbsolutePath().normalize();
  var config=new ObjectMapper().readValue(Files.readAllBytes(dir.resolve("proteccion.json")),Config.class);
  try(var entrada=ImageIO.createImageInputStream(dir.resolve("entrada.imagen").toFile())){
   var lectores=ImageIO.getImageReaders(entrada);if(!lectores.hasNext())throw new IllegalArgumentException("Imagen no procesable");var lector=lectores.next();
   java.awt.image.BufferedImage imagen;
   try{lector.setInput(entrada);int w=lector.getWidth(0),h=lector.getHeight(0);if(w<=0 || h<=0 || (long)w*h>config.maxPixeles())throw new IllegalArgumentException("Límite de imagen");imagen=lector.read(0);}finally{lector.dispose();}
   Path png=dir.resolve("normalizada.png");ImageIO.write(imagen,"png",png.toFile());
   var comando=new ArrayList<>(List.of(config.tesseract(),png.toString(),dir.resolve("ocr").toString(),"-l",config.idiomas(),"--psm","11"));
   if(!config.tessdata().isBlank())comando.addAll(List.of("--tessdata-dir",config.tessdata()));comando.add("tsv");
   if(new ProcesoLimitado().ejecutar(comando,dir,Duration.ofSeconds(25))!=0)throw new IllegalStateException("OCR no disponible");
   Path tsv=dir.resolve("ocr.tsv");if(!Files.isRegularFile(tsv,LinkOption.NOFOLLOW_LINKS) || Files.size(tsv)>2097152)throw new IllegalStateException("OCR inválido");
   var motor=new MotorProteccionImagen();var palabras=motor.leerTsv(Files.readString(tsv),imagen.getWidth(),imagen.getHeight());
   if(palabras.isEmpty())throw new IllegalStateException("No se pudo reconocer texto para proteger la imagen");
   ImageIO.write(motor.ocultar(imagen,motor.detectar(palabras,imagen.getWidth(),imagen.getHeight())),"png",dir.resolve("protegida.png").toFile());
  }
 }
}
