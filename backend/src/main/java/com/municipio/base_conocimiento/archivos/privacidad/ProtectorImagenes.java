package com.municipio.base_conocimiento.archivos.privacidad;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import com.municipio.base_conocimiento.documentos.proceso.ExtractorAislado;
import com.municipio.base_conocimiento.documentos.service.GestorTemporales;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service public class ProtectorImagenes {
 private final ConfiguracionDocumentos config;private final GestorTemporales temporales;private final ExtractorAislado extractor;private final ObjectMapper json;private final Path cache;
 private final Semaphore turno=new Semaphore(1);
 public ProtectorImagenes(ConfiguracionDocumentos config,GestorTemporales temporales,ExtractorAislado extractor,ObjectMapper json,@Value("${app.storage.path}")String storage){
  this.config=config;this.temporales=temporales;this.extractor=extractor;this.json=json;cache=Path.of(storage).toAbsolutePath().normalize().resolve(".vistas-protegidas-v1");
 }
 public byte[] proteger(byte[] original){
  try{
   if(original==null || original.length==0 || original.length>config.maxArchivoBytes())throw new IllegalArgumentException("Límite de imagen");
   var digest=MessageDigest.getInstance("SHA-256");digest.update(config.idiomas().getBytes(java.nio.charset.StandardCharsets.UTF_8));
   String nombre=HexFormat.of().formatHex(digest.digest(original))+".png";Path destino=cache.resolve(nombre);
   if(Files.isRegularFile(destino,LinkOption.NOFOLLOW_LINKS))return leer(destino);
   if(!turno.tryAcquire())throw new ArchivoInvalidoException(HttpStatus.TOO_MANY_REQUESTS,"PROTECCION_OCUPADA","Se está protegiendo otra imagen. Reintentá en unos instantes.");
   try{
    if(Files.isRegularFile(destino,LinkOption.NOFOLLOW_LINKS))return leer(destino);
    try(var trabajo=temporales.crear()){
     Path dir=trabajo.carpeta();Files.write(dir.resolve("entrada.imagen"),original,StandardOpenOption.CREATE_NEW);
     Files.write(dir.resolve("proteccion.json"),json.writeValueAsBytes(new ProteccionImagenWorker.Config(config.tesseract(),config.tessdata(),config.idiomas(),config.maxPixeles())));
     extractor.ejecutarWorker(dir,ProteccionImagenWorker.class,Duration.ofSeconds(40));byte[] protegida=leer(dir.resolve("protegida.png"));
     Files.createDirectories(cache);if(Files.isSymbolicLink(cache))throw new IllegalStateException("Caché inválida");
     Path temporal=Files.createTempFile(cache,"vista-",".tmp");try{Files.write(temporal,protegida);Files.move(temporal,destino,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporal);}
     return protegida;
    }
   }finally{turno.release();}
  }catch(ArchivoInvalidoException e){throw e;}
   catch(Exception e){throw new ArchivoInvalidoException(HttpStatus.SERVICE_UNAVAILABLE,"PROTECCION_NO_DISPONIBLE","No se muestra la imagen porque no se pudo proteger. Revisá OCR, formato y límites con administración.");}
 }
 private byte[] leer(Path archivo)throws java.io.IOException{
  if(!Files.isRegularFile(archivo,LinkOption.NOFOLLOW_LINKS) || Files.size(archivo)>config.maxTemporalBytes())throw new java.io.IOException("Vista inválida");
  byte[] b=Files.readAllBytes(archivo);if(b.length<8 || b[0]!=(byte)0x89 || b[1]!=0x50 || b[2]!=0x4e || b[3]!=0x47)throw new java.io.IOException("Vista inválida");return b;
 }
}
