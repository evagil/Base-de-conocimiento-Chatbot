package com.municipio.base_conocimiento.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.municipio.base_conocimiento.archivos.service.ArchivoService;
import com.municipio.base_conocimiento.archivos.service.DetectorDocumentoTest;
import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.problemas.dto.ProblemaRequest;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.entity.Rol;
import com.municipio.base_conocimiento.seguridad.service.AuthService;
import com.municipio.base_conocimiento.seguridad.service.UsuarioService;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:visibilidad;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.storage.path=target/visibility-test-storage"})
@ActiveProfiles("securitytest")
@Transactional
class VisibilidadIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filters;
    @Autowired AreaService areas;
    @Autowired ProblemaService problemas;
    @Autowired SolucionService soluciones;
    @Autowired ArchivoService archivos;
    @Autowired UsuarioService usuarios;
    @Autowired AuthService auth;
    MockMvc mvc;
    long subarea;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
        var area = areas.crear(new AreaRequest("Visibilidad", null, 0));
        subarea = areas.crearSubarea(area.id(), new SubareaRequest("Soporte", null, 0)).id();
    }

    private String token(Rol rol) {
        String usuario = "prueba-" + rol.name().toLowerCase();
        usuarios.crear(new UsuarioRequest("Prueba", usuario, null, "Password123!", rol, false));
        return "Bearer " + auth.login(new LoginRequest(usuario, "Password123!")).accessToken();
    }

    private long problema(EstadoProblema estado) {
        return problemas.crear(new ProblemaRequest(subarea, "Error", "<p>Descripción</p>", estado, Set.of(), null)).id();
    }

    private SolucionResponse solucion(long problema) {
        return soluciones.crear(problema, new SolucionRequest("Solución", "<p>Respuesta</p>", true, 0, null));
    }

    private long imagen(ArchivoService.Propietario propietario, long id) {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        return archivos.cargar(propietario, id,
                new MockMultipartFile("archivo", "captura.png", "image/png", png), "Captura", 0).id();
    }

    @Test void aplicaLaMismaVisibilidadEnDetalleListadoYDescargas() throws Exception {
        String lector = token(Rol.LECTOR);
        String editor = token(Rol.EDITOR);
        String admin = token(Rol.ADMINISTRADOR);
        for (EstadoProblema estado : EstadoProblema.values()) {
            long p = problema(estado);
            var s = solucion(p);
            var paso = soluciones.crearPaso(s.id(), new PasoRequest("Paso", "<p>Acción</p>", 1));
            long imagen = imagen(ArchivoService.Propietario.PASO, paso.id());
            var documento = archivos.cargarDocumento(ArchivoService.Propietario.SOLUCION, s.id(),
                    new MockMultipartFile("archivo", "manual.pdf", "application/pdf", DetectorDocumentoTest.pdf()), "Manual", 0);
            int esperado = estado == EstadoProblema.PUBLICADO ? 200 : 404;
            for (String ruta : new String[]{"/api/problemas/" + p, "/api/problemas/" + p + "/soluciones",
                    "/api/soluciones/" + s.id(), "/api/soluciones/" + s.id() + "/adjuntos",
                    "/api/pasos/" + paso.id() + "/imagenes", "/api/archivos/" + imagen + "/contenido",
                    "/api/archivos/" + documento.id() + "/contenido"}) {
                mvc.perform(get(ruta).header("Authorization", lector)).andExpect(status().is(esperado));
                mvc.perform(get(ruta).header("Authorization", editor)).andExpect(status().isOk());
                mvc.perform(get(ruta).header("Authorization", admin)).andExpect(status().isOk());
                mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            }
        }
        mvc.perform(get("/api/problemas/999999/soluciones").header("Authorization", lector)).andExpect(status().isNotFound());
    }

    @Test void ocultaBajasLogicasSinCambiarElHistorialDelEditor() throws Exception {
        String lector = token(Rol.LECTOR);
        String editor = token(Rol.EDITOR);
        long p = problema(EstadoProblema.PUBLICADO);
        var s = solucion(p);
        var primero = soluciones.crearPaso(s.id(), new PasoRequest("Primero", "<p>Uno</p>", 1));
        var segundo = soluciones.crearPaso(s.id(), new PasoRequest("Segundo", "<p>Dos</p>", 2));
        long captura = imagen(ArchivoService.Propietario.PASO, primero.id());
        long capturaSolucion = imagen(ArchivoService.Propietario.SOLUCION, s.id());
        var archivo = archivos.listar(ArchivoService.Propietario.SOLUCION, s.id()).getFirst();
        archivos.cambiarActivo(capturaSolucion, false, archivo.version());
        mvc.perform(get("/api/soluciones/" + s.id() + "/imagenes").header("Authorization", lector))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/soluciones/" + s.id() + "/imagenes").header("Authorization", editor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/archivos/" + capturaSolucion + "/contenido").header("Authorization", lector)).andExpect(status().isNotFound());
        mvc.perform(get("/api/soluciones/" + s.id()).header("Authorization", lector))
                .andExpect(jsonPath("$.pasos[0].id").value(primero.id())).andExpect(jsonPath("$.pasos[1].id").value(segundo.id()));
        soluciones.cambiarActivoPaso(primero.id(), false, primero.version());
        mvc.perform(get("/api/soluciones/" + s.id()).header("Authorization", lector))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pasos.length()").value(1)).andExpect(jsonPath("$.pasos[0].id").value(segundo.id()));
        mvc.perform(get("/api/pasos/" + primero.id() + "/imagenes").header("Authorization", lector)).andExpect(status().isNotFound());
        mvc.perform(get("/api/archivos/" + captura + "/contenido").header("Authorization", lector)).andExpect(status().isNotFound());
        soluciones.cambiarActivo(s.id(), false, s.version());
        mvc.perform(get("/api/soluciones/" + s.id()).header("Authorization", lector)).andExpect(status().isNotFound());
        mvc.perform(get("/api/problemas/" + p + "/soluciones").header("Authorization", lector))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/problemas/" + p).header("Authorization", lector))
                .andExpect(status().isOk()).andExpect(jsonPath("$.soluciones.length()").value(0));
        mvc.perform(get("/api/soluciones/" + s.id()).header("Authorization", editor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pasos.length()").value(2));
        problemas.cambiarActivo(p, false, problemas.obtener(p).problema().version());
        mvc.perform(get("/api/problemas/" + p + "/soluciones").header("Authorization", lector)).andExpect(status().isNotFound());
        assertThat(soluciones.obtener(s.id()).activo()).isFalse();
    }
}
