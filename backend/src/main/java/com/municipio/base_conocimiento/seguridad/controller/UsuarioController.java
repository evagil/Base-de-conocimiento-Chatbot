package com.municipio.base_conocimiento.seguridad.controller;
import com.municipio.base_conocimiento.seguridad.dto.*;
import com.municipio.base_conocimiento.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;import java.util.List;
import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/usuarios")
public class UsuarioController{
 private final UsuarioService service;public UsuarioController(UsuarioService service){this.service=service;}
 @GetMapping public List<UsuarioResponse> listar(){return service.listar();}
 @PostMapping public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest r){var u=service.crear(r);return ResponseEntity.created(URI.create("/api/usuarios/"+u.id())).body(u);}
 @PutMapping("/{id}") public UsuarioResponse actualizar(@PathVariable Long id,@Valid @RequestBody UsuarioUpdateRequest r, @RequestParam long version){return service.actualizar(id,r,version);}
 @PostMapping("/{id}/password") public UsuarioResponse resetearPassword(@PathVariable Long id,@Valid @RequestBody PasswordResetRequest r){return service.resetearPassword(id,r);}
}
