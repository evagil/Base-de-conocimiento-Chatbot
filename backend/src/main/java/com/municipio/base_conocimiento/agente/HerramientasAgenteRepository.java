package com.municipio.base_conocimiento.agente;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Únicamente consultas parametrizadas de texto publicado; no recibe SQL del modelo. */
@Repository @Transactional(readOnly=true,timeout=15)
public class HerramientasAgenteRepository {
 private final JdbcTemplate jdbc;
 public HerramientasAgenteRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Solucion(long id,long problemaId,String titulo,String texto,boolean principal,int orden){}
 public record Paso(long id,long solucionId,String titulo,String texto,int orden){}
 private static final String VISIBLE=" JOIN problemas p ON p.id=s.problema_id JOIN subareas sub ON sub.id=p.subarea_id JOIN areas a ON a.id=sub.area_id WHERE p.activo=TRUE AND p.estado='PUBLICADO' AND sub.activo=TRUE AND a.activo=TRUE AND s.activo=TRUE ";
 public List<Solucion> soluciones(long problema){
  return jdbc.query("SELECT s.id,s.problema_id,COALESCE(s.titulo,'Solución') titulo,substring(s.descripcion_texto,1,1201) texto,s.principal,s.orden FROM soluciones s"+VISIBLE+"AND p.id=? ORDER BY s.principal DESC,s.orden,s.id LIMIT 6",
   (r,n)->new Solucion(r.getLong("id"),r.getLong("problema_id"),r.getString("titulo"),r.getString("texto"),r.getBoolean("principal"),r.getInt("orden")),problema);
 }
 public List<Paso> pasos(long solucion){
  return jdbc.query("SELECT ps.id,ps.solucion_id,COALESCE(ps.titulo,'Paso') titulo,substring(ps.contenido_texto,1,601) texto,ps.orden FROM pasos_solucion ps JOIN soluciones s ON s.id=ps.solucion_id"+VISIBLE+"AND ps.activo=TRUE AND s.id=? ORDER BY ps.orden,ps.id LIMIT 21",
   (r,n)->new Paso(r.getLong("id"),r.getLong("solucion_id"),r.getString("titulo"),r.getString("texto"),r.getInt("orden")),solucion);
 }
}
