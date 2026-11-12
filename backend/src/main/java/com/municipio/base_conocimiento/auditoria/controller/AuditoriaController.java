package com.municipio.base_conocimiento.auditoria.controller;
import com.municipio.base_conocimiento.auditoria.dto.EventoAuditoriaResponse;import com.municipio.base_conocimiento.auditoria.service.AuditoriaService;import java.util.List;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auditoria") public class AuditoriaController{private final AuditoriaService service;public AuditoriaController(AuditoriaService service){this.service=service;}@GetMapping public List<EventoAuditoriaResponse> listar(){return service.listar();}}
