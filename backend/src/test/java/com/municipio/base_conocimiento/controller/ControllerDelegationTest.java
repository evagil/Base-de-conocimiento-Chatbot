package com.municipio.base_conocimiento.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.municipio.base_conocimiento.archivos.controller.ArchivoController;
import com.municipio.base_conocimiento.archivos.dto.ArchivoResponse;
import com.municipio.base_conocimiento.archivos.service.ArchivoService;
import com.municipio.base_conocimiento.areas.controller.*;
import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.problemas.controller.ProblemaController;
import com.municipio.base_conocimiento.problemas.controller.EtiquetaController;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.soluciones.controller.SolucionController;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

class ControllerDelegationTest {
    @Test void delegaEndpointsDeAreasYSubareas() {
        AreaService s=mock(AreaService.class); AreaController a=new AreaController(s); SubareaController sub=new SubareaController(s);
        var ar=new AreaResponse(1L,"A",null,0,true,Instant.now(),Instant.now(),0); var sr=new SubareaResponse(2L,1L,"A","S",null,0,true,Instant.now(),Instant.now(),0);
        when(s.crear(any())).thenReturn(ar); when(s.obtener(1L)).thenReturn(ar); when(s.actualizar(eq(1L),any(),eq(0L))).thenReturn(ar); when(s.cambiarEstado(1L,false,0L)).thenReturn(ar);
        when(s.crearSubarea(eq(1L),any())).thenReturn(sr); when(s.listarSubareas(1L,true)).thenReturn(List.of(sr)); when(s.obtenerSubarea(2L)).thenReturn(sr); when(s.actualizarSubarea(eq(2L),any(),eq(0L))).thenReturn(sr); when(s.cambiarEstadoSubarea(2L,false,0L)).thenReturn(sr);
        when(s.listar(eq(true),any())).thenReturn(Page.empty());
        a.listar(true,PageRequest.of(0,10)); a.obtener(1L); a.crear(new AreaRequest("A",null,0)); a.actualizar(1L,new AreaRequest("A",null,0),0L); a.cambiarEstado(1L,new EstadoActivoRequest(false),0L); a.listarSubareas(1L,true); a.crearSubarea(1L,new SubareaRequest("S",null,0));
        sub.obtener(2L); sub.actualizar(2L,new SubareaRequest("S",null,0),0L); sub.cambiarEstado(2L,new EstadoActivoRequest(false),0L);
    }

    @Test void delegaEndpointsDeProblemasYSoluciones() {
        ProblemaService ps=mock(ProblemaService.class); ProblemaController pc=new ProblemaController(ps);
        var pr=new ProblemaResponse(1L,2L,"S",3L,"A","P","<p>x</p>",EstadoProblema.BORRADOR,true,Set.of(),Instant.now(),Instant.now(),0);
        var pd=new ProblemaDetalleResponse(pr,List.of()); var req=new ProblemaRequest(2L,"P","<p>x</p>",EstadoProblema.BORRADOR,Set.of(),null);
        when(ps.crear(any())).thenReturn(pr);when(ps.obtener(1L)).thenReturn(pd);when(ps.actualizar(eq(1L),any(),eq(0L))).thenReturn(pr);when(ps.cambiarEstado(1L,EstadoProblema.PUBLICADO,0L)).thenReturn(pr);when(ps.cambiarActivo(1L,false,0L)).thenReturn(pr);when(ps.listar(any(),any(),any())).thenReturn(Page.empty());
        pc.listar(null,null,PageRequest.of(0,10));pc.obtener(1L);pc.crear(req);pc.actualizar(1L,req,0L);pc.estado(1L,EstadoProblema.PUBLICADO,0L);pc.activo(1L,new EstadoActivoRequest(false),0L);
        when(ps.listarEtiquetas()).thenReturn(List.of("tag"));new EtiquetaController(ps).listar();

        SolucionService ss=mock(SolucionService.class);SolucionController sc=new SolucionController(ss);var paso=new PasoResponse(4L,"Paso","<p>x</p>",1,true,0);var sol=new SolucionResponse(3L,1L,"Sol","<p>x</p>",true,1,true,List.of(paso),Instant.now(),Instant.now(),0);var sreq=new SolucionRequest("Sol","<p>x</p>",true,1,null);var preq=new PasoRequest("Paso","<p>x</p>",1);
        when(ss.listar(1L)).thenReturn(List.of(sol));when(ss.crear(eq(1L),any())).thenReturn(sol);when(ss.obtener(3L)).thenReturn(sol);when(ss.actualizar(eq(3L),any(),eq(0L))).thenReturn(sol);when(ss.marcarPrincipal(3L,0L)).thenReturn(sol);when(ss.cambiarActivo(3L,false,0L)).thenReturn(sol);when(ss.crearPaso(eq(3L),any())).thenReturn(paso);when(ss.actualizarPaso(eq(4L),any(),eq(0L))).thenReturn(paso);when(ss.cambiarActivoPaso(4L,false,0L)).thenReturn(paso);
        sc.listar(1L);sc.crear(1L,sreq);sc.obtener(3L);sc.actualizar(3L,sreq,0L);sc.principal(3L,0L);sc.activo(3L,new EstadoActivoRequest(false),0L);sc.crearPaso(3L,preq);sc.actualizarPaso(4L,preq,0L);sc.activoPaso(4L,new EstadoActivoRequest(false),0L);
    }

    @Test void delegaEndpointsDeArchivos() {
        ArchivoService s=mock(ArchivoService.class);ArchivoController c=new ArchivoController(s);var ar=new ArchivoResponse(1L,"x.png","image/png",3,"abc","alt",0,true,Instant.now(),"/api/archivos/1/contenido",0);var file=new MockMultipartFile("archivo","x.png","image/png",new byte[]{1});
        when(s.cargar(any(),anyLong(),any(),anyString(),anyInt())).thenReturn(ar);when(s.contenido(1L)).thenReturn(new ArchivoService.ContenidoArchivo("image/png","x.png",new byte[]{1}));when(s.actualizar(1L,"alt",1,0L)).thenReturn(ar);when(s.cambiarActivo(1L,false,0L)).thenReturn(ar);
        when(s.listar(any(),eq(1L))).thenReturn(List.of(ar));
        c.problema(1L,file,"alt",0);c.solucion(1L,file,"alt",0);c.paso(1L,file,"alt",0);c.imagenesProblema(1L);c.imagenesSolucion(1L);c.imagenesPaso(1L);c.contenido(1L);c.actualizar(1L,"alt",1,0L);c.activo(1L,new EstadoActivoRequest(false),0L);
    }
}
