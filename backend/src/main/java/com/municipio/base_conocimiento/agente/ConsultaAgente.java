package com.municipio.base_conocimiento.agente;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.*;
import java.util.Map;
public record ConsultaAgente(@NotBlank @Size(max=2000) String pregunta) {
 @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
 public static ConsultaAgente leer(Map<String,Object> datos){
  if(datos==null || datos.size()!=1 || !(datos.get("pregunta") instanceof String texto))throw new IllegalArgumentException("Solo se admite una pregunta textual.");
  return new ConsultaAgente(texto);
 }
 @Override public String toString(){return "ConsultaAgente[pregunta=omitida]";}
}
