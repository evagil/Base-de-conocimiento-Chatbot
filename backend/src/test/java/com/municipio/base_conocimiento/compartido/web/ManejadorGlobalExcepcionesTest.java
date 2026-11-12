package com.municipio.base_conocimiento.compartido.web;
import static org.assertj.core.api.Assertions.assertThat;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import com.municipio.base_conocimiento.compartido.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
class ManejadorGlobalExcepcionesTest {
    private final ManejadorGlobalExcepciones h=new ManejadorGlobalExcepciones();
    @Test void traduceExcepcionesDeDominio(){assertThat(h.noEncontrado(new RecursoNoEncontradoException("no")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);assertThat(h.conflicto(new ConflictoException("C","c")).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);assertThat(h.archivoInvalido(new ArchivoInvalidoException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"A","a")).getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);}
    @Test void informaBloqueosSinExponerSql(){
        assertThat(h.operacionEnCurso(new org.springframework.dao.CannotAcquireLockException("SQL interno")).getBody().codigo()).isEqualTo("OPERACION_EN_CURSO");
        assertThat(h.versionDesactualizada(new ObjectOptimisticLockingFailureException("X",1L)).getBody().codigo()).isEqualTo("VERSION_DESACTUALIZADA");
    }
    @Test void ocultaDetallesDeIntegridadYConcurrencia(){assertThat(h.integridad(new DataIntegrityViolationException("interno")).getBody().mensaje()).doesNotContain("interno");assertThat(h.versionDesactualizada(new ObjectOptimisticLockingFailureException("X",1L)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);}
}
