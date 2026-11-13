package com.municipio.base_conocimiento.documentos.worker;

import com.municipio.base_conocimiento.archivos.exception.ArchivoInvalidoException;
import com.municipio.base_conocimiento.documentos.proceso.ProtocoloWorker;
import java.nio.file.*;
import java.util.List;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import tools.jackson.databind.ObjectMapper;

/** Entrada de una JVM auxiliar; no inicia Spring ni accede a la base de datos. */
public final class ExtractorWorker {
    private ExtractorWorker() { }
    public static void main(String[] argumentos) throws Exception {
        Path carpeta = Path.of(argumentos[0]).toAbsolutePath().normalize();
        ObjectMapper json = new ObjectMapper();
        var limites = json.readValue(Files.readAllBytes(carpeta.resolve("config.json")), ProtocoloWorker.Limites.class);
        ProtocoloWorker.Resultado resultado;
        try {
            resultado = new MotorExtraccion().extraer(carpeta, limites);
        } catch (LimiteDocumento e) {
            resultado = new ProtocoloWorker.Resultado(null, List.of(), e.getMessage());
        } catch (InvalidPasswordException e) {
            resultado = new ProtocoloWorker.Resultado(null, List.of(), "DOCUMENTO_PROTEGIDO");
        } catch (ArchivoInvalidoException e) {
            resultado = new ProtocoloWorker.Resultado(null, List.of(), "FORMATO_INVALIDO");
        } catch (Exception e) {
            resultado = new ProtocoloWorker.Resultado(null, List.of(), "DOCUMENTO_INVALIDO");
        }
        Files.write(carpeta.resolve("resultado.json"), json.writeValueAsBytes(resultado), StandardOpenOption.CREATE_NEW);
    }
}
