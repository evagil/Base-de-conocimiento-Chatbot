package com.municipio.base_conocimiento.documentos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties={"app.documentos.habilitada=true","app.ia.habilitada=false","app.storage.path=target/documentos-test-storage"})
@ActiveProfiles("securitytest")
@org.springframework.transaction.annotation.Transactional
@EnabledIfEnvironmentVariable(named="BC_TEST_TESSERACT",matches=".+")
class DocumentosRealIntegrationTest {
    private static final Path TEMPORALES = Path.of("target/documentos-api-temporales").toAbsolutePath();
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.documentos.tesseract",() -> System.getenv("BC_TEST_TESSERACT"));
        registro.add("app.documentos.tessdata",() -> System.getenv("BC_TEST_TESSDATA") == null ? "" : System.getenv("BC_TEST_TESSDATA"));
        registro.add("app.documentos.temporales",() -> TEMPORALES.toString());
    }
    @Autowired WebApplicationContext contexto;
    @Autowired FilterChainProxy filtros;
    @Autowired ObjectMapper json;
    MockMvc mvc;
    @BeforeEach void preparar() { mvc=MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtros).build(); }
    private String login(String usuario,String password) throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("usuario",usuario,"password",password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private void vacio() throws Exception { try(var archivos=Files.list(TEMPORALES)) { assertThat(archivos.toList()).isEmpty(); } }
    @Test void extraePdfYDocxConTodosLosRolesSinPersistirConocimiento() throws Exception {
        var pdf=new MockMultipartFile("archivo","manual.pdf","application/pdf",DocumentosFixtures.pdf("ERROR E17"));
        mvc.perform(multipart("/api/documentos/extraer").file(pdf)).andExpect(status().isUnauthorized());
        String admin=login("admin","Admin123!");
        long cantidad=json.readTree(mvc.perform(get("/api/problemas").header("Authorization","Bearer "+admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("totalElements").asLong();
        for (String rol:java.util.List.of("ADMINISTRADOR","EDITOR","LECTOR")) {
            String token=admin;
            if(!rol.equals("ADMINISTRADOR")) {
                String usuario="doc-"+rol.toLowerCase();
                mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("nombre",usuario,"usuario",usuario,"email",usuario+"@test.local","password","Password123!","rol",rol))))
                        .andExpect(status().isCreated());
                token=login(usuario,"Password123!");
            }
            mvc.perform(multipart("/api/documentos/extraer").file(pdf).header("Authorization","Bearer "+token))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(jsonPath("$.paginas").value(1)).andExpect(jsonPath("$.secciones[0].texto").value("ERROR E17"));
            vacio();
        }
        mvc.perform(multipart("/api/documentos/extraer").file(new MockMultipartFile("archivo","manual.docx","application/octet-stream",DocumentosFixtures.docx("ERROR E17")))
                .header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.ocrUsado").value(false));
        vacio();
        assertThat(json.readTree(mvc.perform(get("/api/problemas").header("Authorization","Bearer "+admin))
                .andReturn().getResponse().getContentAsString()).get("totalElements").asLong()).isEqualTo(cantidad);
    }
    @Test void ejecutaOcrRealEnJpgYPdfEscaneadoYLimpiaTambienAlFallar() throws Exception {
        String token=login("admin","Admin123!");
        for(String extension:java.util.List.of("jpg","pdf")) {
            byte[] bytes=extension.equals("jpg") ? DocumentosFixtures.imagen("jpg") : DocumentosFixtures.escaneado(1);
            var respuesta=mvc.perform(multipart("/api/documentos/extraer").file(new MockMultipartFile("archivo","escaneo."+extension,"application/octet-stream",bytes))
                    .header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.ocrUsado").value(true))
                    .andExpect(jsonPath("$.secciones[0].metodo").value("OCR")).andReturn().getResponse().getContentAsString();
            assertThat(json.readTree(respuesta).get("secciones").get(0).get("texto").asText()).contains("E17","REINICIAR");
            vacio();
        }
        mvc.perform(multipart("/api/documentos/extraer").file(new MockMultipartFile("archivo","roto.pdf","application/pdf",new byte[]{1,2,3}))
                .header("Authorization","Bearer "+token)).andExpect(status().isUnsupportedMediaType());
        vacio();
    }
}
