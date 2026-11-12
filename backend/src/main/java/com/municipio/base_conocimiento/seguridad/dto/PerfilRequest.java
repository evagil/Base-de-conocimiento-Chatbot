package com.municipio.base_conocimiento.seguridad.dto;
import jakarta.validation.constraints.*;
public record PerfilRequest(@NotBlank @Size(max=120) String nombre,@Email @Size(max=180) String email){}
