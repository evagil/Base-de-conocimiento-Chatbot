package com.municipio.base_conocimiento.ia.dto;

import java.util.List;

public record EstadoIa(boolean habilitada, boolean disponible, String versionOllama, List<EstadoModelo> modelos) {
    public record EstadoModelo(String uso, String nombre, boolean instalado) { }
}
