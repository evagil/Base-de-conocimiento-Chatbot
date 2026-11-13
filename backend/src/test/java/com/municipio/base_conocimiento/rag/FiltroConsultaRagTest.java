package com.municipio.base_conocimiento.rag;

import com.municipio.base_conocimiento.rag.config.FiltroConsultaRag;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class FiltroConsultaRagTest {
 @Test void conservaElJsonAcotadoParaElControlador() throws Exception {
  var request=new MockHttpServletRequest("POST","/api/rag/buscar");
  String cuerpo="{\"pregunta\":\"¿Cómo corregir E17?\"}";
  request.setContent(cuerpo.getBytes(StandardCharsets.UTF_8));
  new FiltroConsultaRag(new ObjectMapper()).doFilter(request,new MockHttpServletResponse(),(req,res)->{
   var envuelta=(HttpServletRequest)req;
   assertThat(envuelta.getContentLengthLong()).isEqualTo(request.getContentLength());
   assertThat(envuelta.getReader().readLine()).isEqualTo(cuerpo);
   var entrada=envuelta.getInputStream();assertThat(entrada.isReady()).isTrue();assertThat(entrada.isFinished()).isFalse();
   assertThat(new String(entrada.readAllBytes(),StandardCharsets.UTF_8)).isEqualTo(cuerpo);
   assertThat(entrada.isFinished()).isTrue();assertThat(entrada.read()).isEqualTo(-1);
   assertThatThrownBy(()->entrada.setReadListener(null)).isInstanceOf(IllegalStateException.class);
  });
 }
 @Test void rechazaTamanoDeclaradoYFragmentadoSinExponerElCuerpo() throws Exception {
  for(boolean fragmentado:new boolean[]{false,true}) {
   var request=new MockHttpServletRequest("POST","/api/rag/responder"){
    @Override public long getContentLengthLong(){return fragmentado?-1:super.getContentLengthLong();}
   };
   request.setContent(("PRIVADO".repeat(3000)).getBytes(StandardCharsets.UTF_8));
   var response=new MockHttpServletResponse();
   new FiltroConsultaRag(new ObjectMapper()).doFilter(request,response,(req,res)->{throw new AssertionError("No debe deserializar");});
   assertThat(response.getStatus()).isEqualTo(413);
   assertThat(response.getContentAsString()).contains("RAG_CONSULTA_DEMASIADO_GRANDE").doesNotContain("PRIVADO");
   assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
  }
 }
 @Test void dejaPasarOtrosEndpoints() throws Exception {
  for(String ruta:new String[]{"/api/areas","/api/rag/buscar"}) {
   var request=new MockHttpServletRequest("GET",ruta);var chain=new MockFilterChain();
   new FiltroConsultaRag(new ObjectMapper()).doFilter(request,new MockHttpServletResponse(),chain);
   assertThat(chain.getRequest()).isSameAs(request);
  }
  assertThat(new com.municipio.base_conocimiento.rag.dto.ConsultaRag("PRIVADO").toString()).doesNotContain("PRIVADO");
 }
}
