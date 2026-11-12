package com.municipio.base_conocimiento.areas.controller;

import com.municipio.base_conocimiento.areas.dto.EstadoActivoRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaRequest;
import com.municipio.base_conocimiento.areas.dto.SubareaResponse;
import com.municipio.base_conocimiento.areas.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/subareas")
public class SubareaController {
    private final AreaService service;

    public SubareaController(AreaService service) { this.service = service; }

    @GetMapping("/{id}")
    public SubareaResponse obtener(@PathVariable Long id) { return service.obtenerSubarea(id); }

    @PutMapping("/{id}")
    public SubareaResponse actualizar(@PathVariable Long id, @Valid @RequestBody SubareaRequest request, @RequestParam long version) {
        return service.actualizarSubarea(id, request,version);
    }

    @PatchMapping("/{id}/estado")
    public SubareaResponse cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody EstadoActivoRequest request, @RequestParam long version) {
        return service.cambiarEstadoSubarea(id, request.activo(),version);
    }
}
