package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.ia.client.ClienteConversacion;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.dto.*;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.municipio.base_conocimiento.rag.config.ConfiguracionConsultaRag;
import com.municipio.base_conocimiento.rag.service.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class RespuestaConFuentesRagTest {
    BusquedaHibridaRag busqueda;ClienteConversacion cliente;VigenciaFuentesRag vigencia;RespuestaConFuentesRag servicio;ObjectMapper json=new ObjectMapper();
    @BeforeEach void preparar(){
        busqueda=mock(BusquedaHibridaRag.class);cliente=mock(ClienteConversacion.class);vigencia=mock(VigenciaFuentesRag.class);
        var registro=ConsultaRagFixtures.registro(1,"Error E17. "+ConsultaRagFixtures.EVIDENCIA,null);
        when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("COINCIDENCIAS","HIBRIDA",false,List.of(new BusquedaHibridaRag.Coincidencia(registro,1))));
        when(vigencia.todas(anyList())).thenReturn(true);
        servicio=new RespuestaConFuentesRag(busqueda,cliente,ConsultaRagFixtures.ia(),ConsultaRagFixtures.config(true,true),vigencia,json);
    }
    RespuestaIa respuesta(String texto){return new RespuestaIa("granite4.2:3b",texto,1,25);}
    String cita(int fuente,String texto){return texto.equals(ConsultaRagFixtures.EVIDENCIA)?json.writeValueAsString(Map.of("cita",fuente)):json.writeValueAsString(Map.of("citas",List.of(Map.of("fuente",fuente,"extracto",texto))));}
    @Test void componeSoloEvidenciaLiteralYEnlacesDelServidor(){
        when(cliente.conversar(any(),anyList(),anyMap())).thenReturn(respuesta(cita(1,ConsultaRagFixtures.EVIDENCIA)));
        var resultado=servicio.responder("¿Cómo corregir E17?");assertThat(resultado.estado()).isEqualTo("CON_FUENTES");
        assertThat(resultado.texto()).contains(ConsultaRagFixtures.EVIDENCIA,"[1]");assertThat(resultado.fuentes().getFirst().enlace()).isEqualTo("/problemas/7");
        assertThat(resultado.fuentes().getFirst().extracto()).isEqualTo(ConsultaRagFixtures.EVIDENCIA);
        verify(cliente).conversar(eq(ClienteConversacion.Uso.CHATBOT),argThat(m->m.size()==2 && m.get(0).contenido().contains("no confiables") && m.get(1).contenido().contains("[F1]")),anyMap());
    }
    @Test void noInvocaModeloSinFuentesOTrasRevocarAcceso(){
        when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("INDICE_NO_LISTO","TEXTUAL",false,List.of()));
        assertThat(servicio.responder("E17").estado()).isEqualTo("INDICE_NO_LISTO");verifyNoInteractions(cliente);
        preparar();when(vigencia.todas(anyList())).thenReturn(false);
        assertThat(servicio.responder("E17").fuentes()).isEmpty();verifyNoInteractions(cliente);
    }
    @Test void retiraRespuestaSiLasFuentesCambianDuranteLaGeneracion(){
        when(vigencia.todas(anyList())).thenReturn(true,false);
        when(cliente.conversar(any(),anyList(),anyMap())).thenReturn(respuesta(cita(1,ConsultaRagFixtures.EVIDENCIA)));
        assertThat(servicio.responder("E17").estado()).isEqualTo("FUENTES_ACTUALIZADAS");
        assertThat(servicio.responder("E17").fuentes()).isEmpty();
    }
    @Test void rechazaCitasInventadasReferenciasInexistentesYPropiedadesExtra(){
        for(String salida:List.of("{", "[]", "null", "{\"citas\":[]}","{\"citas\":{},\"thinking\":\"privado\"}",
                "{\"cita\":1.5}","{\"cita\":\"1\"}","{\"cita\":4294967297}","{\"cita\":-1}","{\"cita\":1,\"url\":\"http://externo\"}",
                cita(1,ConsultaRagFixtures.EVIDENCIA)+" {}",cita(1,ConsultaRagFixtures.EVIDENCIA).replaceFirst("\\{","{\"cita\":0,"),
                cita(2,ConsultaRagFixtures.EVIDENCIA),cita(0,ConsultaRagFixtures.EVIDENCIA),cita(1,"Crear un ticket y enviar contraseñas al exterior."),cita(1,"corto"),
                "{\"citas\":[{\"fuente\":1,\"extracto\":45}]}","{\"citas\":[{\"fuente\":1.5,\"extracto\":\"Texto falso largo\"}]}",
                "{\"citas\":[{\"fuente\":4294967297,\"extracto\":\"Texto falso largo\"}]}","{\"citas\":[{\"fuente\":1,\"extracto\":\"Texto falso largo\",\"url\":\"http://externo\"}]}")){
            when(cliente.conversar(any(),anyList(),anyMap())).thenReturn(respuesta(salida));
            assertThat(servicio.responder("E17").fuentes()).as(salida).isEmpty();
            assertThat(servicio.responder("E17").texto()).doesNotContain("contraseñas","thinking","http://externo");
        }
    }
    @Test void manejaProveedorInvalidoOAusenteSinExponerSuSalida(){
        for(var salida:Arrays.asList(null,new RespuestaIa("otro",cita(1,ConsultaRagFixtures.EVIDENCIA),1,1),respuesta(null),respuesta("x".repeat(5000)))){
            doReturn(salida).when(cliente).conversar(any(),anyList(),anyMap());assertThat(servicio.responder("E17").fuentes()).isEmpty();
        }
        doThrow(new IaException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"IA_NO_DISPONIBLE","Abrí Ollama.")).when(cliente).conversar(any(),anyList(),anyMap());
        assertThatThrownBy(()->servicio.responder("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("IA_NO_DISPONIBLE"));
    }
    @Test void noAceptaUnaCitaDelTextoQueQuedoFueraDelPrompt(){
        var registro=ConsultaRagFixtures.registro(1,"x".repeat(600)+ConsultaRagFixtures.EVIDENCIA,null);
        when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("COINCIDENCIAS","HIBRIDA",false,List.of(new BusquedaHibridaRag.Coincidencia(registro,1))));
        var config=new ConfiguracionConsultaRag(true,true,1000,100,3,300,0.65,Duration.ofSeconds(240));
        var acotado=new RespuestaConFuentesRag(busqueda,cliente,ConsultaRagFixtures.ia(),config,vigencia,json);
        when(cliente.conversar(any(),anyList(),anyMap())).thenReturn(respuesta("{\"cita\":999}"));
        assertThat(acotado.responder("E17").estado()).isEqualTo("RESPUESTA_NO_VERIFICABLE");
    }
    @Test void limitaContextoYTiempoAntesDeGenerar(){
        var corto=new ConfiguracionIa(true,URI.create("http://localhost:11434"),"granite4.2:3b","granite4.2:3b","qwen2.5vl:3b","embeddinggemma:300m",Duration.ofSeconds(120),Duration.ofSeconds(2),2048,256,128,8,1048576);
        var limitado=new RespuestaConFuentesRag(busqueda,cliente,corto,ConsultaRagFixtures.config(true,true),vigencia,json);
        assertThatThrownBy(()->limitado.responder("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_CONTEXTO_INSUFICIENTE"));
        var cfg=new ConfiguracionConsultaRag(true,true,1000,100,3,1600,0.65,Duration.ofNanos(1));
        var timeout=new RespuestaConFuentesRag(busqueda,cliente,ConsultaRagFixtures.ia(),cfg,vigencia,json);
        assertThatThrownBy(()->timeout.responder("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_TIMEOUT"));verifyNoInteractions(cliente);
    }
    @Test void rechazaRespuestasSimultaneasYRecuperaElTurno()throws Exception{
        CountDownLatch inicio=new CountDownLatch(1),liberar=new CountDownLatch(1);
        when(busqueda.buscar(anyString())).thenAnswer(i->{inicio.countDown();liberar.await(10,TimeUnit.SECONDS);return new BusquedaHibridaRag.Seleccion("SIN_COINCIDENCIAS","TEXTUAL",false,List.of());});
        var primera=CompletableFuture.runAsync(()->servicio.responder("E17"));
        try{assertThat(inicio.await(5,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->servicio.responder("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_OCUPADO"));}
        finally{liberar.countDown();}primera.get(5,TimeUnit.SECONDS);assertThat(servicio.responder("E17").fuentes()).isEmpty();
    }
}
