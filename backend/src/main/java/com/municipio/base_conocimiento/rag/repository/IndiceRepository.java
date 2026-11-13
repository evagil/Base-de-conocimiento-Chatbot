package com.municipio.base_conocimiento.rag.repository;

import com.municipio.base_conocimiento.rag.dto.FuentesIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.Fragmento;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Repository
public class IndiceRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public IndiceRepository(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
    public record Trabajo(long problemaId,long generacion,String token){}
    public record Estado(String huella,String estado,int intentos,Instant reintentar){}
    @Transactional public Trabajo tomar(){
        var control=control();
        if(control.vence()!=null && control.vence().isAfter(Instant.now()))return null;
        var siguientes=jdbc.queryForList("SELECT id FROM problemas WHERE id>? ORDER BY id LIMIT 1",Long.class,control.cursor());
        if(siguientes.isEmpty()){jdbc.update("UPDATE rag_control SET cursor_id=0,token=NULL,vence_en=NULL WHERE id=1");return null;}
        long id=siguientes.getFirst();String token=UUID.randomUUID().toString();
        jdbc.update("UPDATE rag_control SET cursor_id=?,token=?,vence_en=? WHERE id=1",id,token,Timestamp.from(Instant.now().plusSeconds(600)));
        return new Trabajo(id,control.generacion(),token);
    }
    public Estado estado(long id){
        return jdbc.query("SELECT * FROM rag_estado WHERE problema_id=?",(r,i)->new Estado(r.getString("huella"),r.getString("estado"),r.getInt("intentos"),
                r.getTimestamp("reintentar_en")==null?null:r.getTimestamp("reintentar_en").toInstant()),id).stream().findFirst().orElse(null);
    }
    @Transactional public void liberar(Trabajo trabajo){jdbc.update("UPDATE rag_control SET token=NULL,vence_en=NULL WHERE id=1 AND token=?",trabajo.token());}
    @Transactional public boolean guardar(Trabajo trabajo,FuentesIndice fuentes,String huella,List<Fragmento> partes,String modelo,List<List<Double>> vectores){
        if(!propietario(trabajo))return false;
        jdbc.update("DELETE FROM rag_fragmentos WHERE problema_id=?",trabajo.problemaId());
        for(int i=0;i<partes.size();i++){
            var parte=partes.get(i);var origen=parte.fuente();List<Double> vector=vectores.isEmpty()?null:vectores.get(i);
            jdbc.update("""
                INSERT INTO rag_fragmentos(problema_id,tipo,origen_id,solucion_id,paso_id,archivo_id,area_id,subarea_id,
                estado,activo,version_origen,version_problema,modificado_en,pagina,posicion,texto,huella,modelo,dimension,vector_json)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,fuentes.problemaId(),origen.tipo(),origen.id(),origen.solucionId(),origen.pasoId(),origen.archivoId(),fuentes.areaId(),fuentes.subareaId(),
                fuentes.estado(),fuentes.activo(),origen.version(),fuentes.version(),Timestamp.from(origen.modificado()),parte.pagina(),i,parte.texto(),huella,
                vector==null?null:modelo,vector==null?null:vector.size(),vector==null?null:json.writeValueAsString(vector));
        }
        escribirEstado(trabajo.problemaId(),huella,"LISTO",0,null,null);return true;
    }
    @Transactional public void fallar(Trabajo trabajo,String huella,String codigo,int intentos){
        if(!propietario(trabajo))return;
        jdbc.update("DELETE FROM rag_fragmentos WHERE problema_id=?",trabajo.problemaId());
        escribirEstado(trabajo.problemaId(),huella,"ERROR",intentos,codigo,Instant.now().plusSeconds(60L*intentos));
    }
    @Transactional public void reconstruir(boolean soloErrores){
        control(); // Serializa la orden con publicación y toma de trabajos, no con la inferencia.
        jdbc.update("UPDATE rag_control SET cursor_id=0,generacion=generacion+1,token=NULL,vence_en=NULL WHERE id=1");
        jdbc.update("UPDATE rag_estado SET huella='',estado='PENDIENTE',intentos=0,codigo_error=NULL,reintentar_en=NULL"+(soloErrores?" WHERE estado='ERROR'":""));
    }
    public Map<String,Long> resumen(){
        Map<String,Long> valores=new LinkedHashMap<>();
        for(String estado:List.of("PENDIENTE","LISTO","ERROR"))valores.put(estado.toLowerCase(Locale.ROOT),jdbc.queryForObject("SELECT count(*) FROM rag_estado WHERE estado=?",Long.class,estado));
        valores.put("fragmentos",jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos",Long.class));
        valores.put("problemas",jdbc.queryForObject("SELECT count(*) FROM problemas",Long.class));
        valores.put("cursor",jdbc.queryForObject("SELECT cursor_id FROM rag_control WHERE id=1",Long.class));return Map.copyOf(valores);
    }
    private boolean propietario(Trabajo trabajo){var control=control();return trabajo.token().equals(control.token()) && trabajo.generacion()==control.generacion()
            && control.vence()!=null && control.vence().isAfter(Instant.now());}
    private record Control(long cursor,long generacion,String token,Instant vence){}
    private Control control(){return jdbc.queryForObject("SELECT * FROM rag_control WHERE id=1 FOR UPDATE",(r,i)->new Control(r.getLong("cursor_id"),r.getLong("generacion"),
            r.getString("token"),r.getTimestamp("vence_en")==null?null:r.getTimestamp("vence_en").toInstant()));}
    private void escribirEstado(long id,String huella,String estado,int intentos,String codigo,Instant proximo){
        var fecha=proximo==null?null:Timestamp.from(proximo);
        if(jdbc.update("UPDATE rag_estado SET huella=?,estado=?,intentos=?,codigo_error=?,reintentar_en=?,actualizado_en=CURRENT_TIMESTAMP WHERE problema_id=?",
                huella,estado,intentos,codigo,fecha,id)==0)jdbc.update("INSERT INTO rag_estado(problema_id,huella,estado,intentos,codigo_error,reintentar_en) VALUES(?,?,?,?,?,?)",id,huella,estado,intentos,codigo,fecha);
    }
}
