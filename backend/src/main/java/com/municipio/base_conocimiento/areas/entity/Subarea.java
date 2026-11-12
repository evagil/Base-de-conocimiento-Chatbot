package com.municipio.base_conocimiento.areas.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subareas")
public class Subarea extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_normalizado", nullable = false, length = 100)
    private String nombreNormalizado;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    protected Subarea() {}

    public Subarea(Area area, String nombre, String nombreNormalizado, String descripcion, int orden) {
        this.area = area;
        actualizar(nombre, nombreNormalizado, descripcion, orden);
    }

    public void actualizar(String nombre, String nombreNormalizado, String descripcion, int orden) {
        this.nombre = nombre.strip();
        this.nombreNormalizado = nombreNormalizado;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        this.orden = orden;
    }

    public void cambiarEstado(boolean activo) { this.activo = activo; }
    public Long getId() { return id; }
    public Area getArea() { return area; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getOrden() { return orden; }
    public boolean isActivo() { return activo; }
}
