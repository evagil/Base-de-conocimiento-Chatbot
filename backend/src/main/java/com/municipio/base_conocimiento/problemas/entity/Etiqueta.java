package com.municipio.base_conocimiento.problemas.entity;

import com.municipio.base_conocimiento.compartido.persistence.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "etiquetas")
public class Etiqueta extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60)
    private String nombre;
    @Column(name = "nombre_normalizado", nullable = false, unique = true, length = 60)
    private String nombreNormalizado;
    @Column(nullable = false)
    private boolean activo = true;

    protected Etiqueta() {}
    public Etiqueta(String nombre, String normalizado) { this.nombre = nombre.strip(); this.nombreNormalizado = normalizado; }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
