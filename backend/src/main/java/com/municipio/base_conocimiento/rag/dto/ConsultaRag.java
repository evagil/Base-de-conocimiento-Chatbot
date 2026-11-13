package com.municipio.base_conocimiento.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultaRag(@NotBlank @Size(max=2000) String pregunta) {
    @Override public String toString(){return "ConsultaRag[pregunta=omitida]";}
}
