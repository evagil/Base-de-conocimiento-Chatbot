package com.municipio.base_conocimiento.soluciones.dto;

public record PasoResponse(Long id, String titulo, String contenidoHtml, int orden, boolean activo, long version) {}
