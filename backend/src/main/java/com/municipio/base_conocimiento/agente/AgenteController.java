package com.municipio.base_conocimiento.agente;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rag/agente")
public class AgenteController {
 private final AgenteConsulta agente;
 public AgenteController(AgenteConsulta agente){this.agente=agente;}
 @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<ResultadoAgente> consultar(@Valid @RequestBody ConsultaAgente consulta){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(agente.consultar(consulta.pregunta()));}
}
