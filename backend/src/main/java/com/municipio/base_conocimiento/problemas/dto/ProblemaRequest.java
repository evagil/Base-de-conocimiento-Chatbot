package com.municipio.base_conocimiento.problemas.dto;

import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import jakarta.validation.constraints.*;
import java.util.Set;

public record ProblemaRequest(
        @NotNull(message = "La subárea es obligatoria.") Long subareaId,
        @NotBlank(message = "El título es obligatorio.") @Size(max = 200) String titulo,
        @NotBlank(message = "La descripción es obligatoria.") String descripcionHtml,
        @NotNull(message = "El estado es obligatorio.") EstadoProblema estado,
        Set<@NotBlank @Size(max = 60) String> etiquetas,
        @Size(max = 300) String motivoCambio
) {}
