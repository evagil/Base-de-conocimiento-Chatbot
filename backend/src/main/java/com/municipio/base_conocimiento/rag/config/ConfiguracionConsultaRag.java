package com.municipio.base_conocimiento.rag.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("app.rag")
public record ConfiguracionConsultaRag(boolean habilitado, boolean semantica, int maxPregunta,
        int maxCandidatos, int maxResultados, int maxContexto, double similitudMinima, Duration timeout) {
    public ConfiguracionConsultaRag {
        if(maxPregunta<20 || maxPregunta>2000 || maxCandidatos<10 || maxCandidatos>2000
                || maxResultados<1 || maxResultados>5 || maxContexto<300 || maxContexto>4000
                || !Double.isFinite(similitudMinima) || similitudMinima<0.3 || similitudMinima>1
                || timeout==null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(5))>0)
            throw new IllegalArgumentException("Configuración de consulta RAG inválida.");
    }
    @Configuration @EnableConfigurationProperties(ConfiguracionConsultaRag.class)
    public static class Registro { }
}
