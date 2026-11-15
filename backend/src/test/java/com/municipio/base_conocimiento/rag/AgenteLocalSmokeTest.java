package com.municipio.base_conocimiento.rag;
import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.agente.*;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.config.OllamaConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.nio.file.*;
import java.util.*;
@EnabledIfEnvironmentVariable(named="BC_TEST_OLLAMA_URL",matches="http://(127\\.0\\.0\\.1|localhost):[0-9]+/?")
class AgenteLocalSmokeTest {
 @Test void modeloLocalEligeSolucionYPasosTrasBusquedaAutomatica() throws Exception {
  var f=new AgenteFixtures();var base=ConsultaRagFixtures.ia();
  var ia=new com.municipio.base_conocimiento.ia.config.ConfiguracionIa(true,java.net.URI.create(System.getenv("BC_TEST_OLLAMA_URL")),base.modeloConversacion(),base.modeloAgente(),base.modeloMultimodal(),base.modeloEmbeddings(),base.timeout(),base.conexionTimeout(),base.contexto(),base.maxPromptChars(),base.maxOutputTokens(),base.maxBatch(),base.maxResponseBytes());
  try(var http=new OllamaConfiguration().ollamaHttpClient(ia)){
   var mediciones=new ArrayList<Map<String,Object>>();
   for(int segundos:List.of(0,120)){
   var proveedor=new ClienteOllama(ia,http,new ObjectMapper(),Duration.ofSeconds(segundos));
   var llamadas=new ArrayList<Long>();
   com.municipio.base_conocimiento.ia.client.ClienteConversacion cliente=(uso,mensajes,esquema)->{
    long inicioLlamada=System.nanoTime();var respuesta=proveedor.conversar(uso,mensajes,esquema);
    llamadas.add((System.nanoTime()-inicioLlamada)/1_000_000);return respuesta;
   };
   var agente=new AgenteConsulta(f.config,ConsultaRagFixtures.config(true,true),ia,f.busqueda,f.vigencia,f.repo,cliente,new ObjectMapper());
   long inicio=System.nanoTime();var r=agente.consultar("¿Qué solución y pasos debo seguir para corregir E17?");
   assertThat(r.estado()).isEqualTo("CON_FUENTES");assertThat(r.herramientas()).extracting(ResultadoAgente.Ejecucion::nombre).containsExactly("buscar_problema","obtener_solucion","obtener_pasos");
   assertThat(r.texto()).contains(ConsultaRagFixtures.EVIDENCIA,"1. Cerrar","2. Abrir");
   long duracion=(System.nanoTime()-inicio)/1_000_000;
   assertThat(llamadas).hasSize(2);
   mediciones.add(Map.of("keepAliveSegundos",segundos,"duracionMs",duracion,"llamadasMs",List.copyOf(llamadas),"estado",r.estado()));
   System.out.printf("AGENTE_LOCAL modelo=%s herramientas=%d inferencias=%d keep_alive=%d segundos=%.2f%n",r.modelo(),r.herramientas().size(),llamadas.size(),segundos,duracion/1000.0);
   }
   Path informe=Path.of("..", ".local", "rendimiento-agente.json");Files.createDirectories(informe.getParent());
   Files.writeString(informe,new ObjectMapper().writeValueAsString(Map.of("fecha",java.time.Instant.now().toString(),"modelo",ia.modeloAgente(),"mediciones",mediciones)));
  }
 }
}
