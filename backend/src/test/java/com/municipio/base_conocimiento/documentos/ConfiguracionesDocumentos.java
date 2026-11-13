package com.municipio.base_conocimiento.documentos;

import com.municipio.base_conocimiento.documentos.config.ConfiguracionDocumentos;
import java.nio.file.Path;
import java.time.Duration;

public final class ConfiguracionesDocumentos {
    private ConfiguracionesDocumentos() { }
    public static ConfiguracionDocumentos crear(Path raiz, boolean habilitada, Duration timeout, int caracteres) {
        return new ConfiguracionDocumentos(habilitada, raiz, "tesseract", "", "spa+eng", timeout,
                10485760, 10, 3, caracteres, 6000000, 33554432, 120);
    }
}
