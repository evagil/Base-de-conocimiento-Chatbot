package com.municipio.base_conocimiento.conocimiento;

import static org.assertj.core.api.Assertions.*;

import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import com.municipio.base_conocimiento.archivos.service.ArchivoService;
import com.municipio.base_conocimiento.archivos.service.ArchivoService.Propietario;
import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"app.storage.path=target/test-storage","app.storage.max-image-size=1024"})
@ActiveProfiles("test") @Transactional
class ConocimientoIntegrationTest {
    @Autowired AreaService areas; @Autowired ProblemaService problemas; @Autowired SolucionService soluciones; @Autowired ArchivoService archivos;

    @Test void flujoCompletoConMultiplesSolucionesPasosHistorialEImagenes() {
        var area=areas.crear(new AreaRequest("Sistemas",null,2));
        var sub=areas.crearSubarea(area.id(),new SubareaRequest("Soporte",null,1));
        var creado=problemas.crear(new ProblemaRequest(sub.id(),"Error de acceso","<h2>Error</h2><script>alert(1)</script><p>Sin acceso</p>",EstadoProblema.BORRADOR,Set.of("Acceso","Urgente"),null));
        assertThat(creado.descripcionHtml()).doesNotContain("script").contains("Sin acceso");
        assertThat(problemas.listarEtiquetas()).containsExactlyInAnyOrder("Acceso","Urgente");
        assertThat(problemas.listar(null,null,PageRequest.of(0,10))).hasSize(1);
        assertThat(problemas.listar(sub.id(),null,PageRequest.of(0,10))).hasSize(1);
        assertThat(problemas.listar(null,true,PageRequest.of(0,10))).hasSize(1);
        assertThat(problemas.listar(sub.id(),true,PageRequest.of(0,10))).hasSize(1);
        var editado=problemas.actualizar(creado.id(),new ProblemaRequest(sub.id(),"Error de acceso actualizado","<p>Detalle seguro</p>",EstadoProblema.BORRADOR,Set.of("Acceso"),"Corrección"),problemas.obtener(creado.id()).problema().version());
        assertThat(editado.estado()).isEqualTo(EstadoProblema.BORRADOR);
        problemas.cambiarEstado(creado.id(),EstadoProblema.PUBLICADO,problemas.obtener(creado.id()).problema().version()); problemas.cambiarActivo(creado.id(),true,problemas.obtener(creado.id()).problema().version());

        var s1=soluciones.crear(creado.id(),new SolucionRequest("Reiniciar","<p>Reiniciar sesión</p>",true,1,null));
        var s2=soluciones.crear(creado.id(),new SolucionRequest("Alternativa","<p>Limpiar caché</p>",true,2,null));
        assertThat(soluciones.obtener(s1.id()).principal()).isFalse();
        assertThat(soluciones.obtener(s2.id()).principal()).isTrue();
        soluciones.actualizar(s1.id(),new SolucionRequest("Reiniciar aplicación","<p>Cerrar y abrir</p>",false,1,"Mejora"),soluciones.obtener(s1.id()).version());
        soluciones.marcarPrincipal(s1.id(),soluciones.obtener(s1.id()).version());
        var paso=soluciones.crearPaso(s1.id(),new PasoRequest("Cerrar","<p>Cierre la aplicación</p>",1));
        soluciones.actualizarPaso(paso.id(),new PasoRequest("Cerrar sesión","<p>Cierre la sesión</p>",1),soluciones.buscarPaso(paso.id()).getVersion());
        soluciones.cambiarActivoPaso(paso.id(),false,soluciones.buscarPaso(paso.id()).getVersion());
        assertThatThrownBy(()->soluciones.crearPaso(s1.id(),new PasoRequest(null,"<p>Duplicado</p>",1))).isInstanceOf(ConflictoException.class);
        soluciones.cambiarActivo(s1.id(),false,soluciones.obtener(s1.id()).version());
        assertThatThrownBy(()->soluciones.marcarPrincipal(s1.id(),soluciones.obtener(s1.id()).version())).isInstanceOf(ConflictoException.class);
        assertThat(problemas.obtener(creado.id()).soluciones()).hasSize(2);

        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        var archivo=archivos.cargar(Propietario.PROBLEMA,creado.id(),new MockMultipartFile("archivo","captura.png","image/png",png),"Captura del error",0);
        assertThat(archivos.listar(Propietario.PROBLEMA,creado.id())).extracting("id").containsExactly(archivo.id());
        assertThat(archivos.contenido(archivo.id()).bytes()).isEqualTo(png);
        archivos.actualizar(archivo.id(),"Captura ampliada",1,archivos.listar(Propietario.PROBLEMA,creado.id()).get(0).version()); archivos.cambiarActivo(archivo.id(),false,archivos.listar(Propietario.PROBLEMA,creado.id()).get(0).version());
        assertThatThrownBy(()->archivos.contenido(archivo.id())).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test void rechazaContenidoYArchivosInvalidos() {
        var area=areas.crear(new AreaRequest("Rentas pruebas",null,1)); var sub=areas.crearSubarea(area.id(),new SubareaRequest("Catastro pruebas",null,1));
        assertThatThrownBy(()->problemas.crear(new ProblemaRequest(sub.id(),"Vacío","<script>x</script>",EstadoProblema.BORRADOR,Set.of(),null))).isInstanceOf(ConflictoException.class);
        var problema=problemas.crear(new ProblemaRequest(sub.id(),"Válido","<p>Contenido</p>",EstadoProblema.BORRADOR,null,null));
        assertThatThrownBy(()->archivos.cargar(Propietario.PROBLEMA,problema.id(),new MockMultipartFile("a","mal.exe","application/octet-stream",new byte[]{1,2,3}),"Mal",0)).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->archivos.cargar(Propietario.PROBLEMA,problema.id(),new MockMultipartFile("a","vacío.png","image/png",new byte[0]),"Vacío",0)).isInstanceOf(ArchivoInvalidoException.class);
        assertThatThrownBy(()->archivos.cargar(Propietario.PROBLEMA,99999L,new MockMultipartFile("a","x.png","image/png",new byte[]{1}),"No existe",0)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
