package com.municipio.base_conocimiento.ia.client;

import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import com.municipio.base_conocimiento.ia.dto.*;
import com.municipio.base_conocimiento.ia.exception.IaException;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;

@Component
public class ClienteOllama implements ClienteConversacion, ClienteEmbeddings {
    private final ConfiguracionIa configuracion;
    private final HttpClient http;
    private final ObjectMapper json;
    private final Duration permanencia;
    private final Semaphore operacion = new Semaphore(1);

    public ClienteOllama(ConfiguracionIa configuracion, @Qualifier("ollamaHttpClient") HttpClient http, ObjectMapper json) {
        this(configuracion,http,json,Duration.ZERO);
    }

    @Autowired
    public ClienteOllama(ConfiguracionIa configuracion, @Qualifier("ollamaHttpClient") HttpClient http, ObjectMapper json,
            @Value("${app.ia.permanencia-modelo:120s}") Duration permanencia) {
        if(permanencia==null || permanencia.isNegative() || permanencia.compareTo(Duration.ofMinutes(5))>0
                || permanencia.getNano()!=0)throw new IllegalArgumentException("La permanencia del modelo debe estar entre 0 y 300 segundos enteros.");
        this.configuracion = configuracion;
        this.http = http;
        this.json = json;
        this.permanencia=permanencia;
    }

