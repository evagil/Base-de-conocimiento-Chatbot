package com.municipio.base_conocimiento.seguridad.dto;
import com.municipio.base_conocimiento.seguridad.entity.Rol;
import jakarta.validation.constraints.*;
public record UsuarioUpdateRequest(@NotBlank @Size(max=120) String nombre,@NotBlank @Size(min=3,max=60) @Pattern(regexp="[A-Za-z0-9._-]+") String usuario,@Email @Size(max=180) String email,@NotNull Rol rol,boolean activo,Boolean passwordTemporal){}
