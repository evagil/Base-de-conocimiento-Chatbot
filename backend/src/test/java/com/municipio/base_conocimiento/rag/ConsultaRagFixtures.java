package com.municipio.base_conocimiento.rag;

import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.rag.config.ConfiguracionConsultaRag;
import com.municipio.base_conocimiento.rag.repository.BusquedaRagRepository.Registro;
import java.net.URI;
import java.time.Duration;

final class ConsultaRagFixtures {
    static final String EVIDENCIA="Cerrar y volver a abrir la aplicación para corregir E17.";
    static ConfiguracionConsultaRag config(boolean habilitado,boolean semantica){return new ConfiguracionConsultaRag(habilitado,semantica,1000,1000,3,1600,0.65,Duration.ofSeconds(240));}
    static ConfiguracionIa ia(){return new ConfiguracionIa(true,URI.create("http://127.0.0.1:11434"),"granite4.2:3b","granite4.2:3b","qwen2.5vl:3b","embeddinggemma:300m",Duration.ofSeconds(120),Duration.ofSeconds(2),2048,4000,128,8,1048576);}
    static Registro registro(long id,String texto,String vector){return new Registro(id,7L,8L,null,null,8L,"SOLUCION","Error E17",null,texto,"h".repeat(64),"embeddinggemma:300m",2,vector,1);}
}
