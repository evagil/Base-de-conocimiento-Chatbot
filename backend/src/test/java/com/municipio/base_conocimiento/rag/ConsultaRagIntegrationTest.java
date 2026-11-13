package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.dto.*;
import com.municipio.base_conocimiento.rag.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rag_consultas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "app.indice.habilitado=true","app.indice.embeddings=true","app.indice.inicio=99999999","app.rag.habilitado=true"})
@ActiveProfiles("test")
class ConsultaRagIntegrationTest {
 @Autowired JdbcTemplate jdbc; @Autowired IndexadorConocimiento indexador;
 @Autowired BusquedaHibridaRag busqueda; @Autowired RespuestaConFuentesRag respuestas; @Autowired ConfiguracionIa ia;
 @MockitoBean ClienteOllama ollama;
 long problema,area,subarea,solucion; String codigo;
 @BeforeEach void datos(){
  String sufijo=UUID.randomUUID().toString();
  area=insertar("INSERT INTO areas(nombre,nombre_normalizado) VALUES(?,?)","Sistemas "+sufijo,sufijo);
  subarea=insertar("INSERT INTO subareas(area_id,nombre,nombre_normalizado) VALUES(?,?,?)",area,"Soporte",sufijo);
  problema=insertar("INSERT INTO problemas(subarea_id,titulo,descripcion_html,descripcion_texto,estado) VALUES(?,?,?,?,?)",subarea,"Temporal","<p>Detalle</p>","Detalle del incidente","PUBLICADO");
  codigo="T"+(1000+problema);jdbc.update("UPDATE problemas SET titulo=? WHERE id=?","Error "+codigo,problema);
  solucion=insertar("INSERT INTO soluciones(problema_id,titulo,descripcion_html,descripcion_texto) VALUES(?,?,?,?)",problema,"Recuperar "+codigo,"<p>Reiniciar</p>","Cerrar y volver a abrir la aplicación para corregir "+codigo+".");
  when(ollama.vectorizar(anyList())).thenAnswer(i->{List<String> textos=i.getArgument(0);return new ResultadoEmbeddings(ia.modeloEmbeddings(),textos.stream().map(t->List.of(1.0,0.0,0.0)).toList());});
  ejecutar(); clearInvocations(ollama);
 }
 long insertar(String sql,Object... valores){var clave=new GeneratedKeyHolder();jdbc.update(c->{var ps=c.prepareStatement(sql,new String[]{"id"});for(int i=0;i<valores.length;i++)ps.setObject(i+1,valores[i]);return ps;},clave);return clave.getKey().longValue();}
 void ejecutar(){jdbc.update("UPDATE rag_control SET token=NULL,vence_en=NULL,cursor_id=? WHERE id=1",problema-1);assertThat(indexador.procesarSiguiente()).isTrue();}
 @Test void recuperaSoloFuentesPublicadasYDetectaCambiosAntesDeReindexar(){
  var seleccion=busqueda.buscar(codigo);assertThat(seleccion.coincidencias()).hasSize(2);
  assertThat(seleccion.respuesta().fuentes()).allSatisfy(f->{assertThat(f.problemaId()).isEqualTo(problema);assertThat(f.enlace()).isEqualTo("/problemas/"+problema);});
  // Renombrar un padre no cambia la versión del problema: la huella completa debe invalidar igual.
  jdbc.update("UPDATE areas SET nombre='Nueva área' WHERE id=?",area);
  assertThat(busqueda.buscar(codigo).coincidencias()).isEmpty();
  ejecutar(); assertThat(busqueda.buscar(codigo).coincidencias()).hasSize(2);
  jdbc.update("UPDATE soluciones SET activo=FALSE,version=version+1 WHERE id=?",solucion);
  assertThat(busqueda.buscar(codigo).coincidencias()).isEmpty();
  ejecutar();assertThat(busqueda.buscar(codigo).coincidencias()).hasSize(1);
  jdbc.update("UPDATE problemas SET estado='BORRADOR',version=version+1 WHERE id=?",problema);
  assertThat(busqueda.buscar(codigo).coincidencias()).isEmpty();
 }
 @Test void validaCitaLiteralYRetiraUnaFuenteModificadaDuranteLaGeneracion(){
  String cita="Cerrar y volver a abrir la aplicación para corregir "+codigo+".";
  when(ollama.conversar(any(),anyList(),anyMap())).thenAnswer(i->{
   List<MensajeIa> mensajes=i.getArgument(1);String prompt=mensajes.getLast().contenido();
   int inicio=prompt.indexOf(cita);String anterior=prompt.substring(0,inicio);
   int fuente=Integer.parseInt(anterior.substring(anterior.lastIndexOf("[F")+2,anterior.lastIndexOf("]")));
   return new RespuestaIa(ia.modeloConversacion(),"{\"citas\":[{\"fuente\":"+fuente+",\"extracto\":\""+cita+"\"}]}",1,10);
  });
  var respuesta=respuestas.responder(codigo);assertThat(respuesta.estado()).isEqualTo("CON_FUENTES");
  assertThat(respuesta.fuentes()).hasSize(1);assertThat(respuesta.fuentes().getFirst().solucionId()).isEqualTo(solucion);
  assertThat(respuesta.texto()).contains(cita);
  doAnswer(i->{
   jdbc.update("UPDATE subareas SET activo=FALSE WHERE id=?",subarea);
   return new RespuestaIa(ia.modeloConversacion(),"{\"citas\":[]}",1,1);
  }).when(ollama).conversar(any(),anyList(),anyMap());
  var retirada=respuestas.responder(codigo);assertThat(retirada.estado()).isEqualTo("FUENTES_ACTUALIZADAS");assertThat(retirada.fuentes()).isEmpty();
 }
 @Test void sinEvidenciaNoInvocaLaConversacionYNoConfundeCodigos(){
  when(ollama.vectorizar(anyList())).thenReturn(new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of(0.0,1.0,0.0))));
  assertThat(respuestas.responder("ornitorrinco submarino").fuentes()).isEmpty();
  verify(ollama,never()).conversar(any(),anyList(),anyMap());
  when(ollama.vectorizar(anyList())).thenReturn(new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of(1.0,0.0,0.0))));
  assertThat(busqueda.buscar(codigo+"0").coincidencias()).isEmpty();
 }
 @Test void ignoraVectoresCorruptosYNoExponePadresInactivos(){
  jdbc.update("UPDATE rag_fragmentos SET vector_json='no-json' WHERE problema_id=?",problema);
  assertThat(busqueda.buscar(codigo).coincidencias()).hasSize(2); // el texto sigue siendo recuperable
  jdbc.update("UPDATE subareas SET activo=FALSE WHERE id=?",subarea);
  assertThat(busqueda.buscar(codigo).coincidencias()).isEmpty();
 }
}
