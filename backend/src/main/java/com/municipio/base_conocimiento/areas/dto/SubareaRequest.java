package com.municipio.base_conocimiento.areas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubareaRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
        String nombre,
        @Size(max = 500, message = "La descripción admite hasta 500 caracteres.")
        String descripcion,
        @PositiveOrZero(message = "El orden no puede ser negativo.")
        int orden
) {}
