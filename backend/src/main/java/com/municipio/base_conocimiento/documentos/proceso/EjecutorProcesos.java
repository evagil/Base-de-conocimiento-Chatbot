package com.municipio.base_conocimiento.documentos.proceso;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface EjecutorProcesos {
    int ejecutar(List<String> comando, Path directorio, Duration timeout);
}
