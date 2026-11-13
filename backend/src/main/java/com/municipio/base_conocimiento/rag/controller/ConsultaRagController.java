package com.municipio.base_conocimiento.rag.controller;

import com.municipio.base_conocimiento.rag.dto.*;
import com.municipio.base_conocimiento.rag.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/rag")
public class ConsultaRagController {
    private final BusquedaHibridaRag busqueda;
    private final RespuestaConFuentesRag respuestas;
    public ConsultaRagController(BusquedaHibridaRag busqueda,RespuestaConFuentesRag respuestas){this.busqueda=busqueda;this.respuestas=respuestas;}
    @PostMapping("/buscar") public ResponseEntity<ResultadoRag.Busqueda> buscar(@Valid @RequestBody ConsultaRag consulta){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(busqueda.buscar(consulta.pregunta()).respuesta());
    }
    @PostMapping("/responder") public ResponseEntity<ResultadoRag.Respuesta> responder(@Valid @RequestBody ConsultaRag consulta){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuestas.responder(consulta.pregunta()));
    }
}
