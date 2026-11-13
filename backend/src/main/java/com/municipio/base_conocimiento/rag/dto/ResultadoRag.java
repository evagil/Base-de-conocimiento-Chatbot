package com.municipio.base_conocimiento.rag.dto;

import java.util.List;

public final class ResultadoRag {
    private ResultadoRag() { }
    public record Fuente(long problemaId,Long solucionId,Long pasoId,Long archivoId,String tipo,
            String titulo,Integer pagina,String extracto,String enlace,String enlaceArchivo,double puntuacion) { }
    public record Busqueda(String estado,String modo,boolean acotada,List<Fuente> fuentes) { }
    public record Respuesta(String estado,String texto,String modelo,List<Fuente> fuentes,boolean busquedaAcotada) { }
}
