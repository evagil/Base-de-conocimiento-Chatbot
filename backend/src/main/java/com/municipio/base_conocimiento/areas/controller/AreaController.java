package com.municipio.base_conocimiento.areas.controller;

import com.municipio.base_conocimiento.areas.dto.AreaRequest;
import com.municipio.base_conocimiento.areas.dto.AreaResponse;
import com.municipio.base_conocimiento.areas.dto.EstadoActivoRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaResponse;
import com.municipio.base_conocimiento.areas.service.AreaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas")
public class AreaController {
    private final AreaService service;

    public AreaController(AreaService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Lista áreas", description = "Permite filtrar por estado y pagina el resultado.")
    public Page<AreaResponse> listar(@RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = {"orden", "nombre"}) Pageable pageable) {
        return service.listar(activo, pageable);
    }

    @GetMapping("/{id}")
    public AreaResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    public ResponseEntity<AreaResponse> crear(@Valid @RequestBody AreaRequest request) {
        AreaResponse creada = service.crear(request);
        return ResponseEntity.created(URI.create("/api/areas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public AreaResponse actualizar(@PathVariable Long id, @Valid @RequestBody AreaRequest request, @RequestParam long version) {
        return service.actualizar(id, request,version);
    }

    @PatchMapping("/{id}/estado")
    public AreaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoActivoRequest request, @RequestParam long version) {
        return service.cambiarEstado(id, request.activo(),version);
    }

    @GetMapping("/{id}/subareas")
    public List<SubareaResponse> listarSubareas(@PathVariable Long id,
            @RequestParam(required = false) Boolean activo) {
        return service.listarSubareas(id, activo);
    }

    @PostMapping("/{id}/subareas")
    public ResponseEntity<SubareaResponse> crearSubarea(@PathVariable Long id,
            @Valid @RequestBody SubareaRequest request) {
        SubareaResponse creada = service.crearSubarea(id, request);
        return ResponseEntity.created(URI.create("/api/subareas/" + creada.id())).body(creada);
    }
}
