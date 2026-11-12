package com.municipio.base_conocimiento.seguridad.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Size(min=3,max=60) @Pattern(regexp="[A-Za-z0-9._-]+") String usuario,@NotBlank String password){}
