package com.municipio.base_conocimiento.ia;

import static org.assertj.core.api.Assertions.*;
import com.municipio.base_conocimiento.ia.config.ConfiguracionIa;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConfiguracionIaTest {
    private ConfiguracionIa crear(URI url, String modelo, Duration timeout, Duration conexion, int[] limites) {
        return new ConfiguracionIa(true, url, modelo, "qwen3:4b", "qwen2.5vl:3b", "embeddinggemma:300m",
                timeout, conexion, limites[0], limites[1], limites[2], limites[3], limites[4]);
    }
    private int[] limites() { return new int[]{2048, 4000, 128, 8, 1048576}; }

    @Test void soloAceptaUrlsLocalesSinCredencialesNiRutas() {
        for (String url : List.of("http://localhost:11434", "http://127.0.0.1:1234/", "http://[::1]:11434")) {
            assertThat(crear(URI.create(url), "gemma3:1b", Duration.ofSeconds(120), Duration.ofSeconds(2), limites()).url().getPath()).isEqualTo("/");
        }
        for (URI url : Arrays.asList(null, URI.create("https://localhost"), URI.create("http://example.com"),
                URI.create("http:/sin-host"), URI.create("http://user:pass@localhost"), URI.create("http://localhost/api"),
                URI.create("http://localhost:99999"),
                URI.create("http://localhost?dato=1"), URI.create("http://localhost#fragmento"))) {
            assertThatThrownBy(() -> crear(url, "gemma3:1b", Duration.ofSeconds(120), Duration.ofSeconds(2), limites())).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void validaNombresModelosYLimites() {
        for (String nombre : Arrays.asList(null, "", " ", "modelo:cloud", "qwen:4b-cloud", "modelo?dato=1")) {
            assertThatThrownBy(() -> crear(URI.create("http://localhost"), nombre, Duration.ofSeconds(120), Duration.ofSeconds(2), limites())).isInstanceOf(IllegalArgumentException.class);
        }
        for (Duration timeout : Arrays.asList(null, Duration.ZERO, Duration.ofSeconds(-1), Duration.ofSeconds(181))) {
            assertThatThrownBy(() -> crear(URI.create("http://localhost"), "gemma3:1b", timeout, Duration.ofSeconds(2), limites())).isInstanceOf(IllegalArgumentException.class);
        }
        for (Duration conexion : Arrays.asList(null, Duration.ZERO, Duration.ofSeconds(-1), Duration.ofSeconds(11))) {
            assertThatThrownBy(() -> crear(URI.create("http://localhost"), "gemma3:1b", Duration.ofSeconds(120), conexion, limites())).isInstanceOf(IllegalArgumentException.class);
        }
        int[][] extremos = {{511, 4097}, {255, 16001}, {15, 513}, {0, 17}, {1023, 2097153}};
        for (int i = 0; i < extremos.length; i++) {
            for (int extremo : extremos[i]) {
                int[] limites = limites();
                limites[i] = extremo;
                assertThatThrownBy(() -> crear(URI.create("http://localhost"), "gemma3:1b", Duration.ofSeconds(120), Duration.ofSeconds(2), limites)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
