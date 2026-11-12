package com.municipio.base_conocimiento.archivos.service;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.problemas.dto.ProblemaRequest;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.soluciones.dto.SolucionRequest;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import com.municipio.base_conocimiento.compartido.exception.*;
import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:adjuntos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","app.storage.path=target/test-documents","app.storage.max-image-size=1024"})
@ActiveProfiles("test") @Transactional
class AdjuntosIntegrationTest {
    @Autowired ArchivoService archivos;
    @Autowired AreaService areas;
    @Autowired ProblemaService problemas;
    @Autowired SolucionService soluciones;
    @Test void cargaDescargaRenombraYEliminaSinMezclarImagenes() throws Exception {
        var a=areas.crear(new AreaRequest("Adjuntos",null,0));
        var sub=areas.crearSubarea(a.id(),new SubareaRequest("General",null,0));
        var p=problemas.crear(new ProblemaRequest(sub.id(),"Prueba","<p>Contenido</p>",EstadoProblema.PUBLICADO,Set.of(),null));
        var s=soluciones.crear(p.id(),new SolucionRequest("Solución","<p>Detalle</p>",false,0,null));
        var pdf=archivos.cargarDocumento(ArchivoService.Propietario.PROBLEMA,p.id(),new MockMultipartFile("archivo","manual.pdf","text/plain",DetectorDocumentoTest.pdf()),"Manual",0);
        var word=archivos.cargarDocumento(ArchivoService.Propietario.SOLUCION,s.id(),new MockMultipartFile("archivo","manual.docx","application/octet-stream",DetectorDocumentoTest.docx()),"Instructivo",0);
        assertThat(archivos.listar(ArchivoService.Propietario.PROBLEMA,p.id())).isEmpty();
        assertThat(archivos.listarDocumentos(ArchivoService.Propietario.PROBLEMA,p.id())).extracting("id").containsExactly(pdf.id());
        assertThat(archivos.listarDocumentos(ArchivoService.Propietario.SOLUCION,s.id())).extracting("id").containsExactly(word.id());
        assertThat(archivos.contenido(word.id()).mime()).isEqualTo(DetectorDocumento.DOCX);
        assertThat(archivos.contenido(pdf.id()).bytes()).isEqualTo(DetectorDocumentoTest.pdf());
        var renamed=archivos.actualizar(pdf.id(),"Manual actualizado",0,pdf.version());
        assertThatThrownBy(()->archivos.cambiarActivo(pdf.id(),false,pdf.version())).isInstanceOf(ConflictoException.class);
        archivos.cambiarActivo(pdf.id(),false,renamed.version());
        assertThat(archivos.listarDocumentos(ArchivoService.Propietario.PROBLEMA,p.id())).isEmpty();
        assertThatThrownBy(()->archivos.contenido(pdf.id())).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(()->archivos.cargarDocumento(ArchivoService.Propietario.PROBLEMA,p.id(),new MockMultipartFile("archivo","grande.pdf","application/pdf",new byte[2048]),"Grande",0)).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->archivos.cargarDocumento(ArchivoService.Propietario.PROBLEMA,p.id(),new MockMultipartFile("archivo","vacio.pdf","application/pdf",new byte[0]),"Vacío",0)).isInstanceOf(ArchivoInvalidoException.class);
    }
}
