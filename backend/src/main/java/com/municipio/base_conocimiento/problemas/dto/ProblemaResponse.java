package com.municipio.base_conocimiento.problemas.dto;

import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import java.time.Instant;
import java.util.Set;

public record ProblemaResponse(Long id, Long subareaId, String subarea, Long areaId, String area,
        String titulo, String descripcionHtml, EstadoProblema estado, boolean activo, Set<String> etiquetas,
        Instant creadoEn, Instant modificadoEn, long version) {}
