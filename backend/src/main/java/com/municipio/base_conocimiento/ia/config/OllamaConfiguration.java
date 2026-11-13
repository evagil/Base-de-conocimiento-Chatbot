package com.municipio.base_conocimiento.ia.config;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration
@EnableConfigurationProperties(ConfiguracionIa.class)
public class OllamaConfiguration {
    @Bean(destroyMethod = "close")
    public HttpClient ollamaHttpClient(ConfiguracionIa configuracion) {
        return HttpClient.newBuilder().connectTimeout(configuracion.conexionTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1)
                .proxy(new ProxySelector() {
                    @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
                    @Override public void connectFailed(URI uri, SocketAddress address, IOException exception) { }
                }).build();
    }
}
