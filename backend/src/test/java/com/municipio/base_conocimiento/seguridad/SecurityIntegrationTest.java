package com.municipio.base_conocimiento.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import tools.jackson.databind.JsonNode;import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.security.web.FilterChainProxy;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.MockMvc;import org.springframework.test.web.servlet.setup.MockMvcBuilders;import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:security_integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","app.storage.path=target/security-test-storage","app.documentos.habilitada=false"}) @ActiveProfiles("securitytest")
@org.springframework.transaction.annotation.Transactional
class SecurityIntegrationTest{
 MockMvc mvc;@Autowired ObjectMapper json;@Autowired WebApplicationContext context;@Autowired FilterChainProxy filters;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();}
 private JsonNode login(String usuario,String password)throws Exception{return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\""+usuario+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void autenticacionRolesRefreshAuditoria()throws Exception{
  var documento = new org.springframework.mock.web.MockMultipartFile("archivo","manual.pdf","application/pdf",new byte[]{1});
  mvc.perform(multipart("/api/documentos/extraer").file(documento)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/areas")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/ia/indice")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"admin\",\"password\":\"incorrecta\"}")).andExpect(status().isUnauthorized());
  JsonNode admin=login("admin","Admin123!");String adminToken=admin.get("accessToken").asText();String refresh=admin.get("refreshToken").asText();
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
  mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Lectora\",\"usuario\":\"lectora\",\"email\":\"lectora@municipio.local\",\"password\":\"Password123!\",\"rol\":\"LECTOR\"}")).andExpect(status().isCreated());
  JsonNode lector=login("lectora","Password123!");String lectorToken=lector.get("accessToken").asText();
  mvc.perform(get("/api/ia/indice").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(false));
  mvc.perform(get("/api/ia/indice").header("Authorization","Bearer "+lectorToken)).andExpect(status().isForbidden());
  mvc.perform(post("/api/ia/indice/reconstruir").header("Authorization","Bearer "+lectorToken)).andExpect(status().isForbidden());
  for(String token:java.util.List.of(adminToken,lectorToken)) mvc.perform(multipart("/api/documentos/extraer").file(documento).header("Authorization","Bearer "+token))
    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("DOCUMENTOS_DESHABILITADOS"));
  mvc.perform(get("/api/areas").header("Authorization","Bearer "+lectorToken)).andExpect(status().isOk());
  mvc.perform(post("/api/areas").header("Authorization","Bearer "+lectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Prohibida\",\"orden\":0}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
  mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/password").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON).content("{\"passwordActual\":\"Admin123!\",\"passwordNueva\":\"Admin123!!\"}")).andExpect(status().isNoContent());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"admin\",\"password\":\"Admin123!!\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/auditoria").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].usuario").exists());
 }
 @Test void lectorSoloAccedeAPublicadosInclusoPorUrlDirecta() throws Exception {
  String adminToken=login("admin","Admin123!").get("accessToken").asText();
  String suffix=java.util.UUID.randomUUID().toString();
  String usuario="lector-"+suffix.substring(0,8);
  String email=usuario+"@test.local";
  mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
    .content("{\"nombre\":\"Lector\",\"usuario\":\""+usuario+"\",\"email\":\""+email+"\",\"password\":\"Password123!\",\"rol\":\"LECTOR\"}")).andExpect(status().isCreated());
  String readerToken=login(usuario,"Password123!").get("accessToken").asText();
  long area=json.readTree(mvc.perform(post("/api/areas").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
    .content("{\"nombre\":\"Visibilidad "+suffix+"\",\"orden\":0}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  long subarea=json.readTree(mvc.perform(post("/api/areas/"+area+"/subareas").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
    .content("{\"nombre\":\"Pruebas\",\"orden\":0}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
  for(String state:java.util.List.of("BORRADOR","PUBLICADO","ARCHIVADO")) {
   long problem=json.readTree(mvc.perform(post("/api/problemas").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
    .content("{\"subareaId\":"+subarea+",\"titulo\":\""+state+"\",\"descripcionHtml\":\"<p>Detalle</p>\",\"estado\":\""+state+"\",\"etiquetas\":[]}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
   mvc.perform(get("/api/problemas/"+problem).header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
   var document = new org.springframework.mock.web.MockMultipartFile("archivo","manual.pdf","application/pdf",com.municipio.base_conocimiento.archivos.service.DetectorDocumentoTest.pdf());
   long attachment=json.readTree(mvc.perform(multipart("/api/problemas/"+problem+"/adjuntos").file(document).param("nombre","Manual").header("Authorization","Bearer "+adminToken))
    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
   mvc.perform(get("/api/problemas/"+problem+"/adjuntos").header("Authorization","Bearer "+readerToken)).andExpect(status().is(state.equals("PUBLICADO")?200:404));
   mvc.perform(get("/api/archivos/"+attachment+"/contenido").header("Authorization","Bearer "+readerToken)).andExpect(status().is(state.equals("PUBLICADO")?200:404));
   mvc.perform(get("/api/archivos/"+attachment+"/contenido").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));
   mvc.perform(multipart("/api/problemas/"+problem+"/adjuntos").file(document).param("nombre","Manual").header("Authorization","Bearer "+readerToken)).andExpect(status().isForbidden());

   mvc.perform(get("/api/problemas/"+problem).header("Authorization","Bearer "+readerToken))
    .andExpect(status().is(state.equals("PUBLICADO")?200:404));
   mvc.perform(get("/api/problemas/"+problem+"/imagenes").header("Authorization","Bearer "+readerToken))
    .andExpect(status().is(state.equals("PUBLICADO")?200:404));
   mvc.perform(patch("/api/problemas/"+problem+"/estado").param("version","0").param("estado","PUBLICADO").header("Authorization","Bearer "+readerToken)).andExpect(status().isForbidden());
  }
  for(String active:java.util.List.of("true","false")) {
   mvc.perform(get("/api/problemas").param("subareaId",Long.toString(subarea)).param("activo",active).header("Authorization","Bearer "+readerToken))
    .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].estado").value("PUBLICADO"));
  }
 }

}
