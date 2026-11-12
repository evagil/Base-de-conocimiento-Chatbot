package com.municipio.base_conocimiento.seguridad.service;
import com.municipio.base_conocimiento.compartido.exception.ConflictoException;
import com.municipio.base_conocimiento.compartido.exception.RecursoNoEncontradoException;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.entity.Usuario;
import com.municipio.base_conocimiento.seguridad.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class UsuarioService{
 private final UsuarioRepository repo;private final PasswordEncoder encoder;
 public UsuarioService(UsuarioRepository repo,PasswordEncoder encoder){this.repo=repo;this.encoder=encoder;}
 public List<UsuarioResponse> listar(){return repo.findAll().stream().map(UsuarioResponse::de).toList();}
 @Transactional public UsuarioResponse crear(UsuarioRequest r){String email=AuthService.normalizarOpcional(r.email());String usuario=AuthService.normalizarUsuario(r.usuario());validarUsuarioDisponible(usuario,null);validarEmailDisponible(email,null);return UsuarioResponse.de(repo.save(new Usuario(r.nombre(),r.usuario(),usuario,r.email(),email,encoder.encode(r.password()),r.rol(),Boolean.TRUE.equals(r.passwordTemporal()))));}
 @Transactional public UsuarioResponse actualizar(Long id,UsuarioUpdateRequest r, long versionEsperada){Usuario u=repo.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe el usuario "+id+"."));u.verificarVersion(versionEsperada);String usuario=AuthService.normalizarUsuario(r.usuario());String email=AuthService.normalizarOpcional(r.email());validarUsuarioDisponible(usuario,id);validarEmailDisponible(email,id);u.actualizar(r.nombre(),r.usuario(),usuario,r.email(),email,r.rol(),r.activo(),Boolean.TRUE.equals(r.passwordTemporal()));repo.flush(); return UsuarioResponse.de(u);}
 @Transactional public UsuarioResponse resetearPassword(Long id,PasswordResetRequest r){Usuario u=repo.findById(id).orElseThrow(()->new RecursoNoEncontradoException("No existe el usuario "+id+"."));u.resetearPassword(encoder.encode(r.password()),r.temporal());repo.flush();return UsuarioResponse.de(u);}
 private void validarUsuarioDisponible(String usuario,Long idActual){repo.findByUsuarioNormalizado(usuario).filter(existente->idActual==null||!existente.getId().equals(idActual)).ifPresent(existente->{throw new ConflictoException("USUARIO_DUPLICADO","Ya existe un usuario con ese nombre de usuario.");});}
 private void validarEmailDisponible(String email,Long idActual){if(email==null)return;repo.findByEmailNormalizado(email).filter(existente->idActual==null||!existente.getId().equals(idActual)).ifPresent(existente->{throw new ConflictoException("EMAIL_DUPLICADO","Ya existe un usuario con ese correo.");});}
}
