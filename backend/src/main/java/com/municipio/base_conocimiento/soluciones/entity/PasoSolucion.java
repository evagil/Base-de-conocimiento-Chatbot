package com.municipio.base_conocimiento.soluciones.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "pasos_solucion")
public class PasoSolucion extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "solucion_id", nullable = false)
    private Solucion solucion;
    @Column(length = 200) private String titulo;
    @Column(name = "contenido_html", nullable = false, columnDefinition = "TEXT") private String contenidoHtml;
    @Column(name = "contenido_texto", nullable = false, columnDefinition = "TEXT") private String contenidoTexto;
    @Column(nullable = false) private int orden;
    @Column(nullable = false) private boolean activo = true;

    protected PasoSolucion() {}
    public PasoSolucion(Solucion solucion, String titulo, String html, String texto, int orden) {
        this.solucion = solucion; actualizar(titulo, html, texto, orden);
    }
    public void actualizar(String titulo, String html, String texto, int orden) {
        this.titulo = titulo == null || titulo.isBlank() ? null : titulo.strip();
        this.contenidoHtml = html; this.contenidoTexto = texto; this.orden = orden;
    }
    public void cambiarActivo(boolean activo) { this.activo = activo; }
    public Long getId() { return id; }
    public Solucion getSolucion() { return solucion; }
    public String getTitulo() { return titulo; }
    public String getContenidoHtml() { return contenidoHtml; }
    public int getOrden() { return orden; }
    public boolean isActivo() { return activo; }
}
