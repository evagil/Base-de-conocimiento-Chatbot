package com.municipio.base_conocimiento.ia.controller;

import com.municipio.base_conocimiento.ia.client.ClienteOllama;
import com.municipio.base_conocimiento.ia.dto.EstadoIa;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ia")
public class EstadoIaController {
    private final ClienteOllama cliente;
    public EstadoIaController(ClienteOllama cliente) { this.cliente = cliente; }
    @GetMapping("/estado") public EstadoIa estado() { return cliente.estado(); }
}
