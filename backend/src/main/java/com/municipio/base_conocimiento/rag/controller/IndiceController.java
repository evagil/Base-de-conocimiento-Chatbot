package com.municipio.base_conocimiento.rag.controller;

import com.municipio.base_conocimiento.rag.config.ConfiguracionIndice;
import com.municipio.base_conocimiento.rag.repository.IndiceRepository;
import com.municipio.base_conocimiento.ia.exception.IaException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/ia/indice")
public class IndiceController {
    private final IndiceRepository indice;
    private final ConfiguracionIndice config;
    public IndiceController(IndiceRepository indice,ConfiguracionIndice config){this.indice=indice;this.config=config;}
    public record EstadoIndice(boolean habilitado,boolean embeddings,Map<String,Long> cantidades){}
    @GetMapping public ResponseEntity<EstadoIndice> estado(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new EstadoIndice(config.habilitado(),config.embeddings(),indice.resumen()));}
    @PostMapping("/reconstruir") @ResponseStatus(HttpStatus.ACCEPTED) public void reconstruir(){habilitado();indice.reconstruir(false);}
    @PostMapping("/reintentar") @ResponseStatus(HttpStatus.ACCEPTED) public void reintentar(){habilitado();indice.reconstruir(true);}
    private void habilitado(){if(!config.habilitado())throw new IaException(HttpStatus.SERVICE_UNAVAILABLE,"INDICE_DESHABILITADO","La indexación está deshabilitada.");}
}
