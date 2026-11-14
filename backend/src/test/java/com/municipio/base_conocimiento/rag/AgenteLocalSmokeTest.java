package com.municipio.base_conocimiento.rag;
import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.agente.*;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.config.OllamaConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;
@EnabledIfEnvironmentVariable(named="BC_TEST_OLLAMA_URL",matches="http://(127\\.0\\.0\\.1|localhost):[0-9]+/?")
class AgenteLocalSmokeTest {
 @Test void modeloLocalEligeLasTresHerramientasConIdsValidos(){
  var f=new AgenteFixtures();var base=ConsultaRagFixtures.ia();
  var ia=new com.municipio.base_conocimiento.ia.config.ConfiguracionIa(true,java.net.URI.create(System.getenv("BC_TEST_OLLAMA_URL")),base.modeloConversacion(),base.modeloAgente(),base.modeloMultimodal(),base.modeloEmbeddings(),base.timeout(),base.conexionTimeout(),base.contexto(),base.maxPromptChars(),base.maxOutputTokens(),base.maxBatch(),base.maxResponseBytes());
  try(var http=new OllamaConfiguration().ollamaHttpClient(ia)){
   var agente=new AgenteConsulta(f.config,ConsultaRagFixtures.config(true,true),ia,f.busqueda,f.vigencia,f.repo,new ClienteOllama(ia,http,new ObjectMapper()),new ObjectMapper());
   long inicio=System.nanoTime();var r=agente.consultar("¿Qué solución y pasos debo seguir para corregir E17?");
   assertThat(r.estado()).isEqualTo("CON_FUENTES");assertThat(r.herramientas()).extracting(ResultadoAgente.Ejecucion::nombre).containsExactly("buscar_problema","obtener_solucion","obtener_pasos");
   assertThat(r.texto()).contains(ConsultaRagFixtures.EVIDENCIA,"1. Cerrar","2. Abrir");
   System.out.printf("AGENTE_LOCAL modelo=%s herramientas=%d segundos=%.2f%n",r.modelo(),r.herramientas().size(),(System.nanoTime()-inicio)/1e9);
  }
 }
}
