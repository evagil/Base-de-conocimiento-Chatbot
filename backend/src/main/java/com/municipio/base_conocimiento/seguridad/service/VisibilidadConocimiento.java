package com.municipio.base_conocimiento.seguridad.service;

import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.entity.Problema;
import com.municipio.base_conocimiento.soluciones.entity.PasoSolucion;
import com.municipio.base_conocimiento.soluciones.entity.Solucion;
import org.springframework.stereotype.Component;

/** Política compartida de lectura. Las escrituras mantienen su autorización existente. */
@Component
public class VisibilidadConocimiento {
    private final SeguridadActual seguridad;

    public VisibilidadConocimiento(SeguridadActual seguridad) {
        this.seguridad = seguridad;
    }

    public boolean soloPublicados() { return seguridad.esLector(); }

    public boolean visible(Problema problema) {
        return problema.isActivo() && problema.getEstado() == EstadoProblema.PUBLICADO;
    }

    public boolean visible(Solucion solucion) {
        return solucion.isActivo() && visible(solucion.getProblema());
    }

    public boolean visible(PasoSolucion paso) {
        return paso.isActivo() && visible(paso.getSolucion());
    }

    public void verificar(Problema problema) { verificar(visible(problema)); }
    public void verificar(Solucion solucion) { verificar(visible(solucion)); }
    public void verificar(PasoSolucion paso) { verificar(visible(paso)); }

    private void verificar(boolean visible) {
        if (soloPublicados() && !visible) {
            throw new RecursoNoEncontradoException("El contenido no está disponible.");
        }
    }
}
