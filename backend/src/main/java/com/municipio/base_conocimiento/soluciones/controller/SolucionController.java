package com.municipio.base_conocimiento.soluciones.controller;

import com.municipio.base_conocimiento.areas.dto.EstadoActivoRequest;
import com.municipio.base_conocimiento.soluciones.dto.*;
import com.municipio.base_conocimiento.soluciones.service.SolucionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SolucionController {
    private final SolucionService service;
    public SolucionController(SolucionService service) { this.service=service; }
    @GetMapping("/api/problemas/{problemaId}/soluciones") public List<SolucionResponse> listar(@PathVariable Long problemaId){return service.listar(problemaId);}
    @PostMapping("/api/problemas/{problemaId}/soluciones") public ResponseEntity<SolucionResponse> crear(@PathVariable Long problemaId,@Valid @RequestBody SolucionRequest r){var s=service.crear(problemaId,r);return ResponseEntity.created(URI.create("/api/soluciones/"+s.id())).body(s);}
    @GetMapping("/api/soluciones/{id}") public SolucionResponse obtener(@PathVariable Long id){return service.obtener(id);}
    @PutMapping("/api/soluciones/{id}") public SolucionResponse actualizar(@PathVariable Long id,@Valid @RequestBody SolucionRequest r, @RequestParam long version){return service.actualizar(id,r,version);}
    @PatchMapping("/api/soluciones/{id}/principal") public SolucionResponse principal(@PathVariable Long id, @RequestParam long version){return service.marcarPrincipal(id,version);}
    @PatchMapping("/api/soluciones/{id}/activo") public SolucionResponse activo(@PathVariable Long id,@Valid @RequestBody EstadoActivoRequest r, @RequestParam long version){return service.cambiarActivo(id,r.activo(),version);}
    @PostMapping("/api/soluciones/{id}/pasos") public ResponseEntity<PasoResponse> crearPaso(@PathVariable Long id,@Valid @RequestBody PasoRequest r){var p=service.crearPaso(id,r);return ResponseEntity.created(URI.create("/api/pasos/"+p.id())).body(p);}
    @PutMapping("/api/pasos/{id}") public PasoResponse actualizarPaso(@PathVariable Long id,@Valid @RequestBody PasoRequest r, @RequestParam long version){return service.actualizarPaso(id,r,version);}
    @PatchMapping("/api/pasos/{id}/activo") public PasoResponse activoPaso(@PathVariable Long id,@Valid @RequestBody EstadoActivoRequest r, @RequestParam long version){return service.cambiarActivoPaso(id,r.activo(),version);}
}
