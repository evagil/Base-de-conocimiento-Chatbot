package com.municipio.base_conocimiento.archivos.dto;
import java.time.Instant;
public record ArchivoResponse(Long id,String nombreOriginal,String tipoMime,long tamanioBytes,String hashSha256,
        String textoAlternativo,int orden,boolean activo,Instant creadoEn,String urlContenido,long version){}
