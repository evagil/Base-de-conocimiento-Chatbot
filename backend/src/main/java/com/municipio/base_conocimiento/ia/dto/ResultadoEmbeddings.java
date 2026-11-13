package com.municipio.base_conocimiento.ia.dto;

import java.util.List;

public record ResultadoEmbeddings(String modelo, List<List<Double>> vectores) {
    public ResultadoEmbeddings {
        vectores = vectores.stream().map(List::copyOf).toList();
    }
}
