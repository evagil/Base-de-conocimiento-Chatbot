package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.Fragmento;
import com.municipio.base_conocimiento.rag.repository.*;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@EnabledIfEnvironmentVariable(named="BC_TEST_DB_URL",matches="jdbc:postgresql:.+")
class IndicePostgresTest {
    @Test void migraSinPerderConocimientoYValidaIndiceVistaYLeaseEnPostgres()throws Exception{
        String url=System.getenv("BC_TEST_DB_URL"),usuario=System.getenv("BC_TEST_DB_USER"),clave=System.getenv("BC_TEST_DB_PASSWORD");
        String esquema="rag_test_"+UUID.randomUUID().toString().replace("-","");
        var ds=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+esquema,usuario,clave);
        var jdbc=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        try{
            Flyway.configure().dataSource(ds).schemas(esquema).defaultSchema(esquema).locations("classpath:db/migration","classpath:db/vendor/postgresql").target("10").load().migrate();
            jdbc.update("INSERT INTO areas(id,nombre,nombre_normalizado) VALUES(1,'Sistemas','sistemas')");
            jdbc.update("INSERT INTO subareas(id,area_id,nombre,nombre_normalizado) VALUES(1,1,'Soporte','soporte')");
            jdbc.update("INSERT INTO problemas(id,subarea_id,titulo,descripcion_html,descripcion_texto,estado) VALUES(1,1,'E17','<p>Reiniciar</p>','Reiniciar aplicación','PUBLICADO')");
            Flyway.configure().dataSource(ds).schemas(esquema).defaultSchema(esquema).locations("classpath:db/migration","classpath:db/vendor/postgresql").load().migrate();
            assertThat(jdbc.queryForObject("SELECT titulo FROM problemas WHERE id=1",String.class)).isEqualTo("E17");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname=? AND indexname='idx_rag_texto'",Long.class,esquema)).isEqualTo(1);
            var repo=new IndiceRepository(jdbc,new ObjectMapper());
            jdbc.update("INSERT INTO soluciones(id,problema_id,titulo,descripcion_html,descripcion_texto,principal,orden) VALUES(1,1,'Principal','<p>Cerrar</p>','Cerrar la aplicación',TRUE,5)");
            jdbc.update("INSERT INTO soluciones(id,problema_id,titulo,descripcion_html,descripcion_texto,principal,orden) VALUES(2,1,'Otra','<p>Otra</p>','Otra solución',FALSE,1)");
            jdbc.update("INSERT INTO pasos_solucion(id,solucion_id,titulo,contenido_html,contenido_texto,orden) VALUES(1,1,'Segundo','<p>Abrir</p>','Abrir',9),(2,1,'Primero','<p>Cerrar</p>','Cerrar',2)");
            var herramientas=new com.municipio.base_conocimiento.agente.HerramientasAgenteRepository(jdbc);
            assertThat(herramientas.soluciones(1)).extracting(com.municipio.base_conocimiento.agente.HerramientasAgenteRepository.Solucion::id).containsExactly(1L,2L);
            assertThat(herramientas.pasos(1)).extracting(com.municipio.base_conocimiento.agente.HerramientasAgenteRepository.Paso::orden).containsExactly(2,9);
            var lector=new FuentesIndiceRepository(jdbc,new ConfiguracionIndice(true,false,600,80,128,100,3,Duration.ofSeconds(120)));
            var snapshot=lector.obtener(1);
            tx.executeWithoutResult(status->{var trabajo=repo.tomar();assertThat(repo.guardar(trabajo,snapshot,"h".repeat(64),List.of(new Fragmento(snapshot.fuentes().getFirst(),null,"Reiniciar aplicación E17")),null,List.of())).isTrue();status.setRollbackOnly();});
            assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos",Long.class)).isZero();
            var trabajo=tx.execute(status->repo.tomar());
            var otroTrabajo=tx.execute(status->new IndiceRepository(new JdbcTemplate(ds),new ObjectMapper()).tomar());
            assertThat(otroTrabajo).isNull();
            boolean publicado=Boolean.TRUE.equals(tx.execute(status->repo.guardar(trabajo,snapshot,"h".repeat(64),List.of(new Fragmento(snapshot.fuentes().getFirst(),null,"Reiniciar aplicación E17")),null,List.of())));
            assertThat(publicado).isTrue();
            var consulta=new BusquedaRagRepository(jdbc);
            assertThat(consulta.postgres()).isTrue();assertThat(consulta.disponible()).isTrue();
            var encontrados=consulta.textual("reiniciando",10);
            assertThat(encontrados).hasSize(1);assertThat(encontrados.getFirst().rango()).isPositive();
            assertThat(encontrados.getFirst().vector()).isNull();
            assertThat(consulta.vigente(encontrados.getFirst().id())).isPresent();
            var recorridos=new ArrayList<BusquedaRagRepository.Registro>();
            assertThat(consulta.recorrer(10,null,null,recorridos::add)).isFalse();
            assertThat(recorridos).hasSize(1);
            assertThat(consulta.textual("' ; DROP TABLE problemas; --",10)).isEmpty();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos_vigentes WHERE to_tsvector('spanish',texto) @@ plainto_tsquery('spanish','reiniciar')",Long.class)).isEqualTo(1);
            jdbc.update("UPDATE problemas SET estado='ARCHIVADO',version=version+1 WHERE id=1");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos_vigentes",Long.class)).isZero();
            assertThat(consulta.textual("reiniciar",10)).isEmpty();
            assertThat(consulta.vigente(encontrados.getFirst().id())).isEmpty();
            assertThat(herramientas.soluciones(1)).isEmpty();assertThat(herramientas.pasos(1)).isEmpty();
            tx.executeWithoutResult(status->repo.reconstruir(false));
            boolean obsoleto=Boolean.TRUE.equals(tx.execute(status->repo.guardar(trabajo,snapshot,"h".repeat(64),List.of(),null,List.of())));
            assertThat(obsoleto).isFalse();
        }finally{
            // Solo el esquema efímero generado por este test; nunca public ni esquemas existentes.
            if(!esquema.matches("rag_test_[a-f0-9]{32}"))throw new IllegalStateException("Esquema de prueba inválido");
            try(var conexion=DriverManager.getConnection(url,usuario,clave);var sentencia=conexion.createStatement()){
                sentencia.execute("DROP SCHEMA IF EXISTS "+esquema+" CASCADE");
            }
        }
    }
}
