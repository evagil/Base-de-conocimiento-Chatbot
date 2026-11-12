package com.municipio.base_conocimiento.seguridad.dto;
import com.municipio.base_conocimiento.seguridad.entity.*;
public record UsuarioResponse(Long id,String nombre,String usuario,String email,Rol rol,boolean activo,boolean passwordTemporal,long version){public static UsuarioResponse de(Usuario u){return new UsuarioResponse(u.getId(),u.getNombre(),u.getUsuario(),u.getEmail(),u.getRol(),u.isActivo(),u.isPasswordTemporal(),u.getVersion());}}
