package com.municipio.base_conocimiento.ia;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.ia.client.*;
import com.municipio.base_conocimiento.ia.config.*;
import com.municipio.base_conocimiento.ia.dto.MensajeIa;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

/** Opt-in: solo texto sintético. Las suites habituales no requieren Ollama. */
@EnabledIfEnvironmentVariable(named = "BC_TEST_OLLAMA_URL", matches = "http://(127\\.0\\.0\\.1|localhost):[0-9]+/?")
class OllamaLocalSmokeTest {
    @Test void clienteJavaConsultaElModeloLocalSinUsarDatosReales() {
        var configuracion = new ConfiguracionIa(true, URI.create(System.getenv("BC_TEST_OLLAMA_URL")),
                "granite4.2:3b", "granite4.2:3b", "qwen2.5vl:3b", "embeddinggemma:300m",
                Duration.ofSeconds(120), Duration.ofSeconds(2), 2048, 4000, 128, 8, 1048576);
        try (var http = new OllamaConfiguration().ollamaHttpClient(configuracion)) {
            var cliente = new ClienteOllama(configuracion, http, new ObjectMapper());
            assertThat(cliente.estado().disponible()).isTrue();
            long inicio = System.nanoTime();
            var respuesta = cliente.conversar(ClienteConversacion.Uso.CHATBOT, List.of(
                    new MensajeIa("system", "Respondé en español usando únicamente la fuente. Si falta información, respondé: Información insuficiente."),
                    new MensajeIa("user", "Fuente: Si el error E17 impide imprimir, cerrar la aplicación y volver a abrirla. Pregunta: ¿Qué debo hacer si aparece E17?")));
            assertThat(respuesta.modelo()).isEqualTo("granite4.2:3b");
            assertThat(respuesta.texto().toLowerCase(java.util.Locale.ROOT)).containsAnyOf("cerr", "cierr").containsAnyOf("abr", "abre");
            System.out.printf("OLLAMA_LOCAL modelo=%s tokens=%d segundos=%.2f%n", respuesta.modelo(), respuesta.tokensGenerados(), (System.nanoTime() - inicio) / 1e9);
        }
    }
}
