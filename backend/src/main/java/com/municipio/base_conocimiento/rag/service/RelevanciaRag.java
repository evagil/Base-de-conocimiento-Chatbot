package com.municipio.base_conocimiento.rag.service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RelevanciaRag {
    private static final Set<String> VACIAS=Set.of("que","como","para","con","los","las","una","del","por","cual","puedo","hacer","de","el","la","y","en","un","es","se","al","me","mi","a","o","si");
    private static final Pattern CODIGO=Pattern.compile("(?i)\\b[a-z]{1,8}[-_]?\\d{1,8}\\b");
    public String normalizar(String texto){return Normalizer.normalize(texto,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
    public List<String> terminos(String texto){return Arrays.stream(normalizar(texto).split("[^\\p{L}\\p{N}_-]+"))
            .filter(t->t.length()>1 && !VACIAS.contains(t)).distinct().limit(20).toList();}
    public double textual(List<String> terminos,String texto){
        Set<String> palabras=new HashSet<>(Arrays.asList(normalizar(texto).split("[^\\p{L}\\p{N}_-]+")));
        return terminos.isEmpty()?0:(double)terminos.stream().filter(palabras::contains).count()/terminos.size();
    }
    public boolean coincideCodigo(String pregunta,String texto){
        var matcher=CODIGO.matcher(pregunta);List<String> codigos=new ArrayList<>();
        while(matcher.find())codigos.add(matcher.group().toLowerCase(Locale.ROOT));
        return codigos.isEmpty() || codigos.stream().anyMatch(c->Pattern.compile("(?i)(?<![\\p{L}\\p{N}_-])"+Pattern.quote(c)+"(?![\\p{L}\\p{N}_-])").matcher(texto).find());
    }
    public double coseno(List<Double> a,List<Double> b){
        if(a==null || b==null || a.isEmpty() || a.size()!=b.size() || a.size()>8192)return -1;
        double maxA=0,maxB=0;
        for(int i=0;i<a.size();i++){
            if(a.get(i)==null || b.get(i)==null || !Double.isFinite(a.get(i)) || !Double.isFinite(b.get(i)))return -1;
            maxA=Math.max(maxA,Math.abs(a.get(i)));maxB=Math.max(maxB,Math.abs(b.get(i)));
        }
        if(maxA==0 || maxB==0)return -1;
        double producto=0,normaA=0,normaB=0;
        for(int i=0;i<a.size();i++){double x=a.get(i)/maxA,y=b.get(i)/maxB;producto+=x*y;normaA+=x*x;normaB+=y*y;}
        return Math.max(-1,Math.min(1,producto/Math.sqrt(normaA*normaB)));
    }
}
