package com.municipio.base_conocimiento.seguridad.service;

import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.entity.Usuario;
import com.municipio.base_conocimiento.seguridad.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class AuthService {
    private final UsuarioRepository usuarios;private final PasswordEncoder passwords;private final TokenService tokens;
    public AuthService(UsuarioRepository usuarios,PasswordEncoder passwords,TokenService tokens){this.usuarios=usuarios;this.passwords=passwords;this.tokens=tokens;}
    @Transactional public AuthResponse login(LoginRequest request){Usuario u=usuarios.findByUsuarioNormalizado(normalizarUsuario(request.usuario())).orElseThrow(this::credenciales);if(!u.isActivo()||!passwords.matches(request.password(),u.getPasswordHash()))throw credenciales();return respuesta(u);}
    @Transactional public AuthResponse refresh(RefreshRequest request){try{return respuesta(tokens.consumir(request.refreshToken()));}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,e.getMessage());}}
    @Transactional public void logout(RefreshRequest request){tokens.revocar(request.refreshToken());}
    @Transactional public void cambiarPassword(String usuario,PasswordChangeRequest request){Usuario u=usuarios.findByUsuarioNormalizado(normalizarUsuario(usuario)).orElseThrow(this::credenciales);if(!passwords.matches(request.passwordActual(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"La contraseña actual no es correcta.");u.cambiarPassword(passwords.encode(request.passwordNueva()));usuarios.flush();}
    @Transactional public UsuarioResponse actualizarPerfil(String usuario,PerfilRequest request){Usuario u=usuarios.findByUsuarioNormalizado(normalizarUsuario(usuario)).orElseThrow(this::credenciales);String email=normalizarOpcional(request.email());if(email!=null)usuarios.findByEmailNormalizado(email).filter(existente->!existente.getId().equals(u.getId())).ifPresent(existente->{throw new ConflictoException("EMAIL_DUPLICADO","Ya existe un usuario con ese correo.");});u.actualizarPerfil(request.nombre(),request.email(),email);usuarios.flush();return UsuarioResponse.de(u);}
    public UsuarioResponse actual(String usuario){return UsuarioResponse.de(usuarios.findByUsuarioNormalizado(normalizarUsuario(usuario)).orElseThrow(this::credenciales));}
    private AuthResponse respuesta(Usuario u){return new AuthResponse(tokens.accessToken(u),tokens.crearRefresh(u),tokens.getAccessExpiration(),UsuarioResponse.de(u));}
    private ResponseStatusException credenciales(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas.");}
    public static String normalizarOpcional(String email){return email==null||email.isBlank()?null:email.strip().toLowerCase(Locale.ROOT);}
    public static String normalizar(String email){return email.strip().toLowerCase(Locale.ROOT);}
    public static String normalizarUsuario(String usuario){return usuario.strip().toLowerCase(Locale.ROOT);}
}
