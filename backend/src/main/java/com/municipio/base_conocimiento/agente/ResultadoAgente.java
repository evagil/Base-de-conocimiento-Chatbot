package com.municipio.base_conocimiento.agente;
import java.util.List;
import com.municipio.base_conocimiento.rag.dto.ResultadoRag.Fuente;
public record ResultadoAgente(String estado,String texto,String modelo,List<Fuente> fuentes,boolean busquedaAcotada,List<Ejecucion> herramientas) {
 public record Ejecucion(String nombre,long id,String estado){}
}
