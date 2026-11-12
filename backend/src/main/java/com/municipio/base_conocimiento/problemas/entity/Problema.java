package com.municipio.base_conocimiento.problemas.entity;

import com.municipio.base_conocimiento.areas.entity.Subarea;
import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "problemas")
public class Problema extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subarea_id", nullable = false)
    private Subarea subarea;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(name = "descripcion_html", nullable = false, columnDefinition = "TEXT")
    private String descripcionHtml;
    @Column(name = "descripcion_texto", nullable = false, columnDefinition = "TEXT")
    private String descripcionTexto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoProblema estado = EstadoProblema.BORRADOR;
    @Column(nullable = false)
    private boolean activo = true;
    @ManyToMany
    @JoinTable(name = "problema_etiquetas", joinColumns = @JoinColumn(name = "problema_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private Set<Etiqueta> etiquetas = new LinkedHashSet<>();

    protected Problema() {}
    public Problema(Subarea subarea, String titulo, String html, String texto, EstadoProblema estado) {
        this.subarea = subarea; actualizar(titulo, html, texto, estado);
    }
    public void actualizar(String titulo, String html, String texto, EstadoProblema estado) {
        this.titulo = titulo.strip(); this.descripcionHtml = html; this.descripcionTexto = texto; this.estado = estado;
    }
    public void moverA(Subarea subarea) { this.subarea = subarea; }
    public void cambiarEstado(EstadoProblema estado) { this.estado = estado; }
    public void cambiarActivo(boolean activo) { this.activo = activo; }
    public void reemplazarEtiquetas(Set<Etiqueta> etiquetas) { this.etiquetas.clear(); this.etiquetas.addAll(etiquetas); }
    public Long getId() { return id; }
    public Subarea getSubarea() { return subarea; }
    public String getTitulo() { return titulo; }
    public String getDescripcionHtml() { return descripcionHtml; }
    public String getDescripcionTexto() { return descripcionTexto; }
    public EstadoProblema getEstado() { return estado; }
    public boolean isActivo() { return activo; }
    public Set<Etiqueta> getEtiquetas() { return Set.copyOf(etiquetas); }
}
