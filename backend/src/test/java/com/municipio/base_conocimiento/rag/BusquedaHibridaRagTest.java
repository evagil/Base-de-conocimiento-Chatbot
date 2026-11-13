package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.ia.client.ClienteEmbeddings;
import com.municipio.base_conocimiento.ia.dto.ResultadoEmbeddings;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository.Registro;
import com.municipio.base_conocimiento.rag.service.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class BusquedaHibridaRagTest {
    BusquedaRagRepository repo;ClienteEmbeddings embeddings;VigenciaFuentesRag vigencia;BusquedaHibridaRag servicio;
    @BeforeEach void preparar(){
        repo=mock(BusquedaRagRepository.class);embeddings=mock(ClienteEmbeddings.class);vigencia=mock(VigenciaFuentesRag.class);
        servicio=new BusquedaHibridaRag(ConsultaRagFixtures.config(true,true),ConsultaRagFixtures.ia(),repo,embeddings,new RelevanciaRag(),vigencia,new ObjectMapper());
        when(repo.disponible()).thenReturn(true);when(vigencia.comprobar(any(),anyMap())).thenReturn(true);when(vigencia.todas(anyList())).thenReturn(true);
        when(embeddings.vectorizar(anyList())).thenReturn(new ResultadoEmbeddings("embeddinggemma:300m",List.of(List.of(1.0,0.0))));
    }
    void recorrer(List<Registro> texto,List<Registro> vectores){when(repo.recorrer(anyInt(),nullable(String.class),nullable(Integer.class),any())).thenAnswer(i->{
        List<Registro> registros=i.getArgument(1)==null?texto:vectores;Consumer<Registro> consumidor=i.getArgument(3);registros.forEach(consumidor);return false;});}
    @Test void combinaRankingsFiltraCodigosYNoExponeVectores(){
        var uno=ConsultaRagFixtures.registro(1,ConsultaRagFixtures.EVIDENCIA,"[1,0]");
        var falso=new Registro(2,9,null,null,null,9,"PROBLEMA","E18",null,"Error E18","x", "embeddinggemma:300m",2,"[1,0]",1);
        recorrer(List.of(uno,falso),List.of(uno,falso));
        var resultado=servicio.buscar("E17");assertThat(resultado.coincidencias()).hasSize(1);
        assertThat(resultado.coincidencias().getFirst().puntuacion()).isEqualTo(1);
        assertThat(resultado.respuesta().fuentes().getFirst().enlace()).isEqualTo("/problemas/7");
        assertThat(resultado.modo()).isEqualTo("HIBRIDA");
    }
    @Test void recuperaSimilitudSinPalabrasIgualesYOmiteVectoresCorruptos(){
        var correcto=ConsultaRagFixtures.registro(1,ConsultaRagFixtures.EVIDENCIA,"[1,0]");
        recorrer(List.of(),List.of(correcto,ConsultaRagFixtures.registro(2,"otro","incorrecto"),ConsultaRagFixtures.registro(3,"otro","[]"),
            ConsultaRagFixtures.registro(4,"otro","[\"uno\",0]"),ConsultaRagFixtures.registro(5,"otro",null),ConsultaRagFixtures.registro(6,"otro","[0,1]")));
        assertThat(servicio.buscar("sesión bloqueada").coincidencias()).hasSize(1);
    }
    @Test void devuelveEstadoVacioSinGenerarYDescartaFuentesQueCambian(){
        when(repo.disponible()).thenReturn(false);assertThat(servicio.buscar("E17").estado()).isEqualTo("INDICE_NO_LISTO");verifyNoInteractions(embeddings);
        when(repo.disponible()).thenReturn(true);recorrer(List.of(ConsultaRagFixtures.registro(1,"E17",null)),List.of());
        when(vigencia.comprobar(any(),anyMap())).thenReturn(false);assertThat(servicio.buscar("E17").coincidencias()).isEmpty();
        when(vigencia.comprobar(any(),anyMap())).thenReturn(true);when(vigencia.todas(anyList())).thenReturn(false);
        assertThat(servicio.buscar("E17").coincidencias()).isEmpty();
    }
    @Test void soportaPostgresTextualYLimiteDeCandidatos(){
        var textual=new BusquedaHibridaRag(ConsultaRagFixtures.config(true,false),ConsultaRagFixtures.ia(),repo,embeddings,new RelevanciaRag(),vigencia,new ObjectMapper());
        when(repo.postgres()).thenReturn(true);when(repo.textual(anyString(),anyInt())).thenReturn(java.util.stream.LongStream.rangeClosed(1,1001).mapToObj(id->ConsultaRagFixtures.registro(id,"E17",null)).toList());
        assertThat(textual.buscar("E17").acotada()).isTrue();verifyNoInteractions(embeddings);
    }
    @Test void validaEntradaVectoresYErroresDelIndice(){
        for(String entrada:Arrays.asList(null," ","x".repeat(1001),"E17\u0000","de el para"))assertThatThrownBy(()->servicio.buscar(entrada)).isInstanceOf(IaException.class);
        for(var resultado:Arrays.asList(null,new ResultadoEmbeddings("otro",List.of(List.of(1.0))),new ResultadoEmbeddings("embeddinggemma:300m",List.of()),
                new ResultadoEmbeddings("embeddinggemma:300m",List.of(List.of(0.0))))){
            doReturn(resultado).when(embeddings).vectorizar(anyList());assertThatThrownBy(()->servicio.buscar("E17")).isInstanceOf(IaException.class);
        }
        when(repo.disponible()).thenThrow(new org.springframework.dao.QueryTimeoutException("Privado"));
        assertThatThrownBy(()->servicio.buscar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_TIMEOUT"));
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("Privado")).when(repo).disponible();
        assertThatThrownBy(()->servicio.buscar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_INDICE_NO_DISPONIBLE"));
    }
    @Test void rechazaBusquedaSimultaneaYLiberaElTurno()throws Exception{
        CountDownLatch inicio=new CountDownLatch(1),liberar=new CountDownLatch(1);
        when(repo.disponible()).thenAnswer(i->{inicio.countDown();liberar.await(10,TimeUnit.SECONDS);return false;});
        var primera=CompletableFuture.runAsync(()->servicio.buscar("E17"));
        try{assertThat(inicio.await(5,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->servicio.buscar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_OCUPADO"));}
        finally{liberar.countDown();}primera.get(5,TimeUnit.SECONDS);
        assertThat(servicio.buscar("E17").estado()).isEqualTo("INDICE_NO_LISTO");
    }
}
