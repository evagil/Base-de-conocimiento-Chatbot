package com.municipio.base_conocimiento.soluciones.dto;

import jakarta.validation.constraints.*;

public record SolucionRequest(@Size(max = 200) String titulo,
        @NotBlank(message = "La descripción es obligatoria.") String descripcionHtml,
        boolean principal, @PositiveOrZero int orden, @Size(max = 300) String motivoCambio) {}
