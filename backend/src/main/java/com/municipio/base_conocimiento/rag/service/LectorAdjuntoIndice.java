package com.municipio.base_conocimiento.rag.service;

import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.documentos.service.ProcesadorDocumentos;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.Fuente;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LectorAdjuntoIndice {
    private final Path raiz;
    private final ProcesadorDocumentos documentos;
    public LectorAdjuntoIndice(@Value("${app.storage.path}")String raiz,ProcesadorDocumentos documentos){this.raiz=Path.of(raiz).toAbsolutePath().normalize();this.documentos=documentos;}
    public DocumentoExtraido leer(Fuente fuente)throws IOException{
        Path archivo=raiz.resolve(fuente.ruta()).normalize();
        if(!archivo.startsWith(raiz) || !Files.isRegularFile(archivo,LinkOption.NOFOLLOW_LINKS)
                || !archivo.toRealPath().startsWith(raiz.toRealPath()))throw new IOException("ADJUNTO_NO_DISPONIBLE");
        byte[] contenido;
        try(InputStream entrada=Files.newInputStream(archivo)){contenido=entrada.readNBytes(10485761);}
        if(contenido.length>10485760)throw new IllegalArgumentException("LIMITE_ARCHIVO");
        if(!huella(contenido).equals(fuente.hash()))throw new IOException("ADJUNTO_INCONSISTENTE");
        return documentos.extraer(new Entrada(fuente.nombre(),fuente.mime(),contenido));
    }
    public static String huella(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private record Entrada(String nombre,String mime,byte[] bytes) implements MultipartFile {
        public String getName(){return "archivo";} public String getOriginalFilename(){return nombre;} public String getContentType(){return mime;}
        public boolean isEmpty(){return bytes.length==0;} public long getSize(){return bytes.length;}
        public byte[] getBytes(){return bytes.clone();} public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        public void transferTo(File destino)throws IOException{Files.write(destino.toPath(),bytes);}
    }
}
