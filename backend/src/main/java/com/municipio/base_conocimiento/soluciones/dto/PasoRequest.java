package com.municipio.base_conocimiento.soluciones.dto;

import jakarta.validation.constraints.*;

public record PasoRequest(@Size(max = 200) String titulo,
        @NotBlank(message = "El contenido es obligatorio.") String contenidoHtml,
        @Positive(message = "El orden comienza en 1.") int orden) {}
