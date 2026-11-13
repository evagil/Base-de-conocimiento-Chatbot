package com.municipio.base_conocimiento.rag.dto;

import java.time.Instant;
import java.util.List;

/** Snapshot interno. Las rutas físicas nunca se exponen por la API. */
public record FuentesIndice(long problemaId,long areaId,long subareaId,long version,
        String titulo,String area,String subarea,String estado,boolean activo,boolean visible,
        Instant modificado,List<Fuente> fuentes){
    public record Fuente(String tipo,long id,Long solucionId,Long pasoId,Long archivoId,long version,
            Instant modificado,String texto,String ruta,String hash,String nombre,String mime){}
    public record Fragmento(Fuente fuente,Integer pagina,String texto){}
}
