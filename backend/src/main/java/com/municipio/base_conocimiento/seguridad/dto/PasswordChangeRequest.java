package com.municipio.base_conocimiento.seguridad.dto;
import jakarta.validation.constraints.*;
public record PasswordChangeRequest(@NotBlank String passwordActual,@NotBlank @Size(min=8,max=72) String passwordNueva){}
