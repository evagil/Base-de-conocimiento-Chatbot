package com.municipio.base_conocimiento.compartido.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;

@MappedSuperclass
public abstract class EntidadAuditable {
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "modificado_en", nullable = false)
    private Instant modificadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        creadoEn = ahora;
        modificadoEn = ahora;
    }

    @PreUpdate
    void alModificar() {
        modificadoEn = Instant.now();
    }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getModificadoEn() { return modificadoEn; }
    public void verificarVersion(long esperada) {
        if (esperada < 0 || version != esperada) {
            throw new com.municipio.base_conocimiento.compartido.exception.ConflictoException(
                    "VERSION_DESACTUALIZADA",
                    "Otra persona modificó este registro. Tus cambios no se guardaron. Conservá tu texto y consultá la versión actual antes de volver a editar.");
        }
    }

    public long getVersion() { return version; }
}
