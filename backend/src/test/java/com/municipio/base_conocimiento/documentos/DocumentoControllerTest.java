package com.municipio.base_conocimiento.documentos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.municipio.base_conocimiento.compartido.web.ManejadorGlobalExcepciones;
import com.municipio.base_conocimiento.documentos.controller.DocumentoController;
import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.documentos.exception.DocumentoException;
import com.municipio.base_conocimiento.documentos.service.ProcesadorDocumentos;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class DocumentoControllerTest {
    @Test void exponeTextoSinCacheYErroresUniformes() throws Exception {
        var servicio = mock(ProcesadorDocumentos.class);
        var mvc = MockMvcBuilders.standaloneSetup(new DocumentoController(servicio)).setControllerAdvice(new ManejadorGlobalExcepciones()).build();
        var archivo = new MockMultipartFile("archivo","manual.pdf","application/pdf",new byte[]{1});
        when(servicio.extraer(any())).thenReturn(new DocumentoExtraido("manual.pdf","application/pdf",1,List.of(new DocumentoExtraido.Seccion(1,"E17","TEXTO")),3,false));
        mvc.perform(multipart("/api/documentos/extraer").file(archivo)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.secciones[0].texto").value("E17")).andExpect(jsonPath("$.ocrUsado").value(false));
        when(servicio.extraer(any())).thenThrow(new DocumentoException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENTO_INVALIDO","Archivo inválido."));
        mvc.perform(multipart("/api/documentos/extraer").file(archivo)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("DOCUMENTO_INVALIDO"));
        doThrow(new MaxUploadSizeExceededException(10485760)).when(servicio).extraer(any());
        mvc.perform(multipart("/api/documentos/extraer").file(archivo)).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.codigo").value("ARCHIVO_DEMASIADO_GRANDE"));
    }
}
