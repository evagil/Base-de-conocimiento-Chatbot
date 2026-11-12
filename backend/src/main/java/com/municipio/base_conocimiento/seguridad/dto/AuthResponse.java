package com.municipio.base_conocimiento.seguridad.dto;
public record AuthResponse(String accessToken,String refreshToken,long expiresIn,UsuarioResponse usuario){}
