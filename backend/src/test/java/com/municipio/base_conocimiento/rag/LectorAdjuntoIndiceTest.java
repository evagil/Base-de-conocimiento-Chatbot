package com.municipio.base_conocimiento.rag;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.municipio.base_conocimiento.documentos.dto.DocumentoExtraido;
import com.municipio.base_conocimiento.documentos.service.ProcesadorDocumentos;
import com.municipio.base_conocimiento.rag.dto.FuentesIndice.Fuente;
import com.municipio.base_conocimiento.rag.service.LectorAdjuntoIndice;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

class LectorAdjuntoIndiceTest {
    @TempDir Path raiz;
    Fuente fuente(String ruta,String hash){return new Fuente("ADJUNTO",1,null,null,1L,0,Instant.now(),"Manual",ruta,hash,"manual.pdf","application/pdf");}
    @Test void verficaHashYLlevaElAdjuntoAlProcesadorSinCambiarStorage()throws Exception{
        var documentos=mock(ProcesadorDocumentos.class);var lector=new LectorAdjuntoIndice(raiz.toString(),documentos);
        byte[] bytes="PDF sintético".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(raiz.resolve("original.pdf"),bytes);
        when(documentos.extraer(any())).thenAnswer(invocacion->{MultipartFile archivo=invocacion.getArgument(0);
            assertThat(archivo.getName()).isEqualTo("archivo");assertThat(archivo.getOriginalFilename()).isEqualTo("manual.pdf");assertThat(archivo.getContentType()).isEqualTo("application/pdf");
            assertThat(archivo.isEmpty()).isFalse();assertThat(archivo.getSize()).isEqualTo(bytes.length);assertThat(archivo.getBytes()).isEqualTo(bytes);
            try(var entrada=archivo.getInputStream()){assertThat(entrada.readAllBytes()).isEqualTo(bytes);}
            archivo.transferTo(raiz.resolve("copia.pdf").toFile());
            return new DocumentoExtraido("manual.pdf","application/pdf",1,List.of(),0,false);
        });
        assertThat(lector.leer(fuente("original.pdf",LectorAdjuntoIndice.huella(bytes))).paginas()).isEqualTo(1);
        assertThat(Files.readAllBytes(raiz.resolve("original.pdf"))).isEqualTo(bytes);
    }
    @Test void rechazaRutasAjenasAusenciaTamanoYHashInconsistente()throws Exception{
        var documentos=mock(ProcesadorDocumentos.class);var lector=new LectorAdjuntoIndice(raiz.toString(),documentos);
        assertThatThrownBy(()->lector.leer(fuente("../ajeno.pdf","x"))).isInstanceOf(java.io.IOException.class);
        assertThatThrownBy(()->lector.leer(fuente("ausente.pdf","x"))).isInstanceOf(java.io.IOException.class);
        Files.write(raiz.resolve("falso.pdf"),new byte[]{1});
        assertThatThrownBy(()->lector.leer(fuente("falso.pdf","x"))).hasMessage("ADJUNTO_INCONSISTENTE");
        try(var archivo=new java.io.RandomAccessFile(raiz.resolve("grande.pdf").toFile(),"rw")){archivo.setLength(10485761);}
        assertThatThrownBy(()->lector.leer(fuente("grande.pdf","x"))).hasMessage("LIMITE_ARCHIVO");verifyNoInteractions(documentos);
    }
}
