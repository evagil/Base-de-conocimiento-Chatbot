package com.municipio.base_conocimiento.soluciones.dto;

import java.time.Instant;
import java.util.List;

public record SolucionResponse(Long id, Long problemaId, String titulo, String descripcionHtml,
        boolean principal, int orden, boolean activo, List<PasoResponse> pasos,
        Instant creadoEn, Instant modificadoEn, long version) {}
