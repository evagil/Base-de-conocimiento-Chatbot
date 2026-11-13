package com.municipio.base_conocimiento.rag.service;

import com.municipio.base_conocimiento.ia.client.ClienteEmbeddings;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.*;
import com.municipio.base_conocimiento.rag.repository.*;
import java.time.*;
import java.util.*;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class IndexadorConocimiento {
    private final ConfiguracionIndice config;
    private final ConfiguracionIa ia;
    private final FuentesIndiceRepository fuentes;
    private final IndiceRepository indice;
    private final Fragmentador fragmentador;
    private final LectorAdjuntoIndice adjuntos;
    private final ClienteEmbeddings embeddings;
    private final ObjectMapper json;
    public IndexadorConocimiento(ConfiguracionIndice config,ConfiguracionIa ia,FuentesIndiceRepository fuentes,IndiceRepository indice,
            Fragmentador fragmentador,LectorAdjuntoIndice adjuntos,ClienteEmbeddings embeddings,ObjectMapper json){
        this.config=config;this.ia=ia;this.fuentes=fuentes;this.indice=indice;this.fragmentador=fragmentador;this.adjuntos=adjuntos;this.embeddings=embeddings;this.json=json;
    }
    @Scheduled(fixedDelayString="${app.indice.intervalo:15000}",initialDelayString="${app.indice.inicio:30000}")
    public void ciclo(){
        if(!config.habilitado())return;
        try{for(int i=0;i<10 && procesarSiguiente();i++){ /* Barrido acotado; nunca se carga todo el catálogo. */ }}
        catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("No se pudo ejecutar el ciclo de indexación; se reintentará sin afectar el conocimiento.");}
    }
    public boolean procesarSiguiente(){
        if(!config.habilitado())return false;
        var trabajo=indice.tomar();if(trabajo==null)return false;
        String huella="";var previo=indice.estado(trabajo.problemaId());
        int intentos=1;
        long limite=System.nanoTime()+config.timeout().toNanos();
        try{
            if(previo!=null && previo.huella().isEmpty()){
                if(previo.intentos()>=3 || (previo.reintentar()!=null && previo.reintentar().isAfter(Instant.now())))return true;
                intentos=previo.intentos()+1;
            }
            var snapshot=fuentes.obtener(trabajo.problemaId());
            huella=huella(snapshot);
            if(previo!=null && previo.huella().equals(huella)){
                if(previo.estado().equals("LISTO") || previo.intentos()>=3 || (previo.reintentar()!=null && previo.reintentar().isAfter(Instant.now())))return true;
                intentos=previo.intentos()+1;
            }
            List<Fragmento> partes=new ArrayList<>();
            String contexto=snapshot.titulo()+"\nÁrea: "+snapshot.area()+"\nSubárea: "+snapshot.subarea()+"\n";
            for(var fuente:snapshot.fuentes()){
                tiempo(limite);
                if(fuente.archivoId()!=null){
                    for(var seccion:adjuntos.leer(fuente).secciones())agregar(partes,fuente,seccion.pagina(),contexto+fuente.texto()+"\n"+seccion.texto());
                }else agregar(partes,fuente,null,contexto+fuente.texto());
            }
            List<List<Double>> vectores=new ArrayList<>();String modelo=null;int dimension=-1;
            if(config.embeddings())for(int inicio=0;inicio<partes.size();inicio+=Math.min(8,ia.maxBatch())){
                tiempo(limite);int fin=Math.min(partes.size(),inicio+Math.min(8,ia.maxBatch()));
                var resultado=embeddings.vectorizar(partes.subList(inicio,fin).stream().map(Fragmento::texto).toList());
                if(!ia.modeloEmbeddings().equals(resultado.modelo()) || resultado.vectores().size()!=fin-inicio)throw new IllegalArgumentException("VECTOR_INVALIDO");
                for(var vector:resultado.vectores()){
                    if(vector.isEmpty() || vector.size()>8192 || (dimension!=-1 && dimension!=vector.size()) || vector.stream().anyMatch(v->v==null || !Double.isFinite(v))
                            || vector.stream().allMatch(v->v==0))throw new IllegalArgumentException("VECTOR_INVALIDO");
                    dimension=vector.size();vectores.add(vector);
                }
                modelo=resultado.modelo();
            }
            tiempo(limite);
            // No publica un snapshot si las fuentes cambiaron durante extracción/inferencia.
            if(!huella.equals(huella(fuentes.obtener(trabajo.problemaId()))))return true;
            indice.guardar(trabajo,snapshot,huella,partes,modelo,vectores);return true;
        }catch(Exception e){
            String codigo=e instanceof IaException error?error.getCodigo():e instanceof DocumentoException error?error.getCodigo():"INDICE_NO_PROCESABLE";
            if(e.getMessage()!=null && Set.of("LIMITE_FUENTES","LIMITE_FRAGMENTOS","INDICE_TIMEOUT","VECTOR_INVALIDO",
                    "LIMITE_ARCHIVO","ADJUNTO_NO_DISPONIBLE","ADJUNTO_INCONSISTENTE").contains(e.getMessage()))codigo=e.getMessage();
            indice.fallar(trabajo,huella,codigo,intentos);return true;
        }finally{indice.liberar(trabajo);}
    }
    public String huella(FuentesIndice snapshot){return LectorAdjuntoIndice.huella(json.writeValueAsBytes(List.of("indice-v1",config.toString(),ia.modeloEmbeddings(),snapshot)));}
    private void agregar(List<Fragmento> partes,Fuente fuente,Integer pagina,String texto){
        for(String parte:fragmentador.dividir(texto)){
            if(partes.size()>=config.maxFragmentos())throw new IllegalArgumentException("LIMITE_FRAGMENTOS");
            partes.add(new Fragmento(fuente,pagina,parte));
        }
    }
    private void tiempo(long limite){if(System.nanoTime()>=limite)throw new IllegalArgumentException("INDICE_TIMEOUT");}
}
