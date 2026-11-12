package com.municipio.base_conocimiento.seguridad.controller;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController{
 private final AuthService service;public AuthController(AuthService service){this.service=service;}
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r){return service.login(r);}
 @PostMapping("/refresh") public AuthResponse refresh(@Valid @RequestBody RefreshRequest r){return service.refresh(r);}
 @PostMapping("/logout") public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest r){service.logout(r);return ResponseEntity.noContent().build();}
 @PutMapping("/perfil") public UsuarioResponse actualizarPerfil(Principal principal,@Valid @RequestBody PerfilRequest r){return service.actualizarPerfil(principal.getName(),r);}
 @PostMapping("/password") public ResponseEntity<Void> cambiarPassword(Principal principal,@Valid @RequestBody PasswordChangeRequest r){service.cambiarPassword(principal.getName(),r);return ResponseEntity.noContent().build();}
 @GetMapping("/me") public UsuarioResponse me(Principal principal){return service.actual(principal.getName());}
}
