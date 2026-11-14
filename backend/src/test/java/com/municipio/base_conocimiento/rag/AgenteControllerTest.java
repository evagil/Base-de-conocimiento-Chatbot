package com.municipio.base_conocimiento.rag;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.municipio.base_conocimiento.agente.*;
import com.municipio.base_conocimiento.compartido.web.ManejadorGlobalExcepciones;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class AgenteControllerTest {
 @Test void exigeJsonTextualValidoSinArchivosYSinCache()throws Exception{
  var agente=mock(AgenteConsulta.class);when(agente.consultar(anyString())).thenReturn(new ResultadoAgente("SIN_SOLUCION",AgenteConsulta.SIN_SOLUCION,null,List.of(),false,List.of()));
  var mvc=MockMvcBuilders.standaloneSetup(new AgenteController(agente)).setControllerAdvice(new ManejadorGlobalExcepciones()).build();
  mvc.perform(post("/api/rag/agente").contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"E17\"}")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.herramientas").isArray());
  mvc.perform(post("/api/rag/agente").contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\" \"}")).andExpect(status().isBadRequest());
  for(String cuerpo:List.of("{}","null","{\"pregunta\":3}","{\"pregunta\":\"E17\",\"imagen\":\"base64\"}","{\"pregunta\":\"E17\",\"herramienta\":\"ejecutar\"}"))
   mvc.perform(post("/api/rag/agente").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest());
  org.assertj.core.api.Assertions.assertThat(new ConsultaAgente("PRIVADO").toString()).doesNotContain("PRIVADO");
  mvc.perform(multipart("/api/rag/agente").file(new org.springframework.mock.web.MockMultipartFile("archivo","captura.jpg","image/jpeg",new byte[]{1}))).andExpect(status().isUnsupportedMediaType());
  verify(agente,times(1)).consultar(anyString());
 }
}
