package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.dto.ResultadoEmbeddings;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.repository.*;
import com.municipio.base_conocimiento.rag.service.*;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:indice;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "app.indice.habilitado=true","app.indice.embeddings=true","app.indice.inicio=99999999"})
@ActiveProfiles("test")
class IndiceIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired IndexadorConocimiento indexador;@Autowired IndiceRepository indice;
    @Autowired FuentesIndiceRepository fuentes;@Autowired ConfiguracionIndice config;@Autowired ConfiguracionIa ia;
    @Autowired Fragmentador fragmentador;@Autowired ObjectMapper json;
    @MockitoBean ClienteOllama ollama;@MockitoBean LectorAdjuntoIndice adjuntos;
    long problema,subarea,area;
    @BeforeEach void datos(){
        jdbc.update("UPDATE rag_control SET token=NULL,vence_en=NULL,cursor_id=0 WHERE id=1");
        String sufijo=UUID.randomUUID().toString();
        area=insertar("INSERT INTO areas(nombre,nombre_normalizado) VALUES(?,?)","Sistemas "+sufijo,sufijo);
        subarea=insertar("INSERT INTO subareas(area_id,nombre,nombre_normalizado) VALUES(?,?,?)",area,"Soporte",sufijo);
        problema=insertar("INSERT INTO problemas(subarea_id,titulo,descripcion_html,descripcion_texto,estado) VALUES(?,?,?,?,?)",subarea,"ERROR E17","<p>Detalle</p>","Detalle del problema","PUBLICADO");
        when(ollama.vectorizar(anyList())).thenAnswer(i->{List<String> textos=i.getArgument(0);return new ResultadoEmbeddings(ia.modeloEmbeddings(),textos.stream().map(t->List.of(1.0,2.0,3.0)).toList());});
    }
    long insertar(String sql,Object... valores){var clave=new GeneratedKeyHolder();jdbc.update(c->{var ps=c.prepareStatement(sql,new String[]{"id"});for(int i=0;i<valores.length;i++)ps.setObject(i+1,valores[i]);return ps;},clave);return clave.getKey().longValue();}
    void ejecutar(){jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);assertThat(indexador.procesarSiguiente()).isTrue();}
    long cantidad(){return jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos WHERE problema_id=?",Long.class,problema);}
    long solucion(){return insertar("INSERT INTO soluciones(problema_id,titulo,descripcion_html,descripcion_texto) VALUES(?,?,?,?)",problema,"Reiniciar","<p>Cerrar</p>","Cerrar la aplicación");}
    long paso(long solucion){return insertar("INSERT INTO pasos_solucion(solucion_id,titulo,contenido_html,contenido_texto,orden) VALUES(?,?,?,?,?)",solucion,"Abrir","<p>Abrir</p>","Volver a abrir",1);}
    @Test void indexaFuentesMetadatosVectoresYNoRepiteTrabajoSinCambios(){
        long solucion=solucion();paso(solucion);
        long etiqueta=insertar("INSERT INTO etiquetas(nombre,nombre_normalizado) VALUES(?,?)","Acceso","acceso-"+problema);
        jdbc.update("INSERT INTO problema_etiquetas(problema_id,etiqueta_id) VALUES(?,?)",problema,etiqueta);
        ejecutar();assertThat(cantidad()).isEqualTo(3);
        var filas=jdbc.queryForList("SELECT * FROM rag_fragmentos WHERE problema_id=? ORDER BY posicion",problema);
        assertThat(filas.getFirst().get("TEXTO").toString()).contains("ERROR E17","Soporte","Acceso");
        assertThat(jdbc.queryForObject("SELECT dimension FROM rag_fragmentos WHERE problema_id=? LIMIT 1",Integer.class,problema)).isEqualTo(3);
        assertThat(indice.estado(problema).estado()).isEqualTo("LISTO");
        clearInvocations(ollama);ejecutar();verifyNoInteractions(ollama);
        jdbc.update("UPDATE soluciones SET descripcion_texto='Nueva solución',version=version+1 WHERE id=?",solucion);
        ejecutar();assertThat(jdbc.queryForObject("SELECT texto FROM rag_fragmentos WHERE problema_id=? AND tipo='SOLUCION'",String.class,problema)).contains("Nueva solución");
        assertThat(indice.resumen()).containsKeys("listo","error","pendiente","fragmentos","problemas","cursor");
    }
    @Test void retiraInactivosArchivadosYPadresSinPermitirQueVuelvanAlIndice(){
        long solucion=solucion(),paso=paso(solucion);ejecutar();
        jdbc.update("UPDATE pasos_solucion SET activo=FALSE,version=version+1 WHERE id=?",paso);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos_vigentes WHERE problema_id=?",Long.class,problema)).isEqualTo(2);
        ejecutar();assertThat(cantidad()).isEqualTo(2);
        jdbc.update("UPDATE soluciones SET activo=FALSE,version=version+1 WHERE id=?",solucion);ejecutar();assertThat(cantidad()).isEqualTo(1);
        for(String columna:List.of("estado='ARCHIVADO'","estado='BORRADOR'","activo=FALSE")){
            jdbc.update("UPDATE problemas SET "+columna+",version=version+1 WHERE id=?",problema);ejecutar();assertThat(cantidad()).isZero();
            jdbc.update("UPDATE problemas SET activo=TRUE,estado='PUBLICADO',version=version+1 WHERE id=?",problema);ejecutar();assertThat(cantidad()).isEqualTo(1);
        }
        jdbc.update("UPDATE areas SET activo=FALSE WHERE id=?",area);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos_vigentes WHERE problema_id=?",Long.class,problema)).isZero();
        ejecutar();assertThat(cantidad()).isZero();
        jdbc.update("UPDATE areas SET activo=TRUE,nombre='Sistemas renovados' WHERE id=?",area);
        jdbc.update("UPDATE subareas SET nombre='Soporte nuevo' WHERE id=?",subarea);ejecutar();
        assertThat(jdbc.queryForObject("SELECT texto FROM rag_fragmentos WHERE problema_id=?",String.class,problema)).contains("Sistemas renovados","Soporte nuevo");
    }
    @Test void registraFallasReintentaHastaTresVecesYPermiteRecuperacionAdministrativa(){
        ejecutar();doThrow(new IaException(HttpStatus.SERVICE_UNAVAILABLE,"IA_NO_DISPONIBLE","Sin servidor")).when(ollama).vectorizar(anyList());
        jdbc.update("UPDATE problemas SET version=version+1 WHERE id=?",problema);
        ejecutar();assertThat(cantidad()).isZero();assertThat(indice.estado(problema).intentos()).isEqualTo(1);
        clearInvocations(ollama);ejecutar();verifyNoInteractions(ollama);
        for(int i=2;i<=3;i++){jdbc.update("UPDATE rag_estado SET reintentar_en=NULL WHERE problema_id=?",problema);ejecutar();assertThat(indice.estado(problema).intentos()).isEqualTo(i);}
        clearInvocations(ollama);ejecutar();verifyNoInteractions(ollama);
        indice.reconstruir(true);assertThat(indice.estado(problema).estado()).isEqualTo("PENDIENTE");
        doAnswer(i->{List<String> textos=i.getArgument(0);return new ResultadoEmbeddings(ia.modeloEmbeddings(),textos.stream().map(t->List.of(1.0)).toList());}).when(ollama).vectorizar(anyList());
        ejecutar();assertThat(indice.estado(problema).estado()).isEqualTo("LISTO");indice.reconstruir(false);ejecutar();assertThat(cantidad()).isEqualTo(1);
    }
    @Test void descartaResultadosObsoletosYRespetaLeaseEntreTrabajadores(){
        jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);
        var trabajo=indice.tomar();assertThat(indice.tomar()).isNull();indice.reconstruir(false);
        assertThat(indice.guardar(trabajo,fuentes.obtener(problema),"vieja",List.of(),null,List.of())).isFalse();
        indice.fallar(trabajo,"vieja","ERROR",1);assertThat(indice.estado(problema)).isNull();indice.liberar(trabajo);
        when(ollama.vectorizar(anyList())).thenAnswer(i->{jdbc.update("UPDATE problemas SET version=version+1 WHERE id=?",problema);return new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of(1.0)));});
        ejecutar();assertThat(cantidad()).isZero();assertThat(indice.estado(problema)).isNull();
    }
    @Test void extraeSoloDocumentosActivosConReferenciasDePagina()throws Exception{
        long solucion=solucion(),paso=paso(solucion);
        for(String tipo:List.of("problemas","soluciones","pasos")){
            long archivo=insertar("INSERT INTO archivos(nombre_original,nombre_interno,tipo_mime,tamanio_bytes,ruta_relativa,hash_sha256,texto_alternativo) VALUES(?,?,?,?,?,?,?)",
                "manual.pdf",UUID.randomUUID().toString(),"application/pdf",10,"pdf/"+UUID.randomUUID(),"a".repeat(64),"Manual");
            String columna=tipo.equals("problemas")?"problema_id":tipo.equals("soluciones")?"solucion_id":"paso_id";
            jdbc.update("INSERT INTO archivo_"+tipo+"(archivo_id,"+columna+") VALUES(?,?)",archivo,tipo.equals("problemas")?problema:tipo.equals("soluciones")?solucion:paso);
        }
        when(adjuntos.leer(any())).thenReturn(new DocumentoExtraido("manual.pdf","application/pdf",2,List.of(new DocumentoExtraido.Seccion(2,"Manual E17","TEXTO")),10,false));
        ejecutar();assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos WHERE problema_id=? AND tipo='ADJUNTO' AND pagina=2",Long.class,problema)).isEqualTo(3);
        jdbc.update("UPDATE archivos SET activo=FALSE WHERE id IN(SELECT archivo_id FROM archivo_problemas WHERE problema_id=?)",problema);
        ejecutar();assertThat(cantidad()).isEqualTo(5);
    }
    @Test void permiteIndiceTextualSinModeloYDesactivacionCompleta(){
        var textual=new IndexadorConocimiento(FragmentadorTest.config(true,false),ia,fuentes,indice,fragmentador,adjuntos,ollama,json);
        jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);assertThat(textual.procesarSiguiente()).isTrue();verifyNoInteractions(ollama);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos WHERE problema_id=? AND vector_json IS NULL",Long.class,problema)).isEqualTo(1);
        var desactivado=new IndexadorConocimiento(FragmentadorTest.config(false,false),ia,fuentes,indice,fragmentador,adjuntos,ollama,json);
        assertThat(desactivado.procesarSiguiente()).isFalse();desactivado.ciclo();
        jdbc.update("UPDATE rag_control SET cursor_id=999999999 WHERE id=1");assertThat(textual.procesarSiguiente()).isFalse();
    }
    @Test void rechazaVectoresInvalidosSinDejarUnIndiceParcial(){
        for(var resultado:List.of(new ResultadoEmbeddings("otro",List.of(List.of(1.0))),
                new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of()),
                new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of())),
                new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of(0.0))),
                new ResultadoEmbeddings(ia.modeloEmbeddings(),List.of(List.of(Double.NaN))))){
            doReturn(resultado).when(ollama).vectorizar(anyList());indice.reconstruir(false);ejecutar();
            assertThat(cantidad()).isZero();assertThat(indice.estado(problema).estado()).isEqualTo("ERROR");
        }
    }
    @Test void limitaTiempoYFragmentosSinConsultarAlModelo(){
        var corto=new IndexadorConocimiento(new ConfiguracionIndice(true,true,600,80,128,100,3,Duration.ofNanos(1)),ia,fuentes,indice,fragmentador,adjuntos,ollama,json);
        jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);corto.procesarSiguiente();
        assertThat(indice.estado(problema).estado()).isEqualTo("ERROR");verifyNoInteractions(ollama);
        indice.reconstruir(false);jdbc.update("UPDATE problemas SET descripcion_texto=? WHERE id=?","x".repeat(100000),problema);
        ejecutar();assertThat(cantidad()).isZero();verifyNoInteractions(ollama);
    }
    @Test void limitaFuentesYControlaReintentosDeSnapshotInvalido(){
        solucion();var limite=new ConfiguracionIndice(true,true,600,80,128,1,3,Duration.ofSeconds(120));
        var limitado=new IndexadorConocimiento(limite,ia,new FuentesIndiceRepository(jdbc,limite),indice,fragmentador,adjuntos,ollama,json);
        for(int intento=1;intento<=3;intento++){
            jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);
            jdbc.update("UPDATE rag_estado SET reintentar_en=NULL WHERE problema_id=?",problema);
            limitado.procesarSiguiente();assertThat(indice.estado(problema).intentos()).isEqualTo(intento);
        }
        jdbc.update("UPDATE rag_control SET cursor_id=? WHERE id=1",problema-1);limitado.procesarSiguiente();
        assertThat(indice.estado(problema).intentos()).isEqualTo(3);verifyNoInteractions(ollama);
    }
}
