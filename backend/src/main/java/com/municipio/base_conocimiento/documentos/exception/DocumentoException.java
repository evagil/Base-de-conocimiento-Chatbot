package com.municipio.base_conocimiento.documentos.exception;

import org.springframework.http.HttpStatus;

public class DocumentoException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;
    public DocumentoException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}
