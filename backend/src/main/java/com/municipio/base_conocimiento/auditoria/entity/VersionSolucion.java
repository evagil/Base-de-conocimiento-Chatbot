package com.municipio.base_conocimiento.auditoria.entity;

import com.municipio.base_conocimiento.soluciones.entity.Solucion;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "versiones_solucion")
public class VersionSolucion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "solucion_id") private Solucion solucion;
    @Column(name = "numero_version", nullable = false) private long numeroVersion;
    @Column(name = "contenido_anterior", nullable = false, columnDefinition = "TEXT") private String contenidoAnterior;
    @Column(length = 300) private String motivo;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    protected VersionSolucion() {}
    public VersionSolucion(Solucion solucion, String contenido, String motivo) {
        this.solucion = solucion; this.numeroVersion = solucion.getVersion(); this.contenidoAnterior = contenido;
        this.motivo = motivo == null || motivo.isBlank() ? null : motivo.strip(); this.creadoEn = Instant.now();
    }
}
