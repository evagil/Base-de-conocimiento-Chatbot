package com.municipio.base_conocimiento.soluciones.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import com.municipio.base_conocimiento.problemas.entity.Problema;
import jakarta.persistence.*;

@Entity
@Table(name = "soluciones")
public class Solucion extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "problema_id", nullable = false)
    private Problema problema;
    @Column(length = 200)
    private String titulo;
    @Column(name = "descripcion_html", nullable = false, columnDefinition = "TEXT")
    private String descripcionHtml;
    @Column(name = "descripcion_texto", nullable = false, columnDefinition = "TEXT")
    private String descripcionTexto;
    @Column(nullable = false) private boolean principal;
    @Column(nullable = false) private int orden;
    @Column(nullable = false) private boolean activo = true;

    protected Solucion() {}
    public Solucion(Problema problema, String titulo, String html, String texto, boolean principal, int orden) {
        this.problema = problema; actualizar(titulo, html, texto, orden); this.principal = principal;
    }
    public void actualizar(String titulo, String html, String texto, int orden) {
        this.titulo = titulo == null || titulo.isBlank() ? null : titulo.strip();
        this.descripcionHtml = html; this.descripcionTexto = texto; this.orden = orden;
    }
    public void marcarPrincipal(boolean principal) { this.principal = principal; }
    public void cambiarActivo(boolean activo) { this.activo = activo; if (!activo) principal = false; }
    public Long getId() { return id; }
    public Problema getProblema() { return problema; }
    public String getTitulo() { return titulo; }
    public String getDescripcionHtml() { return descripcionHtml; }
    public String getDescripcionTexto() { return descripcionTexto; }
    public boolean isPrincipal() { return principal; }
    public int getOrden() { return orden; }
    public boolean isActivo() { return activo; }
}