    @Override public RespuestaIa conversar(Uso uso, List<MensajeIa> mensajes, Map<String, Object> esquema) {
        return ejecutar(() -> {
            if (uso == null || mensajes == null || mensajes.isEmpty() || mensajes.size() > 20) throw entradaInvalida();
            long longitud = 0;
            if (esquema != null) {
                if (!"object".equals(esquema.get("type"))) throw entradaInvalida();
                longitud += json.writeValueAsString(esquema).length();
            }
            List<Map<String, String>> historial = new ArrayList<>();
            for (MensajeIa mensaje : mensajes) {
                if (mensaje == null || mensaje.rol() == null || !Set.of("system", "user", "assistant").contains(mensaje.rol())
                        || mensaje.contenido() == null || mensaje.contenido().isBlank()) throw entradaInvalida();
                longitud += mensaje.contenido().length();
                historial.add(Map.of("role", mensaje.rol(), "content", mensaje.contenido()));
            }
            if (longitud > configuracion.maxPromptChars()) throw entradaInvalida();
            String modelo = uso == Uso.CHATBOT ? configuracion.modeloConversacion() : configuracion.modeloAgente();
            long limite = limite();
            verificarModelo(modelo, "completion", limite);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelo);
            body.put("messages", historial);
            if (esquema != null) body.put("format", esquema);
            body.put("stream", false);
            body.put("think", false);
            body.put("keep_alive", permanencia.isZero()?0:permanencia.toSeconds()+"s");
            body.put("options", Map.of("num_ctx", configuracion.contexto(), "num_predict", configuracion.maxOutputTokens(),
                    "num_thread", 2, "temperature", 0, "seed", 42));
            JsonNode respuesta = peticion("/api/chat", body, limite);
            if ("length".equals(respuesta.path("done_reason").asText())) {
                throw new IaException(HttpStatus.BAD_GATEWAY, "IA_RESPUESTA_INCOMPLETA",
                        "El modelo alcanzó el límite de respuesta. Acotá la consulta o revisá la configuración.");
            }
            JsonNode texto = respuesta.path("message").path("content");
            if (!respuesta.path("done").asBoolean(false) || !texto.isTextual() || texto.asText().isBlank()
                    || !"assistant".equals(respuesta.path("message").path("role").asText())
                    || !modelo.equals(respuesta.path("model").asText())
                    || respuesta.path("message").path("tool_calls").size() > 0) throw respuestaInvalida();
            if (esquema != null && !esObjetoJson(texto.asText())) throw respuestaInvalida();
            return new RespuestaIa(modelo, texto.asText(), respuesta.path("total_duration").asLong(0),
                    respuesta.path("eval_count").asInt(0));
        });
    }

    @Override public ResultadoEmbeddings vectorizar(List<String> textos) {
        return ejecutar(() -> {
            if (textos == null || textos.isEmpty() || textos.size() > configuracion.maxBatch()) throw entradaInvalida();
            for (String texto : textos) {
                if (texto == null || texto.isBlank() || texto.length() > configuracion.maxPromptChars()) throw entradaInvalida();
            }
            String modelo = configuracion.modeloEmbeddings();
            long limite = limite();
            verificarModelo(modelo, "embedding", limite);
            JsonNode respuesta = peticion("/api/embed", Map.of("model", modelo, "input", List.copyOf(textos),
                    "truncate", false, "keep_alive", "0"), limite);
            JsonNode embeddings = respuesta.path("embeddings");
            if (!modelo.equals(respuesta.path("model").asText()) || !embeddings.isArray() || embeddings.size() != textos.size()) throw respuestaInvalida();
            List<List<Double>> vectores = new ArrayList<>();
            int dimension = -1;
            for (JsonNode vector : embeddings) {
                if (!vector.isArray() || vector.isEmpty() || vector.size() > 8192
                        || (dimension != -1 && dimension != vector.size())) throw respuestaInvalida();
                dimension = vector.size();
                List<Double> valores = new ArrayList<>();
                boolean noNulo = false;
                for (JsonNode valor : vector) {
                    double numero = valor.asDouble(Double.NaN);
                    if (!valor.isNumber() || !Double.isFinite(numero)) throw respuestaInvalida();
                    valores.add(numero);
                    noNulo |= numero != 0;
                }
                if (!noNulo) throw respuestaInvalida();
                vectores.add(valores);
            }
            return new ResultadoEmbeddings(modelo, vectores);
        });
    }

    public EstadoIa estado() {
        if (!configuracion.habilitada()) return new EstadoIa(false, false, null, modelos(Set.of()));
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        String version = peticion("/api/version", null, limite).path("version").asText("");
        JsonNode modelos = peticion("/api/tags", null, limite).path("models");
        if (version.isBlank() || !modelos.isArray()) throw respuestaInvalida();
        Set<String> instalados = new HashSet<>();
        for (JsonNode modelo : modelos) {
            String nombre = modelo.path("name").asText("");
            if (nombre.isBlank()) throw respuestaInvalida();
            instalados.add(nombre);
        }
        return new EstadoIa(true, true, version, modelos(instalados));
    }

    private List<EstadoIa.EstadoModelo> modelos(Set<String> instalados) {
        return List.of(new EstadoIa.EstadoModelo("CHATBOT", configuracion.modeloConversacion(), instalados.contains(configuracion.modeloConversacion())),
                new EstadoIa.EstadoModelo("AGENTE", configuracion.modeloAgente(), instalados.contains(configuracion.modeloAgente())),
                new EstadoIa.EstadoModelo("VISION_OPCIONAL", configuracion.modeloMultimodal(), instalados.contains(configuracion.modeloMultimodal())),
                new EstadoIa.EstadoModelo("EMBEDDINGS", configuracion.modeloEmbeddings(), instalados.contains(configuracion.modeloEmbeddings())));
    }

    private JsonNode verificarModelo(String modelo, String capacidad, long limite) {
        JsonNode detalle = peticion("/api/show", Map.of("model", modelo), limite);
        if (!detalle.path("remote_host").asText("").isBlank() || !detalle.path("remote_model").asText("").isBlank()) {
            throw new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_MODELO_REMOTO", "Solo se permiten modelos ejecutados localmente.");
        }
        if (!contiene(detalle.path("capabilities"), capacidad)) {
            throw new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_MODELO_INCOMPATIBLE", "El modelo configurado no admite esta operación.");
        }
        if ("completion".equals(capacidad)
                && "Thinking".equalsIgnoreCase(detalle.path("model_info").path("general.finetune").asText(""))) {
            throw new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_MODELO_INCOMPATIBLE",
                    "El modelo configurado es una variante de razonamiento obligatorio. Elegí un modelo de respuesta final.");
        }
        return detalle;
    }

    private boolean contiene(JsonNode valores, String buscado) {
        for (JsonNode valor : valores) if (buscado.equals(valor.asText())) return true;
        return false;
    }

    private boolean esObjetoJson(String texto) {
        try {
            JsonNode objeto = json.readTree(texto);
            return objeto != null && objeto.isObject();
        } catch (JacksonException e) { return false; }
    }

    private <T> T ejecutar(Supplier<T> tarea) {
        if (!configuracion.habilitada()) {
            throw new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_DESHABILITADA", "La IA local está deshabilitada.");
        }
        if (!operacion.tryAcquire()) {
            throw new IaException(HttpStatus.TOO_MANY_REQUESTS, "IA_OCUPADA", "La IA está procesando otra consulta. Intentá nuevamente en unos instantes.");
        }
        try { return tarea.get(); } finally { operacion.release(); }
    }

    private long limite() { return System.nanoTime() + configuracion.timeout().toNanos(); }

    private JsonNode peticion(String ruta, Object body, long limite) {
        long restante = limite - System.nanoTime();
        if (restante <= 0) throw timeout();
        HttpRequest.Builder builder = HttpRequest.newBuilder(configuracion.url().resolve(ruta))
                .timeout(Duration.ofNanos(restante)).header("Accept", "application/json");
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
        CompletableFuture<HttpResponse<byte[]>> pendiente = http.sendAsync(builder.build(), info -> new RespuestaLimitada(configuracion.maxResponseBytes()));
        try {
            HttpResponse<byte[]> respuesta = pendiente.get(Math.max(1, limite - System.nanoTime()), TimeUnit.NANOSECONDS);
            int status = respuesta.statusCode();
            if (status == 404 && body != null) throw new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_MODELO_NO_INSTALADO", "El modelo configurado no está instalado en Ollama.");
            if (status >= 500 || status == 429) throw noDisponible();
            if (status != 200 || !respuesta.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT).startsWith("application/json")) throw respuestaInvalida();
            JsonNode resultado = json.readTree(respuesta.body());
            if (resultado == null || !resultado.isObject() || resultado.has("error")) throw respuestaInvalida();
            return resultado;
        } catch (TimeoutException e) {
            pendiente.cancel(true);
            throw timeout();
        } catch (InterruptedException e) {
            pendiente.cancel(true);
            Thread.currentThread().interrupt();
            throw noDisponible();
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            while (causa != null) {
                if (causa instanceof HttpTimeoutException) throw timeout();
                if (causa instanceof RespuestaLimitada.LimiteExcedido) throw respuestaInvalida();
                causa = causa.getCause();
            }
            throw noDisponible();
        } catch (JacksonException e) {
            throw respuestaInvalida();
        }
    }

    private IaException entradaInvalida() { return new IaException(HttpStatus.BAD_REQUEST, "IA_ENTRADA_INVALIDA", "La consulta está vacía, tiene un formato inválido o supera los límites configurados."); }
    private IaException respuestaInvalida() { return new IaException(HttpStatus.BAD_GATEWAY, "IA_RESPUESTA_INVALIDA", "Ollama devolvió una respuesta inválida o demasiado grande."); }
    private IaException timeout() { return new IaException(HttpStatus.GATEWAY_TIMEOUT, "IA_TIMEOUT", "La IA local tardó demasiado en responder. Intentá una consulta más breve."); }
    private IaException noDisponible() { return new IaException(HttpStatus.SERVICE_UNAVAILABLE, "IA_NO_DISPONIBLE", "No se pudo comunicar con Ollama. Comprobá que la aplicación esté abierta."); }
}
