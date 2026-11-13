package com.municipio.base_conocimiento.ia;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.municipio.base_conocimiento.ia.client.*;
import com.municipio.base_conocimiento.ia.config.*;
import com.municipio.base_conocimiento.ia.dto.*;
import com.municipio.base_conocimiento.ia.exception.IaException;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.*;
import tools.jackson.databind.*;

class ClienteOllamaTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer servidor;
    private ExecutorService executor;
    private HttpClient http;
    private ClienteOllama cliente;
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private final List<JsonNode> peticiones = new CopyOnWriteArrayList<>();
    private volatile BiFunction<String, JsonNode, Respuesta> responder;
    private record Respuesta(int status, String tipo, String body) { }

    @BeforeEach void preparar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        servidor.setExecutor(executor);
        responder = this::normal;
        servidor.createContext("/", intercambio -> {
            try (intercambio) {
                String ruta = intercambio.getRequestURI().getPath();
                byte[] bytes = intercambio.getRequestBody().readAllBytes();
                JsonNode solicitud = bytes.length == 0 ? json.createObjectNode() : json.readTree(bytes);
                rutas.add(ruta);
                peticiones.add(solicitud);
                Respuesta respuesta = responder.apply(ruta, solicitud);
                byte[] body = respuesta.body().getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().set("Content-Type", respuesta.tipo());
                intercambio.sendResponseHeaders(respuesta.status(), body.length);
                intercambio.getResponseBody().write(body);
            } catch (IOException ignored) { /* El cliente puede cancelar por timeout o límite. */ }
        });
        servidor.start();
        var configuracion = configuracion(true, Duration.ofSeconds(4), 1048576);
        http = new OllamaConfiguration().ollamaHttpClient(configuracion);
        cliente = new ClienteOllama(configuracion, http, json);
    }

    @AfterEach void cerrar() {
        servidor.stop(0);
        executor.shutdownNow();
        http.close();
    }

    private ConfiguracionIa configuracion(boolean habilitada, Duration timeout, int maxBytes) {
        return new ConfiguracionIa(habilitada, URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()),
                "gemma3:1b", "qwen3:4b", "qwen2.5vl:3b", "embeddinggemma:300m",
                timeout, Duration.ofSeconds(1), 2048, 4000, 128, 8, maxBytes);
    }

    private Respuesta normal(String ruta, JsonNode solicitud) {
        return ok(switch (ruta) {
            case "/api/show" -> "{\"capabilities\":[\"completion\",\"thinking\",\"embedding\"]}";
            case "/api/chat" -> json.writeValueAsString(Map.of("model", solicitud.path("model").asText(), "done", true,
                    "message", Map.of("role", "assistant", "content", "Cerrar y volver a abrir.", "thinking", "No exponer"),
                    "done_reason", "stop", "total_duration", 100, "eval_count", 8));
            case "/api/embed" -> json.writeValueAsString(Map.of("model", "embeddinggemma:300m", "embeddings",
                    java.util.stream.IntStream.range(0, solicitud.path("input").size()).mapToObj(i -> List.of(0.2, -0.4)).toList()));
            case "/api/version" -> "{\"version\":\"0.32.6\"}";
            case "/api/tags" -> "{\"models\":[{\"name\":\"gemma3:1b\"},{\"name\":\"qwen3:4b\"}]}";
            default -> throw new AssertionError("Ruta no autorizada: " + ruta);
        });
    }

    private Respuesta ok(String body) { return new Respuesta(200, "application/json; charset=utf-8", body); }
    private List<MensajeIa> consulta() { return List.of(new MensajeIa("system", "Usá solo la fuente."), new MensajeIa("user", "Error E17")); }
    private RespuestaIa conversar() { return cliente.conversar(ClienteConversacion.Uso.CHATBOT, consulta()); }
    private void codigo(String esperado, org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(IaException.class, error -> {
            assertThat(error.getCodigo()).isEqualTo(esperado);
            assertThat(error.getMessage()).doesNotContain("contenido privado");
        });
    }

    @Test void conversaSinStreamingNiPensamientoYConLimites() {
        var respuesta = conversar();
        assertThat(respuesta.modelo()).isEqualTo("gemma3:1b");
        assertThat(respuesta.texto()).isEqualTo("Cerrar y volver a abrir.");
        assertThat(respuesta.tokensGenerados()).isEqualTo(8);
        assertThat(respuesta.duracionTotalNs()).isEqualTo(100);
        JsonNode body = peticiones.getLast();
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("think").asBoolean()).isFalse();
        assertThat(body.path("keep_alive").asInt()).isZero();
        assertThat(body.path("options").path("num_ctx").asInt()).isEqualTo(2048);
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("system");
        responder = (ruta, solicitud) -> ruta.equals("/api/show") ? ok("{\"capabilities\":[\"completion\"]}") : normal(ruta, solicitud);
        cliente.conversar(ClienteConversacion.Uso.AGENTE, consulta());
        assertThat(peticiones.getLast().path("model").asText()).isEqualTo("qwen3:4b");
        assertThat(peticiones.getLast().path("think").asBoolean()).isFalse();
        assertThat(peticiones.getLast().has("images")).isFalse();
    }

    @Test void estadoNoGeneraTextoYAdmiteModelosTodaviaNoInstalados() {
        var estado = cliente.estado();
        assertThat(estado.habilitada()).isTrue();
        assertThat(estado.disponible()).isTrue();
        assertThat(estado.versionOllama()).isEqualTo("0.32.6");
        assertThat(estado.modelos()).extracting(EstadoIa.EstadoModelo::instalado).containsExactly(true, true, false, false);
        assertThat(rutas).containsExactly("/api/version", "/api/tags");
    }

    @Test void permiteJsonEstructuradoSinEjecutarHerramientas() {
        Map<String, Object> esquema = Map.of("type", "object", "properties", Map.of("herramienta", Map.of("type", "string")));
        responder = (ruta, solicitud) -> ruta.equals("/api/chat")
                ? ok(json.writeValueAsString(Map.of("model", solicitud.path("model").asText(), "done", true,
                        "message", Map.of("role", "assistant", "content", "{\"herramienta\":\"buscar_problema\"}")))) : normal(ruta, solicitud);
        var respuesta = cliente.conversar(ClienteConversacion.Uso.AGENTE, consulta(), esquema);
        assertThat(respuesta.texto()).contains("buscar_problema");
        assertThat(peticiones.getLast().path("format").path("type").asText()).isEqualTo("object");
        for (String texto : List.of("no JSON", "null", "[]")) {
            responder = (ruta, solicitud) -> ruta.equals("/api/chat")
                    ? ok(json.writeValueAsString(Map.of("model", solicitud.path("model").asText(), "done", true,
                            "message", Map.of("role", "assistant", "content", texto)))) : normal(ruta, solicitud);
            codigo("IA_RESPUESTA_INVALIDA", () -> cliente.conversar(ClienteConversacion.Uso.AGENTE, consulta(), esquema));
        }
        codigo("IA_ENTRADA_INVALIDA", () -> cliente.conversar(ClienteConversacion.Uso.AGENTE, consulta(), Map.of("type", "array")));
        codigo("IA_ENTRADA_INVALIDA", () -> cliente.conversar(ClienteConversacion.Uso.AGENTE, consulta(), Map.of("type", "object", "description", "x".repeat(4001))));
    }

    @Test void deshabilitadaNoHaceLlamadasNiImpideElEstado() {
        cliente = new ClienteOllama(configuracion(false, Duration.ofSeconds(1), 1024), http, json);
        codigo("IA_DESHABILITADA", this::conversar);
        codigo("IA_DESHABILITADA", () -> cliente.vectorizar(List.of("Texto")));
        assertThat(cliente.estado().habilitada()).isFalse();
        assertThat(cliente.estado().disponible()).isFalse();
        assertThat(rutas).isEmpty();
    }

    @Test void rechazaEntradasAntesDeEnviarElContenido() {
        codigo("IA_ENTRADA_INVALIDA", () -> cliente.conversar(null, consulta()));
        codigo("IA_ENTRADA_INVALIDA", () -> cliente.conversar(ClienteConversacion.Uso.CHATBOT, null));
        for (List<MensajeIa> mensajes : List.of(List.<MensajeIa>of(), Collections.nCopies(21, new MensajeIa("user", "Hola")),
                Arrays.asList((MensajeIa) null), List.of(new MensajeIa(null, "Texto")), List.of(new MensajeIa("tool", "Texto")),
                List.of(new MensajeIa("user", null)), List.of(new MensajeIa("user", " ")), List.of(new MensajeIa("user", "x".repeat(4001))))) {
            codigo("IA_ENTRADA_INVALIDA", () -> cliente.conversar(ClienteConversacion.Uso.CHATBOT, mensajes));
        }
        codigo("IA_ENTRADA_INVALIDA", () -> cliente.vectorizar(null));
        for (List<String> textos : List.of(List.<String>of(), Collections.nCopies(9, "Texto"), Arrays.asList((String) null), List.of(" "), List.of("x".repeat(4001)))) {
            codigo("IA_ENTRADA_INVALIDA", () -> cliente.vectorizar(textos));
        }
        assertThat(rutas).isEmpty();
    }

    @Test void diferenciaFallosHttpSinExponerElMensajeRemoto() {
        for (int status : new int[]{404, 500, 429, 400, 302}) {
            responder = (ruta, solicitud) -> new Respuesta(status, "text/plain", "contenido privado");
            codigo(status == 404 ? "IA_MODELO_NO_INSTALADO" : status == 500 || status == 429 ? "IA_NO_DISPONIBLE" : "IA_RESPUESTA_INVALIDA", this::conversar);
        }
        for (Respuesta respuesta : List.of(new Respuesta(200, "text/plain", "{}"), ok("no es JSON"), ok("null"), ok("[]"), ok("{\"error\":\"contenido privado\"}"))) {
            responder = (ruta, solicitud) -> respuesta;
            codigo("IA_RESPUESTA_INVALIDA", this::conversar);
        }
        responder = (ruta, solicitud) -> new Respuesta(404, "application/json", "{}");
        codigo("IA_RESPUESTA_INVALIDA", cliente::estado);
    }

    @Test void rechazaModelosRemotosYCapacidadesIncorrectas() {
        for (String detalle : List.of("{\"remote_host\":\"https://example.invalid\"}", "{\"remote_model\":\"remoto\"}")) {
            responder = (ruta, solicitud) -> ok(detalle);
            codigo("IA_MODELO_REMOTO", this::conversar);
        }
        for (String detalle : List.of("{}", "{\"capabilities\":[\"embedding\"]}")) {
            responder = (ruta, solicitud) -> ok(detalle);
            codigo("IA_MODELO_INCOMPATIBLE", this::conversar);
        }
        responder = (ruta, solicitud) -> ok("{\"capabilities\":[\"completion\",\"thinking\"],\"model_info\":{\"general.finetune\":\"Thinking\"}}");
        codigo("IA_MODELO_INCOMPATIBLE", this::conversar);
        assertThat(rutas).doesNotContain("/api/chat");
    }

    @Test void rechazaRespuestasIncompletasOInconsistentes() {
        for (String body : List.of("{}", "{\"done\":true}",
                "{\"done\":true,\"model\":\"gemma3:1b\",\"message\":{\"role\":\"assistant\",\"content\":\" \"}}",
                "{\"done\":true,\"model\":\"otro\",\"message\":{\"role\":\"assistant\",\"content\":\"Texto\"}}",
                "{\"done\":true,\"model\":\"gemma3:1b\",\"message\":{\"role\":\"user\",\"content\":\"Texto\"}}",
                "{\"done\":true,\"model\":\"gemma3:1b\",\"message\":{\"role\":\"assistant\",\"content\":\"Texto\",\"tool_calls\":[{}]}}")) {
            responder = (ruta, solicitud) -> ruta.equals("/api/chat") ? ok(body) : normal(ruta, solicitud);
            codigo("IA_RESPUESTA_INVALIDA", this::conversar);
        }
        responder = (ruta, solicitud) -> ruta.equals("/api/chat") ? ok(normal(ruta, solicitud).body().replace("stop", "length")) : normal(ruta, solicitud);
        codigo("IA_RESPUESTA_INCOMPLETA", this::conversar);
    }

    @Test void validaEmbeddingsYNoLosTruncaSilenciosamente() {
        var resultado = cliente.vectorizar(List.of("Error", "Solución"));
        assertThat(resultado.modelo()).isEqualTo("embeddinggemma:300m");
        assertThat(resultado.vectores()).containsExactly(List.of(0.2, -0.4), List.of(0.2, -0.4));
        assertThatThrownBy(() -> resultado.vectores().getFirst().add(1.0)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(peticiones.getLast().path("truncate").asBoolean()).isFalse();
        for (String vectores : List.of("null", "[]", "[[]]", "[[0,0]]", "[[\"no\",1]]", "[[1,2],[3]]")) {
            responder = (ruta, solicitud) -> ruta.equals("/api/embed") ? ok("{\"model\":\"embeddinggemma:300m\",\"embeddings\":" + vectores + "}") : normal(ruta, solicitud);
            codigo("IA_RESPUESTA_INVALIDA", () -> cliente.vectorizar(vectores.contains("[3]") ? List.of("Uno", "Dos") : List.of("Uno")));
        }
        responder = (ruta, solicitud) -> ruta.equals("/api/embed") ? ok("{\"model\":\"otro\",\"embeddings\":[[1]]}") : normal(ruta, solicitud);
        codigo("IA_RESPUESTA_INVALIDA", () -> cliente.vectorizar(List.of("Uno")));
    }

    @Test void estadoRechazaVersionYListadosInvalidos() {
        for (String version : List.of("{}", "{\"version\":\" \"}")) {
            responder = (ruta, solicitud) -> ruta.equals("/api/version") ? ok(version) : normal(ruta, solicitud);
            codigo("IA_RESPUESTA_INVALIDA", cliente::estado);
        }
        for (String tags : List.of("{}", "{\"models\":[{}]}")) {
            responder = (ruta, solicitud) -> ruta.equals("/api/tags") ? ok(tags) : normal(ruta, solicitud);
            codigo("IA_RESPUESTA_INVALIDA", cliente::estado);
        }
    }

    @Test void limitaRespuestaYLiberaElTurnoTrasUnFallo() {
        cliente = new ClienteOllama(configuracion(true, Duration.ofSeconds(3), 1024), http, json);
        responder = (ruta, solicitud) -> ok("x".repeat(2048));
        codigo("IA_RESPUESTA_INVALIDA", this::conversar);
        responder = this::normal;
        assertThat(conversar().texto()).isNotBlank();
    }

    @Test void aplicaTimeoutYRechazaGeneracionesSimultaneas() throws Exception {
        var entrada = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        responder = (ruta, solicitud) -> {
            if (ruta.equals("/api/chat")) {
                entrada.countDown();
                try { liberar.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            return normal(ruta, solicitud);
        };
        cliente = new ClienteOllama(configuracion(true, Duration.ofSeconds(1), 1048576), http, json);
        CompletableFuture<RespuestaIa> primera = CompletableFuture.supplyAsync(this::conversar);
        try {
            assertThat(entrada.await(3, TimeUnit.SECONDS)).isTrue();
            codigo("IA_OCUPADA", this::conversar);
            assertThatThrownBy(() -> primera.get(3, TimeUnit.SECONDS)).hasCauseInstanceOf(IaException.class);
        } finally { liberar.countDown(); }
        responder = this::normal;
        assertThat(conversar().texto()).isNotBlank();
    }

    @Test void preservaInterrupcionesYClasificaFallosDeConexion() {
        Thread.currentThread().interrupt();
        try {
            codigo("IA_NO_DISPONIBLE", this::conversar);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
        HttpClient simulado = mock(HttpClient.class);
        when(simulado.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(CompletableFuture.failedFuture(new HttpTimeoutException("contenido privado")));
        cliente = new ClienteOllama(configuracion(true, Duration.ofSeconds(1), 1024), simulado, json);
        codigo("IA_TIMEOUT", this::conversar);
        servidor.stop(0);
        cliente = new ClienteOllama(configuracion(true, Duration.ofSeconds(1), 1024), http, json);
        codigo("IA_NO_DISPONIBLE", this::conversar);
    }

    @Test void timeoutTambienLimitaUnCuerpoQueNoTerminaDespuesDeLasCabeceras() throws Exception {
        CountDownLatch cabeceras = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        servidor.createContext("/api/chat", intercambio -> {
            try (intercambio) {
                intercambio.getRequestBody().readAllBytes();
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, 0);
                cabeceras.countDown();
                liberar.await(4, TimeUnit.SECONDS);
                intercambio.getResponseBody().write("{}".getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) { }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        cliente = new ClienteOllama(configuracion(true, Duration.ofSeconds(1), 1024), http, json);
        CompletableFuture<RespuestaIa> respuesta = CompletableFuture.supplyAsync(this::conversar);
        try {
            assertThat(cabeceras.await(3, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> respuesta.get(3, TimeUnit.SECONDS)).satisfies(error ->
                    assertThat(error.getCause()).isInstanceOfSatisfying(IaException.class,
                            ia -> assertThat(ia.getCodigo()).isEqualTo("IA_TIMEOUT")));
        } finally { liberar.countDown(); }
    }
}
