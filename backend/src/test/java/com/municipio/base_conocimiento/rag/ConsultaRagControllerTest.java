package com.municipio.base_conocimiento.rag;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.municipio.base_conocimiento.compartido.web.ManejadorGlobalExcepciones;
import com.municipio.base_conocimiento.rag.controller.ConsultaRagController;
import com.municipio.base_conocimiento.rag.service.*;
import com.municipio.base_conocimiento.rag.dto.ResultadoRag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ConsultaRagControllerTest {
 @Test void exponeContratosSinCacheNiDatosInternosYValidaJson() throws Exception {
  var busqueda=mock(BusquedaHibridaRag.class);var respuestas=mock(RespuestaConFuentesRag.class);
  var mvc=MockMvcBuilders.standaloneSetup(new ConsultaRagController(busqueda,respuestas)).setControllerAdvice(new ManejadorGlobalExcepciones()).build();
  when(busqueda.buscar(anyString())).thenReturn(new BusquedaHibridaRag.Seleccion("SIN_COINCIDENCIAS","HIBRIDA",false,List.of()));
  when(respuestas.responder(anyString())).thenReturn(new ResultadoRag.Respuesta("INFORMACION_INSUFICIENTE","Información insuficiente.",null,List.of(),false));
  mvc.perform(post("/api/rag/buscar").contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"E17\"}"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.fuentes").isArray()).andExpect(jsonPath("$.vector").doesNotExist());
  mvc.perform(post("/api/rag/responder").contentType(MediaType.APPLICATION_JSON).content("{\"pregunta\":\"E17\"}"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.estado").value("INFORMACION_INSUFICIENTE"));
  clearInvocations(busqueda,respuestas);
  for(String ruta:List.of("/api/rag/buscar","/api/rag/responder")) {
   for(String cuerpo:List.of("{}","{\"pregunta\":\" \"}","{\"pregunta\":\""+"a".repeat(2001)+"\"}","{PRIVADO"))
    mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
  }
  verifyNoInteractions(busqueda,respuestas);
 }
}
