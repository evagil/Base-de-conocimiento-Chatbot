package com.municipio.base_conocimiento.archivos.controller;

import com.municipio.base_conocimiento.archivos.dto.ArchivoResponse;
import com.municipio.base_conocimiento.archivos.service.ArchivoService;
import com.municipio.base_conocimiento.archivos.service.ArchivoService.Propietario;
import com.municipio.base_conocimiento.areas.dto.EstadoActivoRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
public class ArchivoController {
    private final ArchivoService service;
    public ArchivoController(ArchivoService service){this.service=service;}
    @PostMapping(value="/api/problemas/{id}/imagenes",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> problema(@PathVariable Long id,@RequestPart("archivo") MultipartFile archivo,
            @RequestParam String textoAlternativo,@RequestParam(defaultValue="0") int orden){return creada(service.cargar(Propietario.PROBLEMA,id,archivo,textoAlternativo,orden));}
    @GetMapping("/api/problemas/{id}/imagenes") public List<ArchivoResponse> imagenesProblema(@PathVariable Long id){return service.listar(Propietario.PROBLEMA,id);}
    @PostMapping(value="/api/soluciones/{id}/imagenes",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> solucion(@PathVariable Long id,@RequestPart("archivo") MultipartFile archivo,
            @RequestParam String textoAlternativo,@RequestParam(defaultValue="0") int orden){return creada(service.cargar(Propietario.SOLUCION,id,archivo,textoAlternativo,orden));}
    @GetMapping("/api/soluciones/{id}/imagenes") public List<ArchivoResponse> imagenesSolucion(@PathVariable Long id){return service.listar(Propietario.SOLUCION,id);}
    @PostMapping(value="/api/pasos/{id}/imagenes",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> paso(@PathVariable Long id,@RequestPart("archivo") MultipartFile archivo,
            @RequestParam String textoAlternativo,@RequestParam(defaultValue="0") int orden){return creada(service.cargar(Propietario.PASO,id,archivo,textoAlternativo,orden));}
    @GetMapping("/api/pasos/{id}/imagenes") public List<ArchivoResponse> imagenesPaso(@PathVariable Long id){return service.listar(Propietario.PASO,id);}
    @GetMapping("/api/archivos/{id}/contenido") public ResponseEntity<byte[]> contenido(@PathVariable Long id){var c=service.contenido(id);String seguro=c.nombre().replace("\"","");return ResponseEntity.ok().contentType(MediaType.parseMediaType(c.mime())).cacheControl(CacheControl.noCache()).header(HttpHeaders.CONTENT_DISPOSITION,(c.mime().startsWith("image/")?"inline":"attachment")+"; filename*=UTF-8''"+java.net.URLEncoder.encode(seguro,StandardCharsets.UTF_8)).header("X-Content-Type-Options","nosniff").body(c.bytes());}
    @PutMapping("/api/archivos/{id}") public ArchivoResponse actualizar(@PathVariable Long id,@RequestParam String textoAlternativo,@RequestParam(defaultValue="0") int orden, @RequestParam long version){return service.actualizar(id,textoAlternativo,orden,version);}
    @PatchMapping("/api/archivos/{id}/activo") public ArchivoResponse activo(@PathVariable Long id,@Valid @RequestBody EstadoActivoRequest r, @RequestParam long version){return service.cambiarActivo(id,r.activo(),version);}
    @PostMapping(value="/api/problemas/{id}/adjuntos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> adjuntarProblema(@PathVariable Long id,@RequestPart("archivo") MultipartFile archivo,@RequestParam String nombre){return creada(service.cargarDocumento(Propietario.PROBLEMA,id,archivo,nombre,0));}
    @PostMapping(value="/api/soluciones/{id}/adjuntos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoResponse> adjuntarSolucion(@PathVariable Long id,@RequestPart("archivo") MultipartFile archivo,@RequestParam String nombre){return creada(service.cargarDocumento(Propietario.SOLUCION,id,archivo,nombre,0));}
    @GetMapping("/api/problemas/{id}/adjuntos") public List<ArchivoResponse> adjuntosProblema(@PathVariable Long id){return service.listarDocumentos(Propietario.PROBLEMA,id);}
    @GetMapping("/api/soluciones/{id}/adjuntos") public List<ArchivoResponse> adjuntosSolucion(@PathVariable Long id){return service.listarDocumentos(Propietario.SOLUCION,id);}
    private ResponseEntity<ArchivoResponse> creada(ArchivoResponse a){return ResponseEntity.created(URI.create(a.urlContenido())).body(a);}
}
