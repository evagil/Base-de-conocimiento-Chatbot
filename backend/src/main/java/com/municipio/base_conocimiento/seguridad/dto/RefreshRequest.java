package com.municipio.base_conocimiento.seguridad.dto;
import jakarta.validation.constraints.NotBlank;
public record RefreshRequest(@NotBlank String refreshToken){}
