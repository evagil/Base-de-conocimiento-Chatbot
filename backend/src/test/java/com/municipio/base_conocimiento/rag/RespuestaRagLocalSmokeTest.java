package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.ia.client.*;
import com.municipio.base_conocimiento.ia.config.*;
import com.municipio.base_conocimiento.rag.service.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

/** Verifica selección de evidencia con el modelo real; corpus y vigencia son sintéticos. */
@EnabledIfEnvironmentVariable(named="BC_TEST_OLLAMA_URL",matches="http://(127\\.0\\.0\\.1|localhost):[0-9]+/?")
class RespuestaRagLocalSmokeTest {
 @Test void modeloLocalSeleccionaUnaCitaQueElServidorPuedeVerificar(){
  var ia=new ConfiguracionIa(true,URI.create(System.getenv("BC_TEST_OLLAMA_URL")),"granite4.2:3b","granite4.2:3b","qwen2.5vl:3b","embeddinggemma:300m",
   Duration.ofSeconds(180),Duration.ofSeconds(2),2048,4000,128,8,1048576);
  var busqueda=mock(BusquedaHibridaRag.class);var vigencia=mock(VigenciaFuentesRag.class);
  when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("COINCIDENCIAS","HIBRIDA",false,
   List.of(new BusquedaHibridaRag.Coincidencia(ConsultaRagFixtures.registro(1,ConsultaRagFixtures.EVIDENCIA,null),1))));
  when(vigencia.todas(anyList())).thenReturn(true);
  try(var http=new OllamaConfiguration().ollamaHttpClient(ia)){
   var servicio=new RespuestaConFuentesRag(busqueda,new ClienteOllama(ia,http,new ObjectMapper()),ia,ConsultaRagFixtures.config(true,true),vigencia,new ObjectMapper());
   long inicio=System.nanoTime();var respuesta=servicio.responder("¿Qué debo hacer para corregir E17?");
   assertThat(respuesta.estado()).isEqualTo("CON_FUENTES");assertThat(respuesta.fuentes()).hasSize(1);
   assertThat(ConsultaRagFixtures.EVIDENCIA).contains(respuesta.fuentes().getFirst().extracto());
   assertThat(respuesta.fuentes().getFirst().enlace()).isEqualTo("/problemas/7");
   System.out.printf("RAG_LOCAL modelo=%s fuentes=%d segundos=%.2f%n",respuesta.modelo(),respuesta.fuentes().size(),(System.nanoTime()-inicio)/1e9);
  }
 }
}
