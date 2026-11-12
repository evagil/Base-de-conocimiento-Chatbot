package com.municipio.base_conocimiento.conocimiento;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:edicion_sin_cambios;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class EdicionSinCambiosIntegrationTest {
    @Autowired AreaService areas;
    @Autowired ProblemaService problemas;
    @Autowired SolucionService soluciones;
    @Autowired JdbcTemplate jdbc;

    private ProblemaResponse crearProblema() {
        var a = areas.crear(new AreaRequest("Edición " + UUID.randomUUID(), null, 0));
        var s = areas.crearSubarea(a.id(), new SubareaRequest("Configuraciones", null, 0));
        return problemas.crear(new ProblemaRequest(s.id(), "Problema", "<p>Detalle</p>", EstadoProblema.PUBLICADO, Set.of(), null));
    }

    @Test void guardarSinCambiosNoBloqueaUnaEdicionPosteriorEnNegrita() {
        var p = crearProblema();
        var r = new SolucionRequest("Actualización de Recursos", "<p>Detalle</p>", true, 0, null);
        var s = soluciones.crear(p.id(), r);
        var igual = soluciones.actualizar(s.id(), r, s.version());
        assertThat(igual.version()).isEqualTo(s.version());
        assertThat(jdbc.queryForObject("select count(*) from versiones_solucion where solucion_id=?", Long.class, s.id())).isZero();
        var negrita = soluciones.actualizar(s.id(),
                new SolucionRequest("<strong>Actualización de Recursos</strong>", "<p>Detalle</p>", true, 0, null), igual.version());
        assertThat(negrita.titulo()).isEqualTo("<strong>Actualización de Recursos</strong>");
        assertThat(negrita.version()).isGreaterThan(igual.version());
        assertThat(jdbc.queryForObject("select count(*) from versiones_solucion where solucion_id=?", Long.class, s.id())).isEqualTo(1);
    }

    @Test void problemaSinCambiosNoGeneraHistorialPeroCambiarEtiquetasSi() {
        var p = crearProblema();
        var r = new ProblemaRequest(p.subareaId(), p.titulo(), p.descripcionHtml(), p.estado(), Set.of(), null);
        var igual = problemas.actualizar(p.id(), r, p.version());
        assertThat(igual.version()).isEqualTo(p.version());
        assertThat(jdbc.queryForObject("select count(*) from versiones_problema where problema_id=?", Long.class, p.id())).isZero();
        var etiquetas = problemas.actualizar(p.id(),
                new ProblemaRequest(p.subareaId(), p.titulo(), p.descripcionHtml(), p.estado(), Set.of("AFI"), null), igual.version());
        assertThat(etiquetas.version()).isGreaterThan(igual.version());
        assertThat(jdbc.queryForObject("select numero_version from versiones_problema where problema_id=?", Long.class, p.id())).isEqualTo(p.version());
    }
}
