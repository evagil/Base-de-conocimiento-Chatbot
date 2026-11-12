package com.municipio.base_conocimiento.archivos.exception;
import org.springframework.http.HttpStatus;
public class ArchivoInvalidoException extends RuntimeException {
    private final HttpStatus status; private final String codigo;
    public ArchivoInvalidoException(HttpStatus status,String codigo,String mensaje){super(mensaje);this.status=status;this.codigo=codigo;}
    public HttpStatus getStatus(){return status;} public String getCodigo(){return codigo;}
}
