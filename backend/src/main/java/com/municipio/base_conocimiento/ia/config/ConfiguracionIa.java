package com.municipio.base_conocimiento.ia.config;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ia")
public record ConfiguracionIa(boolean habilitada, URI url, String modeloConversacion,
        String modeloAgente, String modeloMultimodal, String modeloEmbeddings,
        Duration timeout, Duration conexionTimeout, int contexto, int maxPromptChars,
        int maxOutputTokens, int maxBatch, int maxResponseBytes) {
    public ConfiguracionIa {
        if (url == null || !"http".equals(url.getScheme()) || url.getHost() == null || url.getPort() > 65535
                || !Set.of("localhost", "127.0.0.1", "::1", "[::1]").contains(url.getHost())
                || url.getUserInfo() != null || url.getQuery() != null || url.getFragment() != null
                || !(url.getPath().isEmpty() || "/".equals(url.getPath()))) {
            throw new IllegalArgumentException("OLLAMA_BASE_URL debe ser una URL HTTP local sin credenciales ni rutas adicionales.");
        }
        url = url.resolve("/");
        for (String modelo : new String[]{modeloConversacion, modeloAgente, modeloMultimodal, modeloEmbeddings}) {
            if (modelo == null || !modelo.matches("[a-zA-Z0-9][a-zA-Z0-9._/-]*(?::[a-zA-Z0-9._-]+)?")
                    || modelo.toLowerCase(java.util.Locale.ROOT).contains("cloud")) {
                throw new IllegalArgumentException("Los modelos de IA deben ser locales y tener un nombre válido.");
            }
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofSeconds(180)) > 0
                || conexionTimeout == null || conexionTimeout.isNegative() || conexionTimeout.isZero()
                || conexionTimeout.compareTo(Duration.ofSeconds(10)) > 0
                || contexto < 512 || contexto > 4096 || maxPromptChars < 256 || maxPromptChars > 16000
                || maxOutputTokens < 16 || maxOutputTokens > 512 || maxBatch < 1 || maxBatch > 16
                || maxResponseBytes < 1024 || maxResponseBytes > 2097152) {
            throw new IllegalArgumentException("Los límites de IA están fuera de los rangos permitidos.");
        }
    }
}
