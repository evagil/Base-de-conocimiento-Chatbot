package com.municipio.base_conocimiento.rag.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("app.indice")
public record ConfiguracionIndice(boolean habilitado, boolean embeddings, int tamanoFragmento,
        int solapamiento, int maxFragmentos, int maxFuentes, int maxAdjuntos, Duration timeout) {
    public ConfiguracionIndice {
        if(tamanoFragmento<200 || tamanoFragmento>1200 || solapamiento<0 || solapamiento>=tamanoFragmento/2
                || maxFragmentos<1 || maxFragmentos>256 || maxFuentes<1 || maxFuentes>200
                || maxAdjuntos<0 || maxAdjuntos>5 || timeout==null || timeout.isNegative() || timeout.isZero()
                || timeout.compareTo(Duration.ofMinutes(2))>0) throw new IllegalArgumentException("Configuración de indexación inválida.");
    }
    @Configuration @EnableConfigurationProperties(ConfiguracionIndice.class)
    public static class Registro { }
}
