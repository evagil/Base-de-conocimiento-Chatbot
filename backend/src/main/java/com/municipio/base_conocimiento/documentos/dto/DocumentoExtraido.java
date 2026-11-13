package com.municipio.base_conocimiento.documentos.dto;

import java.util.List;

public record DocumentoExtraido(String nombre, String tipoMime, Integer paginas,
        List<Seccion> secciones, int caracteres, boolean ocrUsado) {
    public record Seccion(Integer pagina, String texto, String metodo) { }
}
