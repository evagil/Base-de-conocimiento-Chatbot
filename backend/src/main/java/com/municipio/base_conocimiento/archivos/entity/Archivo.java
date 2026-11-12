package com.municipio.base_conocimiento.archivos.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.*;

@Entity @Table(name="archivos")
public class Archivo extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="nombre_original",nullable=false,length=255) private String nombreOriginal;
    @Column(name="nombre_interno",nullable=false,unique=true,length=100) private String nombreInterno;
    @Column(name="tipo_mime",nullable=false,length=150) private String tipoMime;
    @Column(name="tamanio_bytes",nullable=false) private long tamanioBytes;
    @Column(name="ruta_relativa",nullable=false,unique=true,length=500) private String rutaRelativa;
    @Column(name="hash_sha256",nullable=false,length=64) private String hashSha256;
    @Column(name="texto_alternativo",nullable=false,length=300) private String textoAlternativo;
    @Column(nullable=false) private int orden;
    @Column(nullable=false) private boolean activo=true;
    protected Archivo() {}
    public Archivo(String original,String interno,String mime,long tamanio,String ruta,String hash,String alternativo,int orden){this.nombreOriginal=original;this.nombreInterno=interno;this.tipoMime=mime;this.tamanioBytes=tamanio;this.rutaRelativa=ruta;this.hashSha256=hash;this.textoAlternativo=alternativo.strip();this.orden=orden;}
    public void actualizar(String alternativo,int orden){this.textoAlternativo=alternativo.strip();this.orden=orden;}
    public void cambiarActivo(boolean activo){this.activo=activo;}
    public Long getId(){return id;} public String getNombreOriginal(){return nombreOriginal;} public String getTipoMime(){return tipoMime;} public long getTamanioBytes(){return tamanioBytes;} public String getRutaRelativa(){return rutaRelativa;} public String getHashSha256(){return hashSha256;} public String getTextoAlternativo(){return textoAlternativo;} public int getOrden(){return orden;} public boolean isActivo(){return activo;}
}
