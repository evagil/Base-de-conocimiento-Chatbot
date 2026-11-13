package com.municipio.base_conocimiento.documentos.controller;

import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.documentos.service.ProcesadorDocumentos;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {
    private final ProcesadorDocumentos procesador;
    public DocumentoController(ProcesadorDocumentos procesador) { this.procesador = procesador; }
    @PostMapping(value = "/extraer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoExtraido> extraer(@RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(procesador.extraer(archivo));
    }
}
