package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.config.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

@EnabledIfEnvironmentVariable(named="BC_TEST_OLLAMA_URL",matches="http://(127\\.0\\.0\\.1|localhost):[0-9]+/?")
class EmbeddingsLocalSmokeTest {
    @Test void generaVectoresLocalesParaTextoSintetico(){
        var config=new ConfiguracionIa(true,URI.create(System.getenv("BC_TEST_OLLAMA_URL")),"granite4.2:3b","granite4.2:3b","qwen2.5vl:3b","embeddinggemma:300m",
            Duration.ofSeconds(120),Duration.ofSeconds(2),2048,4000,128,8,1048576);
        try(var http=new OllamaConfiguration().ollamaHttpClient(config)){
            var cliente=new ClienteOllama(config,http,new ObjectMapper());long inicio=System.nanoTime();
            var resultado=cliente.vectorizar(List.of("El error E17 se corrige reiniciando la aplicación.","Reiniciar la aplicación cuando aparece E17."));
            assertThat(resultado.modelo()).isEqualTo("embeddinggemma:300m");assertThat(resultado.vectores()).hasSize(2);
            assertThat(resultado.vectores().getFirst()).hasSize(768).allSatisfy(v->assertThat(Double.isFinite(v)).isTrue());
            System.out.printf("EMBEDDINGS_LOCAL modelo=%s dimension=%d segundos=%.2f%n",resultado.modelo(),resultado.vectores().getFirst().size(),(System.nanoTime()-inicio)/1e9);
        }
    }
}
