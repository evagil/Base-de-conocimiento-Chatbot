package com.municipio.base_conocimiento.archivos.privacidad;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/** Analiza posiciones OCR; nunca genera texto ni envía imágenes fuera del equipo. */
public final class MotorProteccionImagen {
 public record Palabra(int x,int y,int ancho,int alto,String texto,String linea){}
 public record Zona(int x,int y,int ancho,int alto){}
 private static final Pattern IDENTIDAD=Pattern.compile("(?iu)\\b(contribuyentes?|proveedor(?:es)?|razon\\s*social|apellidos?|nombres?|denominacion|titular(?:es)?|cuit|cuil|dni|documento|cliente|beneficiario)\\b");
 private static final Pattern IMPORTE=Pattern.compile("(?iu).*(?:[$€£]|\\d[\\d.,]*[.,]\\d{2}\\b|\\b(?:ars|usd)\\b).*");
 private static final Pattern COLUMNA=Pattern.compile("(?iu)\\b(importes?|montos?|total(?:es)?|saldos?|precios?|deudas?)\\b");
 public List<Palabra> leerTsv(String tsv,int ancho,int alto){
  if(tsv==null || tsv.length()>2097152)throw new IllegalArgumentException("OCR inválido");
  var salida=new ArrayList<Palabra>();String[] lineas=tsv.split("\\R",-1);
  if(lineas.length>20000 || !lineas[0].startsWith("level\tpage_num\t"))throw new IllegalArgumentException("OCR inválido");
  for(int i=1;i<lineas.length;i++){
   if(lineas[i].isBlank())continue;
   String[] c=lineas[i].split("\t",12);if(c.length!=12)throw new IllegalArgumentException("OCR inválido");
   if(!c[0].equals("5") || c[11].isBlank())continue;
   try{
    int x=Integer.parseInt(c[6]),y=Integer.parseInt(c[7]),w=Integer.parseInt(c[8]),h=Integer.parseInt(c[9]);
    if(x<0 || y<0 || w<=0 || h<=0 || (long)x+w>ancho || (long)y+h>alto)throw new IllegalArgumentException("OCR fuera de imagen");
    salida.add(new Palabra(x,y,w,h,c[11],String.join("/",Arrays.copyOfRange(c,1,5))));
   }catch(NumberFormatException e){throw new IllegalArgumentException("OCR inválido");}
  }
  return List.copyOf(salida);
 }
 public List<Zona> detectar(List<Palabra> palabras,int ancho,int alto){
  var zonas=new ArrayList<Zona>();var lineas=new ArrayList<List<Palabra>>();
  for(var p:palabras){
   List<Palabra> fila=null;
   for(var candidata:lineas){var referencia=candidata.getFirst();if(Math.abs((p.y()+p.alto()/2.0)-(referencia.y()+referencia.alto()/2.0))<=Math.max(3,Math.min(p.alto(),referencia.alto())/2.0)){fila=candidata;break;}}
   if(fila==null){fila=new ArrayList<>();lineas.add(fila);}fila.add(p);
   if(IMPORTE.matcher(p.texto()).matches())zonas.add(zona(p.x(),p.y(),p.ancho(),p.alto(),ancho,alto));
  }
  for(var fila:lineas){
   fila.sort(Comparator.comparingInt(Palabra::x));String texto=normalizar(String.join(" ",fila.stream().map(Palabra::texto).toList()));
   if(texto.matches("(?iu).*(?:[$€£]|\\b(?:ars|usd)\\b).*") || COLUMNA.matcher(texto).find()){
    for(var dato:fila)if(dato.texto().matches("[()\\-+\\d.,]+"))zonas.add(zona(dato.x(),dato.y(),dato.ancho(),dato.alto(),ancho,alto));
   }
   if(IDENTIDAD.matcher(texto).find()){
    int x=fila.getFirst().x(),y=fila.stream().mapToInt(Palabra::y).min().orElseThrow();
    int fin=fila.stream().mapToInt(p->p.y()+p.alto()).max().orElseThrow();
    // Oculta la fila de identidad y una posible fila de valor debajo de la etiqueta.
    zonas.add(zona(x,y,ancho-x,Math.min(alto-y,(fin-y)*3),ancho,alto));
   }
   for(var p:fila)if(COLUMNA.matcher(normalizar(p.texto())).find()){
    // Columnas tabulares: oculta también cifras sin símbolo monetario bajo su cabecera.
    for(var dato:palabras)if(dato.y()>p.y()+p.alto() && Math.abs(dato.x()-p.x())<Math.max(100,p.ancho()*2)
      && dato.texto().matches("[()\\-+\\d.,$€%]+"))zonas.add(zona(dato.x(),dato.y(),dato.ancho(),dato.alto(),ancho,alto));
   }
  }
  return List.copyOf(zonas);
 }
 public BufferedImage ocultar(BufferedImage original,List<Zona> zonas){
  var salida=new BufferedImage(original.getWidth(),original.getHeight(),BufferedImage.TYPE_INT_RGB);var g=salida.createGraphics();
  try{g.setColor(Color.WHITE);g.fillRect(0,0,salida.getWidth(),salida.getHeight());g.drawImage(original,0,0,null);
   g.setColor(new Color(190,194,199));for(var z:zonas)g.fillRect(z.x(),z.y(),z.ancho(),z.alto());
  }finally{g.dispose();}return salida;
 }
 private Zona zona(int x,int y,int w,int h,int ancho,int alto){int izquierda=Math.max(0,x-5),arriba=Math.max(0,y-4);return new Zona(izquierda,arriba,Math.min(ancho-izquierda,w+10),Math.min(alto-arriba,h+8));}
 private String normalizar(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT);}
}
