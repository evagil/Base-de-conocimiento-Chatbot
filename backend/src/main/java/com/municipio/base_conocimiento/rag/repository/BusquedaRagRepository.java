package com.municipio.base_conocimiento.rag.repository;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BusquedaRagRepository {
    private final JdbcTemplate jdbc;
    public BusquedaRagRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Registro(long id,long problemaId,Long solucionId,Long pasoId,Long archivoId,long origenId,
            String tipo,String titulo,Integer pagina,String texto,String huella,String modelo,Integer dimension,String vector,double rango) { }
    private static final String COLUMNAS="f.id,f.problema_id,f.solucion_id,f.paso_id,f.archivo_id,f.origen_id,f.tipo,"
            +"f.pagina,substring(f.texto,1,1200) texto,f.huella,f.modelo,f.dimension,p.titulo titulo_problema";
    public boolean disponible(){return jdbc.queryForObject("SELECT count(*) FROM rag_fragmentos_vigentes",Long.class)>0;}
    public boolean postgres(){return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>)c->c.getMetaData().getDatabaseProductName().equals("PostgreSQL")));}
    @Transactional(readOnly=true,timeout=15)
    public List<Registro> textual(String consulta,int maximo){
        return jdbc.query("SELECT "+COLUMNAS+",NULL vector_json,ts_rank_cd(to_tsvector('spanish',f.texto),websearch_to_tsquery('spanish',?)) rango "
                +"FROM rag_fragmentos_vigentes f JOIN problemas p ON p.id=f.problema_id "
                +"WHERE to_tsvector('spanish',f.texto) @@ websearch_to_tsquery('spanish',?) ORDER BY rango DESC,f.id LIMIT ?",
                (r,n)->mapear(r,r.getDouble("rango"),false),consulta,consulta,maximo);
    }
    @Transactional(readOnly=true,timeout=15)
    public boolean recorrer(int maximo,String modelo,Integer dimension,Consumer<Registro> consumidor){
        String condicion=modelo==null?"":" WHERE f.modelo=? AND f.dimension=?";
        return Boolean.TRUE.equals(jdbc.query(c->{
            var ps=c.prepareStatement("SELECT "+COLUMNAS+(modelo==null?",NULL vector_json":",substring(f.vector_json,1,262145) vector_json")
                    +" FROM rag_fragmentos_vigentes f JOIN problemas p ON p.id=f.problema_id"+condicion+" ORDER BY f.id LIMIT ?");
            ps.setFetchSize(16);int i=1;if(modelo!=null){ps.setString(i++,modelo);ps.setInt(i++,dimension);}ps.setInt(i,maximo+1);return ps;
        },(ResultSetExtractor<Boolean>)r->{int leidos=0;while(r.next()){if(leidos++==maximo)return true;consumidor.accept(mapear(r,0,modelo!=null));}return false;}));
    }
    public Optional<Registro> vigente(long id){return jdbc.query("SELECT "+COLUMNAS+",NULL vector_json FROM rag_fragmentos_vigentes f JOIN problemas p ON p.id=f.problema_id WHERE f.id=?",
            (r,n)->mapear(r,0,false),id).stream().findFirst();}
    private Registro mapear(ResultSet r,double rango,boolean vector)throws SQLException{
        String texto=r.getString("texto");if(texto.length()>1200)texto=texto.substring(0,1200);
        String datos=vector?r.getString("vector_json"):null;
        if(datos!=null && datos.length()>262144)datos=null;
        return new Registro(r.getLong("id"),r.getLong("problema_id"),opcional(r,"solucion_id"),opcional(r,"paso_id"),opcional(r,"archivo_id"),r.getLong("origen_id"),
                r.getString("tipo"),r.getString("titulo_problema"),(Integer)r.getObject("pagina"),texto,r.getString("huella"),r.getString("modelo"),(Integer)r.getObject("dimension"),datos,rango);
    }
    private Long opcional(ResultSet r,String nombre)throws SQLException{long valor=r.getLong(nombre);return r.wasNull()?null:valor;}
}
