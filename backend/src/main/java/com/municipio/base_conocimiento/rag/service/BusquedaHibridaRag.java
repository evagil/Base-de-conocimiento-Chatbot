package com.municipio.base_conocimiento.rag.service;

import com.municipio.base_conocimiento.ia.client.ClienteEmbeddings;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.municipio.base_conocimiento.rag.config.ConfiguracionConsultaRag;
import com.municipio.base_conocimiento.rag.dto.ResultadoRag;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository.Registro;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.*;

@Service
public class BusquedaHibridaRag {
    private final ConfiguracionConsultaRag config;
    private final ConfiguracionIa ia;
    private final BusquedaRagRepository repo;
    private final ClienteEmbeddings embeddings;
    private final RelevanciaRag relevancia;
    private final VigenciaFuentesRag vigencia;
    private final ObjectMapper json;
    private final Semaphore turno=new Semaphore(1);
    public BusquedaHibridaRag(ConfiguracionConsultaRag config,ConfiguracionIa ia,BusquedaRagRepository repo,
            ClienteEmbeddings embeddings,RelevanciaRag relevancia,VigenciaFuentesRag vigencia,ObjectMapper json){
        this.config=config;this.ia=ia;this.repo=repo;this.embeddings=embeddings;this.relevancia=relevancia;this.vigencia=vigencia;this.json=json;
    }
    public record Coincidencia(Registro registro,double puntuacion){}
    public record Seleccion(String estado,String modo,boolean acotada,List<Coincidencia> coincidencias){
        public ResultadoRag.Busqueda respuesta(){return new ResultadoRag.Busqueda(estado,modo,acotada,coincidencias.stream().map(c->fuente(c.registro(),c.puntuacion(),c.registro().texto())).toList());}
    }
    public Seleccion buscar(String pregunta){
        validar(pregunta);
        if(!turno.tryAcquire())throw error(HttpStatus.TOO_MANY_REQUESTS,"RAG_OCUPADO","Hay otra búsqueda en curso. Intentá nuevamente en unos instantes.");
        long limite=System.nanoTime()+config.timeout().toNanos();
        try{
            String modo=config.semantica()?"HIBRIDA":"TEXTUAL";
            if(!repo.disponible())return new Seleccion("INDICE_NO_LISTO",modo,false,List.of());
            List<String> terminos=relevancia.terminos(pregunta);
            if(terminos.isEmpty())throw error(HttpStatus.BAD_REQUEST,"RAG_PREGUNTA_INVALIDA","Describí el problema con más detalle.");
            int capacidad=config.maxResultados()*4;
            List<Valor> texto=new ArrayList<>(),semantica=new ArrayList<>();boolean acotada;
            if(repo.postgres()){
                var filas=repo.textual(String.join(" OR ",terminos),config.maxCandidatos()+1);
                acotada=filas.size()>config.maxCandidatos();
                filas.stream().limit(config.maxCandidatos()).filter(r->relevancia.coincideCodigo(pregunta,r.titulo()+" "+r.texto()))
                    .forEach(r->mejor(texto,new Valor(r,r.rango()),capacidad));
            }else{
                acotada=repo.recorrer(config.maxCandidatos(),null,null,r->{
                    double valor=relevancia.textual(terminos,r.titulo()+" "+r.texto());
                    if(valor>=0.35 && relevancia.coincideCodigo(pregunta,r.titulo()+" "+r.texto()))mejor(texto,new Valor(r,valor),capacidad);
                });
            }
            if(config.semantica()){
                tiempo(limite);
                var resultado=embeddings.vectorizar(List.of(pregunta));
                if(resultado==null || !ia.modeloEmbeddings().equals(resultado.modelo()) || resultado.vectores().size()!=1
                        || relevancia.coseno(resultado.vectores().getFirst(),resultado.vectores().getFirst())<0.99)
                    throw error(HttpStatus.BAD_GATEWAY,"RAG_VECTOR_INVALIDO","El modelo devolvió un vector no utilizable.");
                List<Double> consulta=resultado.vectores().getFirst();
                acotada|=repo.recorrer(config.maxCandidatos(),resultado.modelo(),consulta.size(),r->{
                    tiempo(limite);
                    if(r.vector()==null || !relevancia.coincideCodigo(pregunta,r.titulo()+" "+r.texto()))return;
                    try{
                        JsonNode nodo=json.readTree(r.vector());if(!nodo.isArray() || nodo.size()!=consulta.size())return;
                        List<Double> valores=new ArrayList<>();for(JsonNode valor:nodo){if(!valor.isNumber())return;valores.add(valor.asDouble());}
                        double similitud=relevancia.coseno(consulta,valores);
                        if(similitud>=config.similitudMinima())mejor(semantica,new Valor(r,similitud),capacidad);
                    }catch(tools.jackson.core.JacksonException e){ /* Se omite un vector corrupto, sin exponerlo. */ }
                });
            }
            tiempo(limite);
            Map<Long,Coincidencia> fusion=new HashMap<>();fusionar(texto,fusion);fusionar(semantica,fusion);
            Map<Long,String> huellas=new HashMap<>();List<Coincidencia> salida=new ArrayList<>();
            for(var opcion:fusion.values().stream().sorted(Comparator.comparingDouble(Coincidencia::puntuacion).reversed()
                    .thenComparingLong(c->c.registro().id())).toList()){
                tiempo(limite);
                if(vigencia.comprobar(opcion.registro(),huellas))salida.add(opcion);
                if(salida.size()==config.maxResultados())break;
            }
            if(!vigencia.todas(salida.stream().map(Coincidencia::registro).toList()))salida.clear();
            tiempo(limite);
            return new Seleccion(salida.isEmpty()?"SIN_COINCIDENCIAS":"COINCIDENCIAS",modo,acotada,List.copyOf(salida));
        }catch(org.springframework.dao.QueryTimeoutException e){throw error(HttpStatus.GATEWAY_TIMEOUT,"RAG_TIMEOUT","El índice tardó demasiado en responder.");}
        catch(org.springframework.dao.DataAccessException e){throw error(HttpStatus.SERVICE_UNAVAILABLE,"RAG_INDICE_NO_DISPONIBLE","No se pudo consultar el índice. Intentá nuevamente.");}
        finally{turno.release();}
    }
    private record Valor(Registro registro,double valor){}
    private void mejor(List<Valor> lista,Valor valor,int maximo){lista.add(valor);lista.sort(Comparator.comparingDouble(Valor::valor).reversed().thenComparingLong(v->v.registro().id()));if(lista.size()>maximo)lista.removeLast();}
    private void fusionar(List<Valor> lista,Map<Long,Coincidencia> fusion){for(int i=0;i<lista.size();i++){
        var r=lista.get(i).registro();double puntos=61.0/(2*(61+i));
        fusion.compute(r.id(),(id,anterior)->new Coincidencia(r,puntos+(anterior==null?0:anterior.puntuacion())));
    }}
    public static ResultadoRag.Fuente fuente(Registro r,double puntuacion,String extracto){return new ResultadoRag.Fuente(r.problemaId(),r.solucionId(),r.pasoId(),r.archivoId(),
            r.tipo(),r.titulo(),r.pagina(),extracto,"/problemas/"+r.problemaId(),r.archivoId()==null?null:"/api/archivos/"+r.archivoId()+"/contenido",Math.round(puntuacion*10000)/10000.0);}
    private void validar(String pregunta){
        if(!config.habilitado())throw error(HttpStatus.SERVICE_UNAVAILABLE,"RAG_DESHABILITADO","La consulta del conocimiento con IA está deshabilitada.");
        if(pregunta==null || pregunta.isBlank() || pregunta.length()>config.maxPregunta() || pregunta.codePoints().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\t'))
            throw error(HttpStatus.BAD_REQUEST,"RAG_PREGUNTA_INVALIDA","Ingresá una consulta de tamaño permitido, sin caracteres de control.");
    }
    private void tiempo(long limite){if(System.nanoTime()>=limite)throw error(HttpStatus.GATEWAY_TIMEOUT,"RAG_TIMEOUT","La búsqueda tardó demasiado. Acotá la consulta e intentá nuevamente.");}
    private IaException error(HttpStatus estado,String codigo,String mensaje){return new IaException(estado,codigo,mensaje);}
}
