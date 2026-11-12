package com.municipio.base_conocimiento.problemas.controller;
import com.municipio.base_conocimiento.problemas.service.ProblemaService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/etiquetas")
public class EtiquetaController {
    private final ProblemaService service;
    public EtiquetaController(ProblemaService service){this.service=service;}
    @GetMapping public List<String> listar(){return service.listarEtiquetas();}
}
