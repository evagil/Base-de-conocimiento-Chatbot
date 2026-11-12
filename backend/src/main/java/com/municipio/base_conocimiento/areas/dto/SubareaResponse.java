package com.municipio.base_conocimiento.areas.dto;

import java.time.Instant;

public record SubareaResponse(Long id, Long areaId, String areaNombre, String nombre, String descripcion,
                              int orden, boolean activo, Instant creadoEn, Instant modificadoEn, long version) {}
