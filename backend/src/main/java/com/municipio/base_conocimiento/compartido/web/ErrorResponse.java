package com.municipio.base_conocimiento.compartido.web;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String codigo,
        String mensaje,
        Map<String, String> campos,
        Instant fecha
) {}
