package com.municipio.base_conocimiento.ia.dto;

/** Solo respuesta final y métricas; nunca se expone el campo thinking del modelo. */
public record RespuestaIa(String modelo, String texto, long duracionTotalNs, int tokensGenerados) { }
