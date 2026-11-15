package com.municipio.base_conocimiento.rag;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.municipio.base_conocimiento.agente.*;
import com.municipio.base_conocimiento.agente.HerramientasAgenteRepository.*;
import com.municipio.base_conocimiento.ia.client.ClienteConversacion;
import com.municipio.base_conocimiento.ia.dto.RespuestaIa;
import com.municipio.base_conocimiento.ia.exception.IaException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AgenteConsultaTest {
 @Test void consultaDocumentalConSolucionPrincipalTambienRequiereAlModelo(){
  var f=new AgenteFixtures();f.cadena();String pregunta="¿Desde qué opción puedo crear un servicio? Mostrame la fuente.";
  var r=f.servicio().consultar(pregunta);
  assertThat(r.estado()).isEqualTo("CON_FUENTES");assertThat(r.modelo()).isEqualTo(ConsultaRagFixtures.ia().modeloAgente());
  verify(f.busqueda).buscar(pregunta);
  verify(f.cliente,times(2)).conversar(eq(ClienteConversacion.Uso.AGENTE),anyList(),anyMap());
 }
 @Test void decideHerramientasYComponeSoloSolucionYPasosLeidos(){
  var f=new AgenteFixtures();f.cadena();var resultado=f.servicio().consultar("¿Cómo corregir E17?");
  assertThat(resultado.estado()).isEqualTo("CON_FUENTES");assertThat(resultado.texto()).contains("principal",ConsultaRagFixtures.EVIDENCIA,"1. Cerrar","2. Abrir");
  assertThat(resultado.herramientas()).extracting(ResultadoAgente.Ejecucion::nombre).containsExactly("buscar_problema","obtener_solucion","obtener_pasos");
  assertThat(resultado.fuentes()).hasSize(3).allSatisfy(s->assertThat(s.enlace()).isEqualTo("/problemas/7"));
  verify(f.cliente,times(2)).conversar(eq(ClienteConversacion.Uso.AGENTE),argThat(m->m.getFirst().contenido().contains("no confiables")),anyMap());
 }
 @Test void rechazaHerramientasArbitrariasIdsAjenosYSchemasInvalidos(){
  for(String texto:List.of("{","null","[]","{\"herramienta\":\"ejecutar_sql\",\"id\":0}","{\"herramienta\":\"buscar_problema\",\"id\":-1}","{\"herramienta\":\"obtener_pasos\",\"id\":8}","{\"herramienta\":2,\"id\":0}","{\"herramienta\":\"buscar_problema\",\"id\":0.5}","{\"herramienta\":\"buscar_problema\",\"id\":999999999999999999999999}","{\"herramienta\":\"buscar_problema\",\"id\":0,\"sql\":\"DROP\"}",
    "{\"herramienta\":\"ejecutar_sql\",\"id\":7,\"herramienta\":\"obtener_solucion\"}","{\"herramienta\":\"obtener_solucion\",\"id\":7} {}")){
   var f=new AgenteFixtures();when(f.cliente.conversar(any(),anyList(),anyMap())).thenReturn(f.salida(texto));
   var r=f.servicio().consultar("E17");assertThat(r.estado()).as(texto).isEqualTo("ACCION_INVALIDA");assertThat(r.texto()).isEqualTo(AgenteConsulta.SIN_SOLUCION);verifyNoInteractions(f.repo);verify(f.busqueda).buscar("E17");
  }
  var f=new AgenteFixtures();when(f.cliente.conversar(any(),anyList(),anyMap())).thenReturn(f.accion("obtener_solucion",999));
  assertThat(f.servicio().consultar("E17").estado()).isEqualTo("ACCION_INVALIDA");verifyNoInteractions(f.repo);
 }
 @Test void abstieneSinResultadosSinSolucionesOAlPedirFinalizar(){
  var f=new AgenteFixtures();f.cadena();when(f.busqueda.buscar(anyString())).thenReturn(new com.municipio.base_conocimiento.rag.service.BusquedaHibridaRag.Seleccion("SIN_COINCIDENCIAS","HIBRIDA",true,List.of()));
  var r=f.servicio().consultar("E17");assertThat(r.fuentes()).isEmpty();assertThat(r.busquedaAcotada()).isTrue();verifyNoInteractions(f.cliente);
  f=new AgenteFixtures();when(f.cliente.conversar(any(),anyList(),anyMap())).thenReturn(f.accion("finalizar",0));
  assertThat(f.servicio().consultar("E17").estado()).isEqualTo("SIN_SOLUCION");
  f=new AgenteFixtures();when(f.repo.soluciones(7)).thenReturn(List.of());when(f.cliente.conversar(any(),anyList(),anyMap())).thenReturn(f.accion("obtener_solucion",7),f.accion("finalizar",0));
  assertThat(f.servicio().consultar("E17").fuentes()).isEmpty();
 }
 @Test void descartaFuentesCambiadasAntesDuranteYDespuesDeLasHerramientas(){
  for(int punto:List.of(1,2,5,6)){
   var f=new AgenteFixtures();f.cadena();var contador=new java.util.concurrent.atomic.AtomicInteger();when(f.vigencia.todas(anyList())).thenAnswer(i->contador.incrementAndGet()!=punto);
   assertThat(f.servicio().consultar("E17").estado()).isEqualTo("FUENTES_ACTUALIZADAS");
  }
  var f=new AgenteFixtures();f.cadena();when(f.repo.pasos(8)).thenReturn(f.pasos,List.of());
  assertThat(f.servicio().consultar("E17").estado()).isEqualTo("FUENTES_ACTUALIZADAS");
  f=new AgenteFixtures();f.cadena();when(f.repo.soluciones(7)).thenReturn(List.of(f.solucion),List.of());
  assertThat(f.servicio().consultar("E17").fuentes()).isEmpty();
 }
 @Test void limitaIteracionesDuplicadosFuentesYContexto(){
  var f=new AgenteFixtures();f.cadena();var servicio=new AgenteConsulta(new ConfiguracionAgente(true,1,Duration.ofSeconds(600)),ConsultaRagFixtures.config(true,true),ConsultaRagFixtures.ia(),f.busqueda,f.vigencia,f.repo,f.cliente,new ObjectMapper());
  assertThat(servicio.consultar("E17").estado()).isEqualTo("LIMITE_ITERACIONES");
  f=new AgenteFixtures();when(f.cliente.conversar(any(),anyList(),anyMap())).thenReturn(f.accion("buscar_problema",0));
  assertThat(f.servicio().consultar("E17").estado()).isEqualTo("ACCION_INVALIDA");
  for(boolean largos:List.of(false,true)){
   f=new AgenteFixtures();f.cadena();when(f.repo.soluciones(7)).thenReturn(largos?List.of(new Solucion(8,7,"Título","x".repeat(1201),true,0)):Collections.nCopies(6,f.solucion));
   assertThat(f.servicio().consultar("E17").estado()).isEqualTo("LIMITE_FUENTES");
  }
  for(boolean largos:List.of(false,true)){
   f=new AgenteFixtures();f.cadena();when(f.repo.pasos(8)).thenReturn(largos?List.of(new Paso(9,8,"Paso","x".repeat(601),0)):Collections.nCopies(21,f.pasos.getFirst()));
   assertThat(f.servicio().consultar("E17").estado()).isEqualTo("LIMITE_FUENTES");
  }
 }
 @Test void validaEntradaConfiguracionProveedoresYFallosSinExponerContenido(){
  var f=new AgenteFixtures();var servicio=f.servicio();
  for(String entrada:Arrays.asList(null," ","x".repeat(1001),"privado\u0000"))assertThatThrownBy(()->servicio.consultar(entrada)).isInstanceOf(IaException.class);
  for(var config:List.of(new ConfiguracionAgente(false,6,Duration.ofSeconds(600)),new ConfiguracionAgente(true,6,Duration.ofNanos(1)))){
   var otro=new AgenteConsulta(config,ConsultaRagFixtures.config(true,true),ConsultaRagFixtures.ia(),f.busqueda,f.vigencia,f.repo,f.cliente,new ObjectMapper());
   assertThatThrownBy(()->otro.consultar("E17")).isInstanceOf(IaException.class);
  }
  for(int caso=0;caso<6;caso++){int c=caso;assertThatThrownBy(()->new ConfiguracionAgente(true,c==0?0:c==1?11:6,c==2?null:c==3?Duration.ZERO:c==4?Duration.ofSeconds(-1):Duration.ofSeconds(901))).isInstanceOf(IllegalArgumentException.class);}
  for(var salida:Arrays.asList(null,new RespuestaIa("otro","{}",1,1),f.salida(null),f.salida("x".repeat(1025)))){
   doReturn(salida).when(f.cliente).conversar(any(),anyList(),anyMap());assertThat(servicio.consultar("E17").estado()).isEqualTo("ACCION_INVALIDA");
  }
  doThrow(new org.springframework.dao.DataAccessResourceFailureException("privado")).when(f.vigencia).todas(anyList());
  assertThatThrownBy(()->servicio.consultar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("AGENTE_FUENTES_NO_DISPONIBLES"));
 }
 @Test void rechazaConsultasConcurrentesYLiberaElTurno()throws Exception{
  var f=new AgenteFixtures();CountDownLatch inicio=new CountDownLatch(1),liberar=new CountDownLatch(1);
  when(f.cliente.conversar(any(),anyList(),anyMap())).thenAnswer(i->{inicio.countDown();liberar.await(10,TimeUnit.SECONDS);return f.salida("{}");});
  var servicio=f.servicio();var primera=CompletableFuture.runAsync(()->servicio.consultar("E17"));
  try{assertThat(inicio.await(5,TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->servicio.consultar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("AGENTE_OCUPADO"));}
  finally{liberar.countDown();}primera.get(5,TimeUnit.SECONDS);assertThat(servicio.consultar("E17").fuentes()).isEmpty();
 }
 @Test void limitaContextoYExigeRagActivoPeroAdmiteUnaSolucionSinPasos(){
  var f=new AgenteFixtures();f.cadena();var base=ConsultaRagFixtures.ia();
  var corta=new com.municipio.base_conocimiento.ia.config.ConfiguracionIa(true,base.url(),base.modeloConversacion(),base.modeloAgente(),base.modeloMultimodal(),base.modeloEmbeddings(),base.timeout(),base.conexionTimeout(),base.contexto(),256,128,8,1048576);
  var sinContexto=new AgenteConsulta(f.config,ConsultaRagFixtures.config(true,true),corta,f.busqueda,f.vigencia,f.repo,f.cliente,new ObjectMapper());
  assertThatThrownBy(()->sinContexto.consultar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("AGENTE_CONTEXTO_INSUFICIENTE"));
  var sinRag=new AgenteConsulta(f.config,ConsultaRagFixtures.config(false,true),base,f.busqueda,f.vigencia,f.repo,f.cliente,new ObjectMapper());
  assertThatThrownBy(()->sinRag.consultar("E17")).isInstanceOfSatisfying(IaException.class,e->assertThat(e.getCodigo()).isEqualTo("RAG_DESHABILITADO"));
  var otra=new Solucion(8,7,"Otra solución","x".repeat(199)+"😀Descripción publicada.",false,1);
  when(f.repo.soluciones(7)).thenReturn(List.of(otra));when(f.repo.pasos(8)).thenReturn(List.of());
  when(f.busqueda.buscar(anyString())).thenReturn(new com.municipio.base_conocimiento.rag.service.BusquedaHibridaRag.Seleccion("COINCIDENCIAS","HIBRIDA",false,List.of(new com.municipio.base_conocimiento.rag.service.BusquedaHibridaRag.Coincidencia(ConsultaRagFixtures.registro(1,"x".repeat(5000),null),1))));
  var r=f.servicio().consultar("E17");assertThat(r.estado()).isEqualTo("CON_FUENTES");assertThat(r.busquedaAcotada()).isTrue();assertThat(r.fuentes()).hasSize(1);assertThat(r.texto()).doesNotContain("principal").contains("😀Descripción publicada.");
 }
}
