package com.municipio.base_conocimiento.rag.service;

import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Fragmentador {
    private final ConfiguracionIndice config;
    public Fragmentador(ConfiguracionIndice config){this.config=config;}
    public List<String> dividir(String original){
        if(original==null) return List.of();
        String texto=Normalizer.normalize(original,Normalizer.Form.NFC).replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").replaceAll("\\s+"," ").strip();
        List<String> partes=new ArrayList<>();
        for(int inicio=0;inicio<texto.length();){
            if(partes.size()>=config.maxFragmentos()) throw new IllegalArgumentException("LIMITE_FRAGMENTOS");
            int fin=Math.min(inicio+config.tamanoFragmento(),texto.length());
            if(fin<texto.length() && Character.isLowSurrogate(texto.charAt(fin))) fin--;
            if(fin<texto.length()) {int espacio=texto.lastIndexOf(' ',fin);if(espacio>inicio+config.tamanoFragmento()/2) fin=espacio;}
            String parte=texto.substring(inicio,fin).strip();
            if(!parte.isEmpty()) partes.add(parte);
            if(fin==texto.length())break;
            inicio=Math.max(inicio+1,fin-config.solapamiento());
            if(Character.isLowSurrogate(texto.charAt(inicio)))inicio++;
        }
        return List.copyOf(partes);
    }
}
