package com.municipio.base_conocimiento.seguridad.dto;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record PasswordResetRequest(@NotBlank @Size(min=8,max=72) String password, boolean temporal){}
