package com.municipio.base_conocimiento.concurrencia;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.municipio.base_conocimiento.areas.dto.*;
import com.municipio.base_conocimiento.areas.service.AreaService;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.service.AuthService;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Run against a disposable PostgreSQL database, never the application's database.
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BC_TEST_DB_URL", matches = ".+")
class PostgresConcurrencyTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("BC_TEST_DB_URL");
        if (url != null && !url.matches("jdbc:postgresql://[^/]+/bc_concurrency_test")) {
            throw new IllegalArgumentException("La base de pruebas debe llamarse bc_concurrency_test.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("BC_TEST_DB_USER"));
        properties.add("spring.datasource.password", () -> Optional.ofNullable(System.getenv("BC_TEST_DB_PASSWORD")).orElse(""));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.show-sql", () -> false);
        properties.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/vendor/postgresql");
    }

    @Autowired AreaService areas;
    @Autowired ProblemaService problemas;
    @Autowired SolucionService soluciones;
    @Autowired AuthService auth;
    @Autowired com.municipio.base_conocimiento.seguridad.service.UsuarioService usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired WebApplicationContext context;

    private ProblemaResponse problema() {
        var area = areas.crear(new AreaRequest("Prueba " + UUID.randomUUID(), null, 0));
        var subarea = areas.crearSubarea(area.id(), new SubareaRequest("Subárea", null, 0));
        return problemas.crear(new ProblemaRequest(subarea.id(), "Original", "<p>Original</p>",
                EstadoProblema.BORRADOR, Set.of(), null));
    }

    private ProblemaRequest cambio(ProblemaResponse p, String titulo) {
        return new ProblemaRequest(p.subareaId(), titulo, "<p>" + titulo + "</p>",
                EstadoProblema.PUBLICADO, Set.of(), "Prueba concurrente");
    }

    private List<Object> simultaneas(IntFunction<Object> operation) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("No comenzó la prueba.");
                    try { return operation.apply(index); } catch (RuntimeException ex) { return ex; }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        }
    }

    @Test void unaEdicionGanaYLaOtraNoSobrescribeNiAgregaHistorial() throws Exception {
        var p = problema();
        var results = simultaneas(i -> problemas.actualizar(p.id(), cambio(p, "Cambio " + i), p.version()));
        assertThat(results.stream().filter(ProblemaResponse.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(RuntimeException.class::isInstance)).hasSize(1);
        var winner = (ProblemaResponse) results.stream().filter(ProblemaResponse.class::isInstance).findFirst().orElseThrow();
        assertThat(problemas.obtener(p.id()).problema().titulo()).isEqualTo(winner.titulo());
        assertThat(winner.version()).isGreaterThan(p.version());
        assertThat(jdbc.queryForObject("select count(*) from versiones_problema where problema_id=?", Long.class, p.id())).isEqualTo(1);
        assertThatThrownBy(() -> problemas.cambiarEstado(p.id(), EstadoProblema.ARCHIVADO, p.version()))
                .isInstanceOf(ConflictoException.class);
    }

    @Test void formularioViejoRecibe409YSinVersionNoPuedeGuardar() throws Exception {
        var p = problema();
        var saved = problemas.actualizar(p.id(), cambio(p, "Actual"), p.version());
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(patch("/api/problemas/" + p.id() + "/estado").param("estado", "ARCHIVADO"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/problemas/" + p.id() + "/estado")
                .param("estado", "ARCHIVADO").param("version", Long.toString(p.version())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));
        assertThat(problemas.obtener(p.id()).problema().version()).isEqualTo(saved.version());
    }

    @Test void dosAltasRecomendadasSeSerializanYLaBaseImpideDuplicados() throws Exception {
        var p = problema();
        var results = simultaneas(i -> soluciones.crear(p.id(), new SolucionRequest("Solución " + i, "<p>Detalle</p>", true, i, null)));
        assertThat(results).allMatch(SolucionResponse.class::isInstance);
        assertThat(soluciones.listar(p.id()).stream().filter(SolucionResponse::principal)).hasSize(1);
        // The invariant is enforced even if a future writer bypasses the service.
        assertThatThrownBy(() -> jdbc.update("update soluciones set principal=true where problema_id=?", p.id()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(soluciones.listar(p.id()).stream().filter(SolucionResponse::principal)).hasSize(1);
    }

    @Test void dosEdicionesDeSolucionNoSePisan() throws Exception {
        var p = problema();
        var s = soluciones.crear(p.id(), new SolucionRequest("Original", "<p>Original</p>", false, 0, null));
        var results = simultaneas(i -> soluciones.actualizar(s.id(),
                new SolucionRequest("Cambio " + i, "<p>Nuevo</p>", true, 0, "Edición"), s.version()));
        assertThat(results.stream().filter(SolucionResponse.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ConflictoException.class::isInstance)).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from versiones_solucion where solucion_id=?", Long.class, s.id())).isEqualTo(1);
    }

    @Test void refreshTokenSoloPuedeConsumirseUnaVez() throws Exception {
        String usuario = "concurrencia-" + UUID.randomUUID();
        String email = usuario + "@test.local";
        usuarios.crear(new UsuarioRequest("Prueba", usuario, email, "Password123!", com.municipio.base_conocimiento.seguridad.entity.Rol.LECTOR, false));
        var login = auth.login(new LoginRequest(usuario, "Password123!"));
        var results = simultaneas(i -> auth.refresh(new RefreshRequest(login.refreshToken())));
        assertThat(results.stream().filter(AuthResponse.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(org.springframework.web.server.ResponseStatusException.class::isInstance)).hasSize(1);
    }

    @Test void pasosYAreasRechazanVersionesViejas() {
        var p = problema();
        var s = soluciones.crear(p.id(), new SolucionRequest(null, "<p>Detalle</p>", false, 0, null));
        var step = soluciones.crearPaso(s.id(), new PasoRequest("Paso", "<p>Antes</p>", 1));
        var saved = soluciones.actualizarPaso(step.id(), new PasoRequest("Paso nuevo", "<p>Después</p>", 1), step.version());
        assertThat(saved.version()).isGreaterThan(step.version());
        assertThatThrownBy(() -> soluciones.cambiarActivoPaso(step.id(), false, step.version())).isInstanceOf(ConflictoException.class);
        var area = areas.obtener(p.areaId());
        areas.actualizar(area.id(), new AreaRequest("Cambio " + UUID.randomUUID(), null, 0), area.version());
        assertThatThrownBy(() -> areas.cambiarEstado(area.id(), false, area.version())).isInstanceOf(ConflictoException.class);
    }

    @Test void migracionActualizaV3SinPerderDatosYRechazaDuplicadosPrevios() {
        for (boolean duplicados : List.of(false, true)) {
            String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
            try {
                org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                        .locations("classpath:db/migration", "classpath:db/vendor/postgresql").target("3").load().migrate();
                jdbc.update("insert into " + schema + ".areas(nombre,nombre_normalizado) values ('A','a')");
                jdbc.update("insert into " + schema + ".subareas(area_id,nombre,nombre_normalizado) values (1,'S','s')");
                jdbc.update("insert into " + schema + ".problemas(subarea_id,titulo,descripcion_html,descripcion_texto,estado) values (1,'P','Detalle','Detalle','PUBLICADO')");
                jdbc.update("insert into " + schema + ".soluciones(problema_id,titulo,descripcion_html,descripcion_texto,principal) values (1,'Original','Detalle','Detalle',true)");
                if (duplicados) {
                    jdbc.update("insert into " + schema + ".soluciones(problema_id,titulo,descripcion_html,descripcion_texto,principal) values (1,'Otra','Detalle','Detalle',true)");
                }
                var flyway = org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                        .locations("classpath:db/migration", "classpath:db/vendor/postgresql").load();
                if (duplicados) {
                    assertThatThrownBy(flyway::migrate).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
                    assertThat(jdbc.queryForObject("select count(*) from " + schema + ".soluciones where principal", Long.class)).isEqualTo(2);
                } else {
                    flyway.migrate();
                    assertThat(jdbc.queryForObject("select titulo from " + schema + ".soluciones", String.class)).isEqualTo("Original");
                    assertThatThrownBy(() -> jdbc.update("insert into " + schema + ".soluciones(problema_id,descripcion_html,descripcion_texto,principal) values (1,'Otra','Otra',true)"))
                            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
                }
            } finally {
                jdbc.execute("drop schema if exists " + schema + " cascade");
            }
        }
    }

    @Test void v5ConservaHistorialYDesbloqueaVersionesRegistradasSinCambios() {
        String schema = "repair_" + UUID.randomUUID().toString().replace("-", "");
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration", "classpath:db/vendor/postgresql").target("4").load().migrate();
            jdbc.update("insert into " + schema + ".areas(nombre,nombre_normalizado) values ('A','a')");
            jdbc.update("insert into " + schema + ".subareas(area_id,nombre,nombre_normalizado) values (1,'S','s')");
            jdbc.update("insert into " + schema + ".problemas(subarea_id,titulo,descripcion_html,descripcion_texto,estado) values (1,'P','Detalle','Detalle','PUBLICADO')");
            jdbc.update("insert into " + schema + ".soluciones(problema_id,titulo,descripcion_html,descripcion_texto) values (1,'Original','Detalle','Detalle')");
            jdbc.update("insert into " + schema + ".soluciones(problema_id,titulo,descripcion_html,descripcion_texto,version) values (1,'Sin conflicto','Detalle','Detalle',5)");
            jdbc.update("insert into " + schema + ".versiones_solucion(solucion_id,numero_version,contenido_anterior) values (1,0,'historia original'),(2,2,'historia anterior')");
            jdbc.update("insert into " + schema + ".versiones_problema(problema_id,numero_version,contenido_anterior) values (1,0,'historia problema')");
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration", "classpath:db/vendor/postgresql").load().migrate();
            assertThat(jdbc.queryForObject("select version from " + schema + ".soluciones where id=1", Long.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select version from " + schema + ".soluciones where id=2", Long.class)).isEqualTo(5);
            assertThat(jdbc.queryForObject("select version from " + schema + ".problemas where id=1", Long.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select contenido_anterior from " + schema + ".versiones_solucion where solucion_id=1", String.class)).isEqualTo("historia original");
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".versiones_solucion", Long.class)).isEqualTo(2);
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
    @Test void migracionReduceEstadosConservandoContenidoYVersiones() {
        String schema = "estados_" + UUID.randomUUID().toString().replace("-", "");
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration", "classpath:db/vendor/postgresql").target("5").load().migrate();
            jdbc.update("insert into " + schema + ".areas(nombre,nombre_normalizado) values ('A','a')");
            jdbc.update("insert into " + schema + ".subareas(area_id,nombre,nombre_normalizado) values (1,'S','s')");
            for (String estado : List.of("BORRADOR", "EN_REVISION", "PUBLICADO", "RESUELTO", "ARCHIVADO")) {
                jdbc.update("insert into " + schema + ".problemas(subarea_id,titulo,descripcion_html,descripcion_texto,estado,version) values (1,?,'<p>Detalle</p>','Detalle',?,7)", estado, estado);
            }
            jdbc.update("insert into " + schema + ".versiones_problema(problema_id,numero_version,contenido_anterior) values (2,6,'historial original')");
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .locations("classpath:db/migration", "classpath:db/vendor/postgresql").load().migrate();
            assertThat(jdbc.queryForList("select estado from " + schema + ".problemas order by id", String.class))
                    .containsExactly("BORRADOR", "BORRADOR", "PUBLICADO", "PUBLICADO", "ARCHIVADO");
            assertThat(jdbc.queryForList("select version from " + schema + ".problemas order by id", Long.class))
                    .containsExactly(7L, 8L, 7L, 8L, 7L);
            assertThat(jdbc.queryForList("select descripcion_html from " + schema + ".problemas", String.class))
                    .containsOnly("<p>Detalle</p>");
            assertThat(jdbc.queryForObject("select contenido_anterior from " + schema + ".versiones_problema where problema_id=2", String.class))
                    .isEqualTo("historial original");
            for (String eliminado : List.of("EN_REVISION", "RESUELTO")) {
                assertThatThrownBy(() -> jdbc.update("update " + schema + ".problemas set estado=? where id=1", eliminado))
                        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            }
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }

    @Test void eliminarExigeQuitarSolucionesYNoPermiteReactivarlasEnProblemaEliminado() {
        var p = problema();
        var s = soluciones.crear(p.id(), new SolucionRequest("Solución", "<p>Detalle</p>", false, 0, null));
        assertThatThrownBy(() -> problemas.cambiarActivo(p.id(), false, p.version()))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("Primero eliminá");
        assertThat(problemas.obtener(p.id()).problema().activo()).isTrue();
        var inactive = soluciones.cambiarActivo(s.id(), false, s.version());
        var deleted = problemas.cambiarActivo(p.id(), false, p.version());
        assertThat(deleted.activo()).isFalse();
        assertThatThrownBy(() -> soluciones.cambiarActivo(s.id(), true, inactive.version()))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("problema eliminado");
        assertThatThrownBy(() -> problemas.cambiarActivo(p.id(), true, p.version()))
                .isInstanceOf(ConflictoException.class);
    }

    @Test void altaConcurrenteNoDejaSolucionesActivasEnProblemaEliminado() throws Exception {
        var p = problema();
        var results = simultaneas(i -> i == 0
                ? problemas.cambiarActivo(p.id(), false, p.version())
                : soluciones.crear(p.id(), new SolucionRequest("Nueva", "<p>Detalle</p>", false, 0, null)));
        assertThat(results.stream().filter(ConflictoException.class::isInstance)).hasSize(1);
        boolean active = problemas.obtener(p.id()).problema().activo();
        assertThat(soluciones.tieneActivas(p.id())).isEqualTo(active);
    }

}
