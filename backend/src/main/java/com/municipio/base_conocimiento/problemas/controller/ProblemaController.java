package com.municipio.base_conocimiento.problemas.controller;

import com.municipio.base_conocimiento.areas.dto.EstadoActivoRequest;
import com.municipio.base_conocimiento.problemas.dto.*;
import com.municipio.base_conocimiento.problemas.entity.EstadoProblema;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/problemas")
public class ProblemaController {
    private final ProblemaService service;
    public ProblemaController(ProblemaService service) { this.service=service; }
    @GetMapping public Page<ProblemaResponse> listar(@RequestParam(required=false) Long subareaId,
            @RequestParam(required=false) Boolean activo,
            @PageableDefault(size=20,sort="modificadoEn",direction=Sort.Direction.DESC) Pageable pageable) { return service.listar(subareaId,activo,pageable); }
    @GetMapping("/{id}") public ProblemaDetalleResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<ProblemaResponse> crear(@Valid @RequestBody ProblemaRequest r) { var creado=service.crear(r); return ResponseEntity.created(URI.create("/api/problemas/"+creado.id())).body(creado); }
    @PutMapping("/{id}") public ProblemaResponse actualizar(@PathVariable Long id,@Valid @RequestBody ProblemaRequest r, @RequestParam long version) { return service.actualizar(id,r,version); }
    @PatchMapping("/{id}/estado") public ProblemaResponse estado(@PathVariable Long id,@RequestParam EstadoProblema estado, @RequestParam long version) { return service.cambiarEstado(id,estado,version); }
    @PatchMapping("/{id}/activo") public ProblemaResponse activo(@PathVariable Long id,@Valid @RequestBody EstadoActivoRequest r, @RequestParam long version) { return service.cambiarActivo(id,r.activo(),version); }
}
