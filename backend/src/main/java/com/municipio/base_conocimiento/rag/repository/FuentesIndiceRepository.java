package com.municipio.base_conocimiento.rag.repository;

import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.Fuente;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class FuentesIndiceRepository {
    private final JdbcTemplate jdbc;
    private final ConfiguracionIndice config;
    public FuentesIndiceRepository(JdbcTemplate jdbc,ConfiguracionIndice config){this.jdbc=jdbc;this.config=config;}
    @Transactional(readOnly=true,propagation=Propagation.REQUIRES_NEW,isolation=Isolation.REPEATABLE_READ)
    public FuentesIndice obtener(long id){
        return jdbc.queryForObject("""
            SELECT p.id,p.titulo,p.subarea_id,p.estado,p.activo,p.version,p.modificado_en,
                   substring(p.descripcion_texto,1,200001) descripcion_texto,a.id area_id,a.nombre area,s.nombre subarea,
                   (p.activo AND p.estado='PUBLICADO' AND s.activo AND a.activo) visible
            FROM problemas p JOIN subareas s ON s.id=p.subarea_id JOIN areas a ON a.id=s.area_id WHERE p.id=?
            """,(r,n)->{
            List<Fuente> fuentes=new ArrayList<>();
            if(r.getBoolean("visible")){
                var nombres=jdbc.queryForList("""
                    SELECT e.nombre FROM etiquetas e JOIN problema_etiquetas pe ON pe.etiqueta_id=e.id
                    WHERE pe.problema_id=? AND e.activo=TRUE ORDER BY e.id LIMIT 201
                    """,String.class,id);
                if(nombres.size()>200)throw new IllegalArgumentException("LIMITE_FUENTES");
                String etiquetas=String.join(", ",nombres);
                agregar(fuentes,new Fuente("PROBLEMA",id,null,null,null,r.getLong("version"),fecha(r),
                        r.getString("descripcion_texto")+"\nEtiquetas: "+etiquetas,null,null,null,null));
                leer("""
                    SELECT 'SOLUCION' tipo,id,id solucion_id,NULL paso_id,version,modificado_en,
                        COALESCE(titulo,'') || '\n' || substring(descripcion_texto,1,200001) texto
                    FROM soluciones WHERE problema_id=? AND activo=TRUE ORDER BY orden,id LIMIT 201
                    """,this::fuente,fuentes,id);
                leer("""
                    SELECT 'PASO' tipo,p.id,s.id solucion_id,p.id paso_id,p.version,p.modificado_en,
                        COALESCE(p.titulo,'') || '\n' || substring(p.contenido_texto,1,200001) texto
                    FROM pasos_solucion p JOIN soluciones s ON s.id=p.solucion_id
                    WHERE s.problema_id=? AND s.activo=TRUE AND p.activo=TRUE ORDER BY s.orden,s.id,p.orden,p.id LIMIT 201
                    """,this::fuente,fuentes,id);
                leer("""
                    SELECT ar.*,v.solucion_id,v.paso_id FROM archivos ar JOIN (
                        SELECT archivo_id,CAST(NULL AS BIGINT) solucion_id,CAST(NULL AS BIGINT) paso_id FROM archivo_problemas WHERE problema_id=?
                        UNION ALL SELECT ap.archivo_id,s.id solucion_id,CAST(NULL AS BIGINT) paso_id FROM archivo_soluciones ap
                          JOIN soluciones s ON s.id=ap.solucion_id WHERE s.problema_id=? AND s.activo=TRUE
                        UNION ALL SELECT ap.archivo_id,s.id solucion_id,ps.id paso_id FROM archivo_pasos ap
                          JOIN pasos_solucion ps ON ps.id=ap.paso_id JOIN soluciones s ON s.id=ps.solucion_id
                          WHERE s.problema_id=? AND s.activo=TRUE AND ps.activo=TRUE
                    ) v ON v.archivo_id=ar.id WHERE ar.activo=TRUE AND ar.tipo_mime IN
                    ('application/pdf','application/vnd.openxmlformats-officedocument.wordprocessingml.document') ORDER BY ar.id LIMIT 201
                    """,(a,i)->new Fuente("ADJUNTO",a.getLong("id"),opcional(a,"solucion_id"),opcional(a,"paso_id"),a.getLong("id"),
                        a.getLong("version"),fecha(a),a.getString("texto_alternativo"),a.getString("ruta_relativa"),a.getString("hash_sha256"),
                        a.getString("nombre_original"),a.getString("tipo_mime")),fuentes,id,id,id);
            }
            return new FuentesIndice(id,r.getLong("area_id"),r.getLong("subarea_id"),r.getLong("version"),r.getString("titulo"),
                    r.getString("area"),r.getString("subarea"),r.getString("estado"),r.getBoolean("activo"),r.getBoolean("visible"),fecha(r),List.copyOf(fuentes));
        },id);
    }
    private Fuente fuente(ResultSet r,int n)throws SQLException{return new Fuente(r.getString("tipo"),r.getLong("id"),opcional(r,"solucion_id"),
            opcional(r,"paso_id"),null,r.getLong("version"),fecha(r),r.getString("texto"),null,null,null,null);}
    private void agregar(List<Fuente> fuentes,Fuente fuente){
        if(fuentes.size()>=config.maxFuentes() || fuente.texto().length()+fuentes.stream().mapToLong(f->f.texto().length()).sum()>200000
                || (fuente.archivoId()!=null && fuentes.stream().filter(f->f.archivoId()!=null).count()>=config.maxAdjuntos()))throw new IllegalArgumentException("LIMITE_FUENTES");
        fuentes.add(fuente);
    }
    private void leer(String sql,RowMapper<Fuente> mapper,List<Fuente> fuentes,long... parametros){
        jdbc.query(conexion->{var sentencia=conexion.prepareStatement(sql);sentencia.setFetchSize(16);
            for(int i=0;i<parametros.length;i++)sentencia.setLong(i+1,parametros[i]);return sentencia;
        },(ResultSetExtractor<Void>)resultado->{int fila=0;while(resultado.next())agregar(fuentes,mapper.mapRow(resultado,fila++));return null;});
    }
    private static Long opcional(ResultSet r,String columna)throws SQLException{long valor=r.getLong(columna);return r.wasNull()?null:valor;}
    private static Instant fecha(ResultSet r)throws SQLException{return r.getTimestamp("modificado_en").toInstant();}
}
