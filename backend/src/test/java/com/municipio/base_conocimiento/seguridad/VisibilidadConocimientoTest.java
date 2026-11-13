package com.municipio.base_conocimiento.seguridad;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.problemas.entity.*;
import com.municipio.base_conocimiento.seguridad.service.*;
import com.municipio.base_conocimiento.soluciones.entity.*;
import org.junit.jupiter.api.Test;

class VisibilidadConocimientoTest {
    @Test void heredaVisibilidadYPermiteElHistorialSoloAFuncionesDeEdicion() {
        var seguridad = mock(SeguridadActual.class);
        var politica = new VisibilidadConocimiento(seguridad);
        var problema = new Problema(null, "Error", "<p>Error</p>", "Error", EstadoProblema.PUBLICADO);
        var solucion = new Solucion(problema, "Solución", "<p>Acción</p>", "Acción", false, 0);
        var paso = new PasoSolucion(solucion, "Paso", "<p>Acción</p>", "Acción", 1);
        when(seguridad.esLector()).thenReturn(true);
        assertThatCode(() -> politica.verificar(problema)).doesNotThrowAnyException();
        assertThatCode(() -> politica.verificar(solucion)).doesNotThrowAnyException();
        assertThatCode(() -> politica.verificar(paso)).doesNotThrowAnyException();
        paso.cambiarActivo(false);
        assertThatThrownBy(() -> politica.verificar(paso)).isInstanceOf(RecursoNoEncontradoException.class);
        paso.cambiarActivo(true);
        solucion.cambiarActivo(false);
        assertThat(politica.visible(paso)).isFalse();
        assertThatThrownBy(() -> politica.verificar(solucion)).isInstanceOf(RecursoNoEncontradoException.class);
        solucion.cambiarActivo(true);
        problema.cambiarEstado(EstadoProblema.BORRADOR);
        assertThat(politica.visible(solucion)).isFalse();
        assertThatThrownBy(() -> politica.verificar(problema)).isInstanceOf(RecursoNoEncontradoException.class);
        problema.cambiarEstado(EstadoProblema.PUBLICADO);
        problema.cambiarActivo(false);
        assertThat(politica.visible(problema)).isFalse();
        when(seguridad.esLector()).thenReturn(false);
        assertThatCode(() -> politica.verificar(problema)).doesNotThrowAnyException();
        assertThatCode(() -> politica.verificar(solucion)).doesNotThrowAnyException();
        assertThatCode(() -> politica.verificar(paso)).doesNotThrowAnyException();
    }
}
