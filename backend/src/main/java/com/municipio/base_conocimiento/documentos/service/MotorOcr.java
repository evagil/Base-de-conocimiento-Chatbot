package com.municipio.base_conocimiento.documentos.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

public interface MotorOcr {
    String reconocer(Path imagen, Path carpeta, Duration timeout) throws IOException;
}
