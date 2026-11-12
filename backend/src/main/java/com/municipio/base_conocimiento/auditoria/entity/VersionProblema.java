package com.municipio.base_conocimiento.auditoria.entity;

import com.municipio.base_conocimiento.problemas.entity.Problema;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "versiones_problema")
public class VersionProblema {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "problema_id") private Problema problema;
    @Column(name = "numero_version", nullable = false) private long numeroVersion;
    @Column(name = "contenido_anterior", nullable = false, columnDefinition = "TEXT") private String contenidoAnterior;
    @Column(length = 300) private String motivo;
    @Column(name = "creado_en", nullable = false) private Instant creadoEn;
    protected VersionProblema() {}
    public VersionProblema(Problema problema, String contenido, String motivo) {
        this.problema = problema; this.numeroVersion = problema.getVersion(); this.contenidoAnterior = contenido;
        this.motivo = motivo == null || motivo.isBlank() ? null : motivo.strip(); this.creadoEn = Instant.now();
    }
}
