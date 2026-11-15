package com.municipio.base_conocimiento.rag.service;

import java.util.*;

/** Propone ventanas literales; la selección final sigue siendo del modelo. */
public final class ExtractosCandidatos {
 private ExtractosCandidatos(){}
 public static List<String> preparar(String texto,String pregunta){
  var opciones=new LinkedHashSet<String>();
  for(String parte:texto.split("(?<=[.!?])\\s+|\\R+")){
   String limpio=parte.strip();
   for(int inicio=0;inicio<limpio.length();){
    int fin=Math.min(limpio.length(),inicio+160);if(fin<limpio.length() && Character.isLowSurrogate(limpio.charAt(fin)))fin--;
    if(fin<limpio.length()){int espacio=limpio.lastIndexOf(' ',fin);if(espacio>inicio+80)fin=espacio;}
    String ventana=limpio.substring(inicio,fin).strip();if(ventana.length()>=15)opciones.add(ventana);
    if(fin==limpio.length() || opciones.size()>=32)break;
    inicio=Math.max(inicio+1,fin-30);if(Character.isLowSurrogate(limpio.charAt(inicio)))inicio++;
   }
   if(opciones.size()>=32)break;
  }
  var relevancia=new RelevanciaRag();var terminos=relevancia.terminos(pregunta);
  return opciones.stream().sorted(Comparator.comparingDouble((String s)->relevancia.textual(terminos,s)).reversed()).limit(3).toList();
 }
}
