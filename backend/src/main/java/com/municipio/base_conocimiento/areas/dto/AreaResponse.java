package com.municipio.base_conocimiento.areas.dto;

import java.time.Instant;

public record AreaResponse(Long id, String nombre, String descripcion, int orden, boolean activo,
                           Instant creadoEn, Instant modificadoEn, long version) {}
